package me.mantou.koko.bridge.ws

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import me.mantou.koko.KoKoBot
import me.mantou.koko.kook.KookAPIEndpoints
import me.mantou.koko.bridge.KookBridge
import me.mantou.koko.kook.model.request.GatewayRequest
import me.mantou.koko.kook.model.signal.HelloPayload
import me.mantou.koko.kook.model.signal.Signal
import me.mantou.koko.kook.model.signal.SignalEvent
import me.mantou.koko.kook.model.signal.SignalType
import me.mantou.koko.util.zlibDecompress
import java.net.ConnectException

class KookWebSocketBridge : KookBridge {
    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(WebSockets)
    }
    private var scope = CoroutineScope(Dispatchers.IO)
    private lateinit var koKoBot: KoKoBot

    private var sn: Int = 0
    private val mapper = jacksonObjectMapper {}.apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }
    private val eventChannel = Channel<Event>(Channel.Factory.UNLIMITED)
    private var currentState: State = State.Init

    override fun init(koKoBot: KoKoBot): Job {
        this.koKoBot = koKoBot

        return scope.launch {
            LOGGER.info { "[INIT] 初始化连接中..." }
            eventChannel.send(Event.GetGateway)
            for (event in eventChannel) {
                LOGGER.debug { "[FSM] 收到事件 $event" }
                handleConnectionEvent(event)
            }
        }
    }

    private suspend fun handleConnectionEvent(event: Event) {
        when (currentState) {
            is State.Init -> {
                // 获取Gateway
                if (event is Event.GetGateway) {
                    try {
                        val url = getWebSocketUrl()
                        LOGGER.debug { "[INIT] 成功获取Gateway url: $url" }
                        changeState(State.GotGateway)
                        eventChannel.send(Event.TryConnectGateway(url))
                    } catch (e: Exception) {
                        //TODO 指数倒退 重试 获取Gateway 最大间隔60s
                    }
                }
            }

            is State.GotGateway -> {
                if (event is Event.TryConnectGateway) {
                    try {
                        val session = httpClient.webSocketSession(event.url)
                        eventChannel.send(Event.ConnectSucceed(session))
                    } catch (e: ConnectException) {
                        // TODO 指数倒退 连接ws 最大重试两次
                    }
                } else if (event is Event.ConnectSucceed) {
                    LOGGER.debug { "[INIT] 成功连接到Gateway..." }
                    changeState(State.ConnectedGateway)
                    eventChannel.send(Event.HandleSession(event.session, null))
                }
            }

            is State.ConnectedGateway -> {
                if (event is Event.HandleSession) {
                    scope.launch root@{
                        //6秒未收到Hello包
                        val frame = withTimeoutOrNull(6000) {
                            event.session.incoming.receive()
                        }

                        if (frame == null) {
                            LOGGER.debug { "[WS] Hello信号超时" }
                            throw RuntimeException("Hello信号超时")
                        }

                        suspend fun handleHelloSignal(signal: Signal) {
                            val payload = mapper.convertValue(signal.payload, HelloPayload::class.java)
                            if (payload.code != 0) throw RuntimeException("握手包(HELLO)异常, code: ${payload.code}")

                            LOGGER.debug { "[WS] 成功收到Hello信号" }
                            eventChannel.send(Event.HelloSucceed(event.session, payload.sessionId))
                        }

                        if (frame is Frame.Text) {
                            handleHelloSignal(mapper.readValue(frame.readText(), Signal::class.java))
                        } else if (frame is Frame.Binary) {
                            handleHelloSignal(mapper.readValue(frame.readBytes().zlibDecompress(), Signal::class.java))
                        }
                    }
                } else if (event is Event.HelloSucceed) {
                    changeState(State.Connected)
                    eventChannel.send(Event.HandleSession(event.session, event.sessionId))
                }
            }

            is State.Connected -> {
                if (event is Event.HandleSession) {
                    LOGGER.info { "[WS] 连接成功..." }
                    scope.launch root@{
                        var waitingPong = false
                        var nextPingTime = 0L
                        var lastPingTime = 0L
                        var repingCount = 0

                        launch {
                            while (this@root.isActive) {
                                //发送ping
                                if (System.currentTimeMillis() >= nextPingTime) {
                                    event.session.send(
                                        mapper.writeValueAsString(Signal(SignalType.PING, sn = sn))
                                    )

                                    val timeMillis = System.currentTimeMillis()
                                    nextPingTime = timeMillis + 1000 * 30
                                    lastPingTime = timeMillis

                                    waitingPong = true

                                    LOGGER.debug { "[WS] 发送Ping" }
                                }

                                //查看pong是否超时
                                if (waitingPong && System.currentTimeMillis() - lastPingTime >= 6 * 1000) {
                                    if (repingCount < 2) {
                                        //修改下次ping的时间
                                        nextPingTime = System.currentTimeMillis() + 1000 * 2 * ++repingCount
                                        waitingPong = false
                                        LOGGER.debug { "[WS] 超时 $repingCount 次, 尝试Reping" }
                                    } else {
                                        LOGGER.debug { "[WS] 连接超时" }
                                        this@root.cancel()
                                    }
                                }
                            }
                        }

                        fun handleSignal(signal: Signal) {
                            when (signal.type) {
                                SignalType.PONG -> {
                                    LOGGER.debug { "[WS] 收到Pong" }
                                    repingCount = 0
                                    waitingPong = false
                                }

                                SignalType.EVENT -> {
                                    val signalEvent = mapper.convertValue(signal.payload, SignalEvent::class.java)
                                    LOGGER.debug {
                                        signalEvent
                                    }
                                }

                                else -> {}
                            }
                        }

                        while (this@root.isActive) {
                            val frame = event.session.incoming.receive()
                            if (frame is Frame.Text) {
                                handleSignal(mapper.readValue(frame.readText(), Signal::class.java))
                            } else if (frame is Frame.Binary) {
                                handleSignal(mapper.readValue(frame.readBytes().zlibDecompress(), Signal::class.java))
                            } else if (frame is Frame.Close) {
                                LOGGER.info { "[WS] 连接关闭" }
                                this@root.cancel()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun changeState(state: State) {
        LOGGER.debug { "[FSM] 切换状态 $currentState -> $state" }
        currentState = state
//        eventChannel.send(Event.EnterState)
    }

    override fun destroy() {
        scope.cancel()
    }

    private suspend fun getWebSocketUrl(): String {
        return koKoBot.apiService.http(KookAPIEndpoints.GATEWAY, GatewayRequest()).url
    }
}

private sealed class State {
    data object Init : State()
    data object GotGateway : State()
    data object ConnectedGateway : State()
    data object Connected : State()
}

private sealed class Event {
    //    data object EnterState: Event()
    data object Retry : Event()
    data class HandleSession(val session: DefaultClientWebSocketSession, val lastSessionId: String?) : Event()

    //Init
    data object GetGateway : Event()

    //GotGateway
    data object TryResume : Event()
    data class TryConnectGateway(val url: String) : Event()
    data class ConnectSucceed(val session: DefaultClientWebSocketSession) : Event()

    //ConnectedGateway
    data class HelloSucceed(val session: DefaultClientWebSocketSession, val sessionId: String) : Event()
}

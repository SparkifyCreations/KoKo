package me.mantou.koko.kook.bridge.ws

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import me.mantou.koko.KoKoBot
import me.mantou.koko.kook.KookAPIEndpoints
import me.mantou.koko.kook.bridge.KookBridge
import me.mantou.koko.kook.model.request.GatewayRequest
import me.mantou.koko.kook.model.signal.HelloPayload
import me.mantou.koko.kook.model.signal.Signal
import me.mantou.koko.kook.model.signal.SignalType
import me.mantou.koko.util.zlibDecompress

class KookWebSocketBridge : KookBridge {
    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(WebSockets)
    }

    private val mapper = jacksonObjectMapper()
    private lateinit var koKoBot: KoKoBot
    private var scope = CoroutineScope(Dispatchers.IO)

    private var sn: Int = 0

    override suspend fun init(koKoBot: KoKoBot) {
        this.koKoBot = koKoBot
        var url = getWebSocketUrl()

        while (true) {
            try {
                httpClient.webSocket(url) {
                    var sessionId: String? = null
                    val startTimestamp = System.currentTimeMillis()
                    var nextPingDelay = 0
                    var lastPongTimestamp = 0L
                    var repingCount = 0

                    //处理ping pong和初始连接
                    val lifecycleJob = launch {
                        var lastPingTimestamp = 0L

                        while (isActive) {
                            //如果握手包(HELLO)超过6秒未收到
                            if (sessionId == null) {
                                if (System.currentTimeMillis() - startTimestamp < 1000 * 6) continue
                                LOGGER.info { "等待握手包[HELLO]超时..." }
                                this@launch.cancel()
                                this@webSocket.cancel()
                            }
                            //如果距离上次ping超过6秒且没有收到pong
                            if (lastPongTimestamp - lastPingTimestamp > 1000 * 6) {
                                //如果重新ping也没有收到pong，则断开连接
                                if (repingCount < 2) {
                                    nextPingDelay = 2 * ++repingCount
                                    continue
                                }
                                LOGGER.info { "[HELLO]连接超时..." }
                                //TODO resume
                                this@launch.cancel()
                                this@webSocket.cancel()
                            } else {
                                //重置超时中重连计数
                                repingCount = 0
                                //重置下次ping的时间
                                nextPingDelay = 30
                            }

                            //如果应该开始ping了
                            if (System.currentTimeMillis() - lastPingTimestamp > 1000 * nextPingDelay) {
                                send(mapper.writeValueAsString(Signal(SignalType.PING, sn = sn)))
                                lastPingTimestamp = System.currentTimeMillis()
                                LOGGER.info { "发送[PING]" }
                            }
                        }
                    }

                    LOGGER.info { "开始连接" }
                    while (isActive) {
                        val frame = incoming.receive() as Frame.Binary
                        val signal = mapper.readValue(frame.data.zlibDecompress(), Signal::class.java)

                        when (signal.type) {
                            SignalType.HELLO -> {
                                val payload = mapper.convertValue(signal.payload, HelloPayload::class.java)
                                if (payload.code != 0) throw RuntimeException("握手包(HELLO)异常, code: ${payload.code}")

                                sessionId = payload.sessionId
                                LOGGER.info { "握手成功, sessionId: $sessionId" }
                            }
                            SignalType.PONG -> {
                                lastPongTimestamp = System.currentTimeMillis()
                                LOGGER.info { "收到[PONG]" }
                            }
                            SignalType.RECONNECT -> {
                                lifecycleJob.cancel()
                                this@webSocket.cancel()
                                LOGGER.info { "收到[RECONNECT]" }
                            }
                            else -> {}
                        }
                    }
                }
            }catch (e: Exception){
                LOGGER.info { "遇到异常 ${e.message}, 尝试重新连接" }
            }
        }
    }

    override suspend fun destroy() {
        scope.cancel()
    }

    private suspend fun getWebSocketUrl(): String {
        return koKoBot.apiService.http(KookAPIEndpoints.GATEWAY, GatewayRequest()).url
    }
}

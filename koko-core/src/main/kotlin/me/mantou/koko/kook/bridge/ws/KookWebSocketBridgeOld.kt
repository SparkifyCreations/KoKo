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


@Deprecated("耦合行为过多，难以维护，已弃用")
class KookWebSocketBridgeOld : KookBridge {
    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(WebSockets)
    }

    private val mapper = jacksonObjectMapper()
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var koKoBot: KoKoBot

    private var sn: Int = 0

    override fun init(koKoBot: KoKoBot): Job {
        this.koKoBot = koKoBot
        return scope.launch {
            val websocketUrl = getWebSocketUrl()
            while (this.isActive) {
                //开启连接
                httpClient.webSocket(websocketUrl) {
                    var nextPingTime = 0L
                    var lastPingTime = 0L
                    var needPong = false
                    val startTime = System.currentTimeMillis()
                    var sessionId: String? = null

                    var repingCount = 0
                    //维护ping
                    launch {
                        var inReping = false

                        while (this@webSocket.isActive) {
                            //如果超过6s仍未收到HELLO信号
                            if (sessionId == null) {
                                if (System.currentTimeMillis() - startTime < 1000 * 6) continue
                                LOGGER.info { "[HELLO] 连接成功后在6s内未收到HELLO信号，重新连接" }
                                this@webSocket.cancel()
                                break
                            }

                            //进行ping操作
                            if (System.currentTimeMillis() > nextPingTime) {
                                //发送ping包
                                send(mapper.writeValueAsString(Signal(SignalType.PING, sn = sn)))

                                LOGGER.info { "${takeIf { repingCount != 0 }?.let { "[超时中] " } ?: ""}发送[PING]" }

                                //设置下次ping的时间
                                nextPingTime = System.currentTimeMillis() + 1000 * 30
                                lastPingTime = System.currentTimeMillis()
                                //设置需要接收pong
                                needPong = true
                                inReping = false
                            }

                            if (inReping) continue
                            //如果不需要接收pong
                            if (!needPong) continue
                            //如果当前时间超过预期收到pong的时间
                            if (System.currentTimeMillis() - lastPingTime > 1000 * 6) {
                                //尝试再次ping两次，如果仍然失败，则重新连接
                                if (repingCount < 2) {
                                    //修改下次ping的时间
                                    nextPingTime = System.currentTimeMillis() + 1000 * 2 * ++repingCount
                                    inReping = true
                                    LOGGER.info { "超时中, 第 $repingCount 次尝试" }
                                } else {
                                    LOGGER.info { "已超时, 尝试重新连接" }
                                    //TODO resume
                                    this@webSocket.cancel()
                                    break
                                }
                            }
                        }
                    }

                    //处理信号
                    while (true) {
                        val frame = incoming.receive() as Frame.Binary
                        val signal = mapper.readValue(frame.data.zlibDecompress(), Signal::class.java)

                        when (signal.type) {
                            SignalType.HELLO -> {
                                val payload = mapper.convertValue(signal.payload, HelloPayload::class.java)
                                if (payload.code != 0) throw RuntimeException("握手包(HELLO)异常, code: ${payload.code}")

                                sessionId = payload.sessionId
                                LOGGER.info { "收到[HELLO], 握手成功 sessionId: $sessionId" }
                            }

                            SignalType.PONG -> {
                                repingCount = 0
                                needPong = false
                                LOGGER.info { "收到[PONG]" }
                            }

                            SignalType.RECONNECT -> {
                                this@webSocket.cancel()
                                LOGGER.info { "收到[RECONNECT]" }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }


    override fun destroy() {
        scope.cancel()
    }

    private suspend fun getWebSocketUrl(): String {
        return koKoBot.apiService.http(KookAPIEndpoints.GATEWAY, GatewayRequest()).url
    }
}
package me.mantou.koko.kook.bridge

import com.fasterxml.jackson.annotation.JsonKey
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import me.mantou.koko.KoKoBot
import me.mantou.koko.kook.KookAPIEndpoints
import me.mantou.koko.kook.model.request.GatewayRequest
import me.mantou.koko.kook.model.signal.Signal
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

class KookWebSocketBridge : KookBridge {
    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(WebSockets)
    }

    private val mapper = jacksonObjectMapper()

    private lateinit var koKoBot: KoKoBot

    override suspend fun init(koKoBot: KoKoBot) {
        this.koKoBot = koKoBot
        val url = getWebSocketUrl()

        httpClient.webSocket(url) {
            val frame = incoming.receive() as Frame.Binary
            val signal = mapper.readValue(decompressZlib(frame.data), Signal::class.java)
            LOGGER.debug { signal.type }
        }
    }

    override suspend fun destroy() {

    }

    private fun decompressZlib(bytes: ByteArray, bufferSize: Int = 1024): ByteArray{
        val outputStream = ByteArrayOutputStream()

        return outputStream.use {
            val inflater = Inflater()
            val buffer = ByteArray(bufferSize)
            inflater.setInput(bytes)

            while (!inflater.finished()) {
                outputStream.write(
                    buffer,
                    0,
                    inflater.inflate(buffer)
                )
            }

            inflater.end()
            outputStream.toByteArray()
        }
    }

    private suspend fun getWebSocketUrl(): String {
        return koKoBot.apiService.http(KookAPIEndpoints.GATEWAY, GatewayRequest()).url
    }
}

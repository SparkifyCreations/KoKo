package me.mantou.koko.kook.bridge

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import me.mantou.koko.KoKoBot
import me.mantou.koko.kook.KookAPIEndpoints
import me.mantou.koko.kook.model.request.GatewayRequest

class KookWebSocketBridge : KookBridge {
    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(WebSockets)
    }

    private lateinit var koKoBot: KoKoBot

    override suspend fun init(koKoBot: KoKoBot) {
        this.koKoBot = koKoBot
        val url = getWebSocketUrl()

        LOGGER.info { url }
    }

    private suspend fun getWebSocketUrl(): String {
        return koKoBot.apiService.http(KookAPIEndpoints.GATEWAY, GatewayRequest()).url
    }
}
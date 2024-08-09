package me.mantou.koko

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class WebSocketTest {
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(WebSockets)
    }

    @Test
    fun test(){
        runBlocking {
            httpClient.webSocketSession("ws://127.0.0.1:2334/ws")
        }
    }
}
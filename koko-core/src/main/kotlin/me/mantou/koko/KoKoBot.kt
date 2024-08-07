package me.mantou.koko

import kotlinx.coroutines.runBlocking
import me.mantou.koko.kook.bridge.KookBridge
import me.mantou.koko.kook.KookAPIService

class KoKoBot(
    botToken: String,
    private val connector: KookBridge
) {
    val apiService = KookAPIService("https://www.kookapp.cn/api/", "v3", botToken)

    suspend fun start(){
        connector.init(this@KoKoBot)
    }
}
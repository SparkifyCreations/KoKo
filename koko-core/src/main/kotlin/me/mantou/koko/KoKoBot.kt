package me.mantou.koko

import kotlinx.coroutines.runBlocking
import me.mantou.koko.kook.bridge.KookBridge
import me.mantou.koko.kook.KookAPIService

class KoKoBot(
    botToken: String,
    val connector: KookBridge
) {
    val apiService = KookAPIService("https://www.kookapp.cn/api/", "v3", botToken)

    fun start(){
        runBlocking {
            connector.init(this@KoKoBot)
        }
    }
}
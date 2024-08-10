package me.mantou.koko

import me.mantou.koko.kook.KookAPIService
import me.mantou.koko.bridge.KookBridge
import me.mantou.koko.bridge.ws.KookWebSocketBridge

class KoKoBot(
    block: KoKoConfig.() -> Unit = {}
) {
    private val config: KoKoConfig = KoKoConfig().apply(block)


    val apiService = KookAPIService("https://www.kookapp.cn/api/", "v3", config.botToken)

    suspend fun start(){
        config.bridge.init(this@KoKoBot).join()
    }

    fun stop(){
        config.bridge.destroy()
    }
}

class KoKoConfig{
    var botToken: String = ""
    var bridge: KookBridge = KookWebSocketBridge()
}
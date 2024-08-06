package me.mantou.koko

import me.mantou.koko.kook.bridge.KookWebSocketBridge
import org.junit.jupiter.api.Test

class KoKoBotTest {

    @Test
    fun test() {
        val bot = KoKoBot("1/MTMzMDQ=/d6qOhen2ONH/FC3FLHIwOw==", KookWebSocketBridge())
        bot.start()


    }
}
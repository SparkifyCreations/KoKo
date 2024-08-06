package me.mantou.koko

import me.mantou.koko.kook.bridge.KookWebSocketBridge
import org.junit.jupiter.api.Test
import java.io.FileReader
import java.util.Properties

class KoKoBotTest {

    @Test
    fun test() {
        val properties = Properties()
        properties.load(FileReader("E:\\Personal\\IdeaProjects\\Sparkify\\KoKo\\bot.properties"))
        val bot = KoKoBot(properties.getProperty("token"), KookWebSocketBridge())
        bot.start()
    }
}
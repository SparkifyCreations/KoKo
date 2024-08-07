package me.mantou.koko

import kotlinx.coroutines.runBlocking
import me.mantou.koko.kook.bridge.KookWebSocketBridge
import java.io.FileReader
import java.util.Properties
import kotlin.test.Test

class KoKoBotTest {

    @Test
    fun test() {
        val properties = Properties()
        properties.load(FileReader("E:\\Personal\\IdeaProjects\\Sparkify\\KoKo\\bot.properties"))
        val bot = KoKoBot(properties.getProperty("token"), KookWebSocketBridge())
        runBlocking {
            bot.start()
        }
    }
}
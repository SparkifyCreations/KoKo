package me.mantou.koko

import kotlinx.coroutines.runBlocking
import java.io.FileReader
import java.util.Properties
import kotlin.test.Test

class KoKoBotTest {

    @Test
    fun test() {
        val properties = Properties()
        properties.load(FileReader("E:\\Personal\\IdeaProjects\\Sparkify\\KoKo\\bot.properties"))
        val bot = KoKoBot{
            botToken = properties.getProperty("token")
        }
        runBlocking {
            bot.start()
        }
    }
}
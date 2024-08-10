package me.mantou.koko

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.mantou.koko.kook.KookAPIEndpoints
import me.mantou.koko.kook.model.request.User
import java.io.FileReader
import java.util.Properties
import kotlin.test.Test

class KoKoBotTest {

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    @Test
    fun test() {
        val properties = Properties()
        properties.load(FileReader("E:\\Personal\\IdeaProjects\\Sparkify\\KoKo\\bot.properties"))
        val bot = KoKoBot {
            botToken = properties.getProperty("token")
//            bridge = KookWebSocketBridgeOld()
        }
        runBlocking {
            val botJob = launch {
                bot.start()
            }

            launch {
                delay(1000 * 1)
                val me = bot.apiService.http(
                    KookAPIEndpoints.User.ME,
                    {}
                )

                val onlineState = bot.apiService.http(
                    KookAPIEndpoints.User.GET_ONLINE_STATUS,
                    {}
                )

                LOGGER.info { """
                    
                Bot信息:
                ${me.toPrettyString()}
                Bot在线信息: $onlineState
                """.trimIndent()
                }

                val view = bot.apiService.http(
                    KookAPIEndpoints.User.GET_PERSONAL_PANEL,
                    User.GetPersonalPanelRequest("1491494650")
                )
                LOGGER.info {
                    "馒头\n ${view.get("badge").toPrettyString()}"
                }
            }

            botJob.join()
        }
    }
}
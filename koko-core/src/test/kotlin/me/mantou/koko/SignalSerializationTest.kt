package me.mantou.koko

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import me.mantou.koko.kook.model.signal.Signal
import me.mantou.koko.kook.model.signal.SignalType
import kotlin.test.Test
import kotlin.test.assertEquals

class SignalSerializationTest {

    private val mapper = jacksonObjectMapper()

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }

    @Test
    fun test() {
        val signal = Signal(SignalType.PING, sn = 1)

        assertEquals("{\"s\":2,\"sn\":1}", mapper.writeValueAsString(signal))

        val signalStr = "{\"s\":\"1\",\"d\":{\"k1\":\"v1\",\"k_2\":\"v_2\"}}"
        val signalFromStr = mapper.readValue(signalStr, Signal::class.java)
        assertEquals(
            Signal(SignalType.HELLO, payload = mapOf("k1" to "v1", "k_2" to "v_2")),
            signalFromStr
        )

        val value = mapper.convertValue(signalFromStr.payload, KModel::class.java)
        LOGGER.info { value }
    }

    data class KModel(
        val k1: String,
        @JsonProperty("k_2")
        val k2: String
    )
}
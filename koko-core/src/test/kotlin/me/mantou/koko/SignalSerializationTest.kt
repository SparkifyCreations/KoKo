package me.mantou.koko

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import me.mantou.koko.kook.model.signal.Signal
import me.mantou.koko.kook.model.signal.SignalType
import kotlin.test.Test
import kotlin.test.assertEquals

class SignalSerializationTest {

    private val mapper = jacksonObjectMapper()
    companion object{
        private val LOGGER = KotlinLogging.logger {  }
    }

    @Test
    fun test() {
        val signal = Signal(SignalType.PING, sn = 1)

        assertEquals("{\"s\":2,\"sn\":1}", mapper.writeValueAsString(signal))

        val signalStr = "{\"s\":\"1\",\"d\":{\"k1\":\"v1\"}}"
        val signalFromStr = mapper.readValue(signalStr, Signal::class.java)
        assertEquals(
            Signal(SignalType.HELLO, payload = mapOf("k1" to "v1")),
            signalFromStr
        )

        assert(signalFromStr.payload is LinkedHashMap<*, *>)
    }
}
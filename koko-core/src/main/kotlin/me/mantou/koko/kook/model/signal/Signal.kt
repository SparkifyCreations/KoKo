package me.mantou.koko.kook.model.signal

import com.fasterxml.jackson.annotation.*

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Signal(
    @JsonProperty("s")
    val type: SignalType,
    @JsonProperty("d")
    val payload: Any? = null,
    val sn: Int? = null
)

enum class SignalType(
    @JsonValue
    val op: Int,
    val bound: SignalBound
) {
    EVENT(0, SignalBound.CLIENT_BOUND),
    HELLO(1, SignalBound.CLIENT_BOUND),
    PING(2, SignalBound.SERVER_BOUND),
    PONG(3, SignalBound.CLIENT_BOUND),
    RESUME(4, SignalBound.SERVER_BOUND),
    RECONNECT(5, SignalBound.CLIENT_BOUND),
    RESUME_ACK(6, SignalBound.CLIENT_BOUND);

    companion object {
        @JsonCreator
        fun fromValue(op: Int): SignalType {
            return entries.find { it.op == op } ?: throw IllegalArgumentException("Unknown SignalType: $op")
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class HelloPayload(
    val code: Int,
    @JsonProperty("session_id")
    val sessionId: String
)

enum class SignalBound {
    CLIENT_BOUND,
    SERVER_BOUND
}
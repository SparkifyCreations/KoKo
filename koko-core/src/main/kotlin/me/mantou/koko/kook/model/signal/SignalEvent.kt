package me.mantou.koko.kook.model.signal

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import me.mantou.koko.kook.model.request.User

@JsonIgnoreProperties(ignoreUnknown = true)
data class SignalEvent(
    val channelType: ChannelType,
    @JsonProperty("type")
    val eventType: EventType,
    val targetId: String,
    val authorId: String,
    val content: JsonNode, //为什么不是str, 请看 https://developer.kookapp.cn/doc/event/message#%E9%81%93%E5%85%B7%20%E6%B6%88%E6%81%AF
    val msgId: String,
    val msgTimestamp: Long,
    val nonce: String,
    val extra: Any? = null
)

enum class ChannelType{
    GROUP, PERSON, BROADCAST;

//    @JsonValue
//    fun toKey(): String {
//        return this.name
//    }
//
//    companion object {
//        @JsonCreator
//        fun fromKey(key: String): ChannelType {
//            return entries.find {
//                it.name.equals(key, true)
//            } ?: throw IllegalArgumentException("Unknown ChannelType: $key")
//        }
//    }
}

enum class EventType(
    @JsonValue
    val op: Int,
    val description: String = ""
){
    TEXT_MESSAGE(1, "文字消息"),
    PICTURE_MESSAGE(2, "图片消息"),
    VIDEO_MESSAGE(3, "视频消息"),
    FILE_MESSAGE(4, "文件消息"),
    AUDIO_MESSAGE(8, "音频消息"),
    K_MARKDOWN(9, "KMarkdown"),
    CARD_MESSAGE(10, "卡牌消息"),
    PROP_MESSAGE(12, "道具消息"),
    SYSTEM_MESSAGE(255, "系统消息");

    companion object {
        @JsonCreator
        fun fromValue(op: Int): EventType = entries.find { it.op == op } ?: throw IllegalArgumentException("Unknown EventType: $op")
    }
}

data class CommonExtra(
    val type: EventType,
    val guildId: String,
    val channelName: String,
    val mention: List<String>,
    val mentionAll: Boolean,
    val mentionRoles: List<String>,
    val mentionHere: Boolean,
    val author: User
)

data class SystemExtra(
    val type: String,
    val body: JsonNode
)
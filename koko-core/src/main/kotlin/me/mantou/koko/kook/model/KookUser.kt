package me.mantou.koko.kook.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class KookUser(
    val id: String,
    val username: String,
    val nickname: String,
    val identifyNum: String,
    val online: Boolean,
    val bot: Boolean,
    val status: UserStatus,
    val avatar: String,
    val vipAvatar: String,
    val mobileVerified: Boolean,
    val roles: List<String>
)

enum class UserStatus(
    @JsonValue
    val code: Int,
    val isNormal: Boolean = true
){
    NORMAL_0(0),
    NORMAL_1(1),
    BANNED(10, false);

    companion object{
        @JsonCreator
        fun fromCode(code: Int) = entries.find { it.code == code } ?: throw IllegalArgumentException("Unknown UserStatus: $code")
    }
}
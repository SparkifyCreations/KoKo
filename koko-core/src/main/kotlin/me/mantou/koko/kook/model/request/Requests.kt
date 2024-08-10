package me.mantou.koko.kook.model.request

data class GatewayRequest(
    val compress: Int = 1
)

object User{
    data class ViewRequest(
        val userId: String,
        val guildId: String? = null
    )

    data class GetPersonalPanelRequest(
        val userId: String
    )
}
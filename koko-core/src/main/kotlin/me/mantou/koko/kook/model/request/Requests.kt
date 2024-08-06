package me.mantou.koko.kook.model.request

import me.mantou.koko.kook.Param

data class GatewayRequest(
    @Param
    val compress: Int = 1
)
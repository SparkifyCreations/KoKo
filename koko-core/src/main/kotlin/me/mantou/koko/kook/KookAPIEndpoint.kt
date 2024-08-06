package me.mantou.koko.kook


import io.ktor.http.*
import me.mantou.koko.kook.model.request.GatewayRequest
import me.mantou.koko.kook.model.response.GatewayResponse
import kotlin.reflect.KClass

data class KookAPIEndpoint<REQ: Any, RES: Any>(
    val method: HttpMethod,
    val path: String,
    val req: KClass<REQ>,
    val res: KClass<RES>,
)

@Target(AnnotationTarget.PROPERTY)
annotation class Param

object KookAPIEndpoints {
    val GATEWAY = KookAPIEndpoint(
        HttpMethod.Get,
        "/gateway/index",
        GatewayRequest::class,
        GatewayResponse::class
    )
}

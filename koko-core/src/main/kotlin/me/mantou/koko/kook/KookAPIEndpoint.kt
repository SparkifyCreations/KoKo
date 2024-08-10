package me.mantou.koko.kook


import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import me.mantou.koko.kook.model.request.GatewayRequest
import me.mantou.koko.kook.model.response.GatewayResponse
import kotlin.reflect.KClass

data class KookAPIEndpoint<REQ: Any, RES: Any>(
    val path: String,
    val req: KClass<REQ>,
    val res: KClass<RES>,
    val method: HttpMethod = HttpMethod.Get
)

@Target(AnnotationTarget.PROPERTY)
@Deprecated("Auto parse params on GET method")
annotation class Param

object KookAPIEndpoints {
    val GATEWAY = KookAPIEndpoint(
        "/gateway/index",
        GatewayRequest::class,
        GatewayResponse::class
    )

    object Message{


    }

    object User{
        val ME = KookAPIEndpoint(
            "/user/me",
            Any::class,
            JsonNode::class
        )
        val VIEW = KookAPIEndpoint(
            "/user/view",
            me.mantou.koko.kook.model.request.User.ViewRequest::class,
            JsonNode::class
        )
        val OFFLINE = KookAPIEndpoint(
            "/user/offline",
            Any::class,
            Any::class,
            HttpMethod.Post
        )
        val ONLINE = KookAPIEndpoint(
            "/user/online",
            Any::class,
            Any::class,
            HttpMethod.Post
        )
        val GET_ONLINE_STATUS = KookAPIEndpoint(
            "/user/get-online-status",
            Any::class,
            JsonNode::class,
            HttpMethod.Post
        )
        val GET_PERSONAL_PANEL = KookAPIEndpoint(
            "/user/get-personal-panel",
            me.mantou.koko.kook.model.request.User.GetPersonalPanelRequest::class,
            JsonNode::class
        )
    }
}

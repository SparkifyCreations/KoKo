package me.mantou.koko.kook

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.hasAnnotation

class KookAPIService(
    val baseUrl: String,
    val apiVersion: String,
    private val token: String
) {
    companion object{
        private val LOGGER = KotlinLogging.logger {  }
    }

    private val mapper = jacksonObjectMapper {  }
    private val client = HttpClient(CIO) {}

    suspend fun <REQ : Any, RES : Any> http(
        apiEndpoint: KookAPIEndpoint<REQ, RES>,
        payload: REQ
    ): RES {

        val fullUrl = baseUrl + apiVersion + apiEndpoint.path

        val response = client.request(fullUrl) {
            this.method = apiEndpoint.method
            this.headers[HttpHeaders.Authorization] = "Bot $token"

            when (apiEndpoint.method) {
                HttpMethod.Get -> {
                    payload::class.declaredMemberProperties.filter { p ->
                        p.hasAnnotation<Param>()
                    }.forEach { p ->
                        this.parameter(p.name, p.call(payload))
                    }
                }
                HttpMethod.Post -> {
                    this.contentType(ContentType.Application.Json)
                    this.setBody(mapper.writeValueAsString(payload))
                }
            }
        }

        val jsonNode = mapper.readTree(response.bodyAsText())
        val code = jsonNode.get("code").asInt()
        if (code != 0){
            throw Exception("API response code $code (${jsonNode.get("message").asText()}) on [${apiEndpoint.method.value}] $fullUrl")
        }

//        if (response.status.value != 200) {
//            throw Exception("HTTP response code ${response.status.value} on [${apiEndpoint.method.value}] $fullUrl")
//        }

        return mapper.treeToValue(jsonNode.get("data"), apiEndpoint.res.java)
    }
}
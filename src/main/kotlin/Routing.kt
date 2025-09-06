package com.drbrosdev

import glide.api.GlideClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable

fun Application.configureRouting() {
    routing {
        get("/info") {
            call.respondText("hop-service")
        }
    }
}

@Serializable
data class CreateHopPayload(
    val url: String
)

// TODO: investigate how to introduce redis cache, create docker-compose.local for it
// TODO: Configure health check endpoints - make sure to exclude them from api auth

fun Application.configureHopRoutes() = routing {
    val findHop: FindHopByKey by dependencies
    val createHop: CreateHop by dependencies
    val cache: GlideClient by dependencies

    /*
    POST /hops HTTP/1.1
    Content-Type: application/json

    { "url" : "some-slug" }
     */
    post("/") {
        // parse payload
        val payload = call.receive<CreateHopPayload>()
        log.info("createHop called with $payload.")

        val hop = createHop.execute(payload.url)
        cache.set(hop.key, hop.url).await().also { log.info("Stored ${hop.url} in cache.") }

        call.respond(HttpStatusCode.Created, hop.dto())
    }

    /*
    GET /hops/{key} HTTP/1.1

    301 Moved Permanently ## 302 Found (Moved Temporarily)
     */
    get("/{hop_key}") {
        val key = call.pathParameters["hop_key"]
        requireNotNull(key)
        log.info("findHop called with $key.")

        val hop = cache.get(key).await().also { if (it != null) { log.info("Cache hit for $it.") } }
            ?: findHop.execute(key)?.url

        when {
            hop != null -> call.respondRedirect(url = hop, permanent = false)

            else -> call.respond(status = HttpStatusCode.NotFound, message = "")
        }
    }
}

package com.drbrosdev

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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

fun Application.configureHopRoutes() = routing {
    val findHop: FindHopByKey by dependencies
    val createHop: CreateHop by dependencies

    route("/hops") {
        /*
        POST /hops HTTP/1.1
        Content-Type: application/json

        { "url" : "some-slug" }
         */
        post {
            // parse payload
            val payload = call.receive<CreateHopPayload>()
            // create hop
            val hop = createHop.execute(payload.url)
            // respond
            call.respond(HttpStatusCode.Created, hop.dto())
        }

        /*
        GET /hops/{key} HTTP/1.1

        301 Moved Permanently ## 302 Found (Moved Temporarily)
         */
        get("/{hop_key}") {
            val key = call.pathParameters["hop_key"]
            requireNotNull(key)
            val hop = findHop.execute(key)
            requireNotNull(hop)
            call.respondRedirect(url = hop.url, permanent = false)
        }
    }
}

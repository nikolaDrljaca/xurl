package com.drbrosdev

import glide.api.GlideClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter

fun Application.configureRouting() {
    routing {
        get("/info") {
            call.respondText("hop-service")
        }
        get("/health") {
            call.respond(HttpStatusCode.OK)
        }
    }
}

// ===

@Serializable
data class CreateHopPayload(
    val url: String
)

@Serializable
data class HopDto(
    val id: String,
    val key: String,
    val url: String,
    val fullUrl: String,
    val createdAt: String
)

fun Application.configureHopRoutes() = routing {
    /*
    NOTE: Since these are accessed here, they are created immediately on app startup
    and are essentially singletons
     */
    val findHop: FindHopByKey by dependencies
    val createHop: CreateHop by dependencies
    val config: HopServiceConfiguration by dependencies

    post("/") {
        // parse payload and create hop
        val payload = call.receive<CreateHopPayload>()
        log.info("createHop called with $payload.")
        val hop = createHop.execute(payload.url)

        // cache created hop
        // NOTE: store in cache inside new coroutine so the method responds
        // immediately
        launch {
            // NOTE: since it is accessed here, it will suspend until a client is created
            val cache: GlideClient? = dependencies.resolve()
            when {
                cache != null -> {
                    cache.set(hop.key, hop.url)
                        .await()
                    log.info("Stored ${hop.url} in cache.")
                }
                else -> Unit
            }
        }

        // prepare response
        val response = HopDto(
            id = hop.id.toString(),
            key = hop.key,
            url = hop.url,
            createdAt = hop.createdAt.format(DateTimeFormatter.ISO_DATE),
            fullUrl = "${config.basePath}/${hop.key}"
        )
        call.respond(HttpStatusCode.Created, response)
    }


    get("/{hop_key}") {
        val key = requireNotNull(call.pathParameters["hop_key"])
        log.info("findHop called with $key.")

        val cache: GlideClient? = dependencies.resolve()

        val hop = when {
            cache != null -> {
                val stored = cache.get(key)?.await()
                if (stored != null) { log.info("Cache hit for $stored.")}
                stored
            }

            else -> findHop.execute(key)?.url
        }

        when {
            hop != null -> call.respondRedirect(url = hop, permanent = false)

            else -> call.respond(HttpStatusCode.NotFound)
        }
    }
}

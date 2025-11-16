package com.drbrosdev

import glide.api.GlideClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
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
data class CreateShortUrlPayload(
    val url: String
)

@Serializable
data class ShortUrlDto(
    val id: String,
    val key: String,
    val url: String,
    val fullUrl: String,
    val createdAt: String
)

suspend fun createHopRouteHandler(
    url: String,
    fullPath: String,
    createShortUrl: CreateShortUrl,
    cacheAccessor: suspend () -> GlideClient?
): ShortUrlDto = coroutineScope {
    // create url
    val hop = createShortUrl.execute(url)
    // cache created hop
    // NOTE: since it is accessed here, it will suspend until a client is created
    val cache: GlideClient? = cacheAccessor()
    when {
        cache != null -> cache.set(hop.key, hop.url).await()

        else -> Unit
    }
    // prepare response
    ShortUrlDto(
        id = hop.id.toString(),
        key = hop.key,
        url = hop.url,
        createdAt = hop.createdAt.format(DateTimeFormatter.ISO_DATE),
        fullUrl = "$fullPath/l/${hop.key}"
    )
}

fun Application.configureShortUrlRoutes() = routing {
    /*
    NOTE: Since these are accessed here, they are created immediately on app startup
    and are essentially singletons
     */
    val findHop: FindShortUrlByKey by dependencies
    val createShortUrl: CreateShortUrl by dependencies
    val config: ShortUrlServiceConfiguration by dependencies

    post("/create-url") {
        val payload = call.receiveText()
        log.info("create-url received $payload")
        // NOTE: content is
        // name1=value1
        // name2=value2
        val url = payload.split("=")
            .last()
            .trim()
        val response = createHopRouteHandler(
            url = url,
            fullPath = config.basePath,
            createShortUrl = createShortUrl,
            cacheAccessor = { dependencies.resolve() }
        )
        call.respondHtml {
            shortUrlCreatedPage(
                basePath = config.basePath,
                createdUrl = response.fullUrl
            )
        }
    }

    post("/l") {
        // parse payload and create hop
        val payload = call.receive<CreateShortUrlPayload>()
        log.info("createHop called with $payload.")
        // prepare response
        val response = createHopRouteHandler(
            url = payload.url,
            fullPath = config.basePath,
            createShortUrl = createShortUrl,
            cacheAccessor = { dependencies.resolve() }
        )
        call.respond(HttpStatusCode.Created, response)
    }

    get("/l/{hop_key}") {
        val key = requireNotNull(call.pathParameters["hop_key"])
        log.info("findHop called with $key.")

        val cache: GlideClient? = dependencies.resolve()

        val hop = when {
            cache != null -> {
                val stored = cache.get(key)?.await()
                if (stored != null) {
                    log.info("Cache hit for $stored.")
                }
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

fun Application.configureViewRoutes() = routing {
    val config: ShortUrlServiceConfiguration by dependencies
    // static assets
    staticResources("/public", "public")

    get("/") {
        call.respondHtml {
            createUrlPage(config.basePath)
        }
    }
}

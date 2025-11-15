package com.drbrosdev

import glide.api.GlideClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.html.*
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
    // NOTE: store in cache inside new coroutine so the method responds
    // immediately
    launch {
        // NOTE: since it is accessed here, it will suspend until a client is created
        val cache: GlideClient? = cacheAccessor()
        when {
            cache != null -> cache.set(hop.key, hop.url).await()

            else -> Unit
        }
    }
    // prepare response
    ShortUrlDto(
        id = hop.id.toString(),
        key = hop.key,
        url = hop.url,
        createdAt = hop.createdAt.format(DateTimeFormatter.ISO_DATE),
        fullUrl = "$fullPath/${hop.key}"
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
        val url = payload.split("=").last()
        val response = createHopRouteHandler(
            url = url,
            fullPath = config.basePath,
            createShortUrl = createShortUrl,
            cacheAccessor = { dependencies.resolve() }
        )
        call.respondHtml {
            shortUrlCreatedPage(response.fullUrl)
        }
    }

    post("/") {
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


    get("/{hop_key}") {
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

fun Application.configureClientRoutes() = routing {
    get("/") {
        call.respondHtml {
            head {
                title { +"Another URL Shortener" }
                script(src = "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4") { }
            }

            body(
                classes = "h-screen w-screen"
            ) {
                mainLayout {
                    p(classes = "text-2xl text-center") { +"Yet Another URL Shortener" }
                    postForm(
                        encType = FormEncType.textPlain,
                        action = "/create-url",
                        classes = "flex flex-col items-center w-full space-y-12"
                    ) {
                        input(
                            type = InputType.url,
                            classes = "text-4xl text-center w-full focus:outline-hidden w-full"
                        ) {
                            required = true
                            name = "url"
                            id = "url"
                            placeholder = "Your URL here."
                        }

                        hopButton { +"Go" }
                    }
                }
            }
        }
    }
}

inline fun FlowContent.hopButton(
    crossinline block: FlowContent.() -> Unit
) {
    button(
        classes = """
            text-white
            bg-blue-500
            box-border
            border border-transparent 
            hover:bg-blue-800 
            focus:ring-2 focus:ring-blue-300 
            shadow-xs font-medium leading-5 rounded-base text-sm px-4 py-2.5 focus:outline-none cursor-pointer w-xs
        """.trimIndent(),
        block = block
    )
}

fun HTML.shortUrlCreatedPage(
    createdUrl: String,
) {
    head {
        title { +"Success!" }
        script(src = "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4") { }
    }

    body {
        h1(classes = "text-3xl font-bold underline") { +"Your short URL is: $createdUrl" }
    }
}

inline fun FlowContent.mainLayout(
    crossinline block: FlowContent.() -> Unit
) {
    div(classes = "relative w-screen min-h-screen bg-slate-900") {
        div(classes = "flex w-screen h-screen items-center justify-center") {
            div(classes = "text-white p-10 flex flex-col items-center space-y-12 w-full") {
                block()
            }
        }
    }
}

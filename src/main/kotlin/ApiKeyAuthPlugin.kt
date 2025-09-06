package com.drbrosdev

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

internal fun ApplicationEnvironment.apiKey(): String {
    return requireNotNull(config.propertyOrNull("security.api_key"))
        .getString()
}

class ApiKeyAuthPluginConfiguration {
    private val _protectedPaths = mutableSetOf<String>()
    val protectedPaths: Set<String> = _protectedPaths

    fun protect(path: String) {
        _protectedPaths.add(path)
    }
}

val ApiKeyAuthPlugin = createApplicationPlugin(
    name = "ApiKeyAuthPlugin",
    createConfiguration = ::ApiKeyAuthPluginConfiguration
) {
    val apiKey = environment.apiKey()
    application.log.info("Initializing ApiKey plugin with $apiKey.")

    onCall { call ->
        val incoming = call.request.headers[HopHeaders.API_KEY]
        // Path here does not contain query parameters!
        val url = call.request.path()

        if (url in pluginConfig.protectedPaths) {
            when {
                apiKey != incoming -> call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}

fun Application.configureSecurity() {
    install(ApiKeyAuthPlugin) {
        protect("/")
    }
}


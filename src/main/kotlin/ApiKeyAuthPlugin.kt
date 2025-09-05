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
    private val _allowedPaths = mutableSetOf<String>()
    val allowedPaths: Set<String> = _allowedPaths

    fun allow(path: String) {
        _allowedPaths.add(path)
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
        val url = call.request.path()

        if (url !in pluginConfig.allowedPaths) {
            when {
                apiKey != incoming -> call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}

fun Application.configureSecurity() {
    install(ApiKeyAuthPlugin) {}
}


package com.drbrosdev

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

suspend fun Application.module() {
    configureSecurity()
    configureDatabase()
    configureFrameworks()
    configureSerialization()
    configureHTTP()
    configureRouting()
    configureShortUrlRoutes()
    configureClientRoutes()
}

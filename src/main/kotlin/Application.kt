package com.drbrosdev

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
    configureFrameworks()
    configureSerialization()
    configureHTTP()
    configureRouting()
    configureHopRoutes()
}

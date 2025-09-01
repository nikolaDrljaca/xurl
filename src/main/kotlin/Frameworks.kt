package com.drbrosdev

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import org.jetbrains.exposed.sql.Database

fun Application.configureFrameworks() {
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = "",
    )

    dependencies {
        provide {  }
    }
}

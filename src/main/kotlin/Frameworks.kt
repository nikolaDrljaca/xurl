package com.drbrosdev

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    // create instance
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = "",
    )
    // configure schemas
    transaction(database) {
        SchemaUtils.create(HopTable)
    }
}

fun Application.configureFrameworks() {

    dependencies {
        provide<CreateHop> { CreateHopImpl() }
        provide<FindHopByKey> { FindHopByKeyImpl() }
    }
}

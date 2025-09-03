package com.drbrosdev

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun Application.configureDatabase() {
    val url = environment.config.propertyOrNull("database.url")?.getString()
    requireNotNull(url) { "Database URL not specified!" }

    val database = Database.connect(
        url = url,
        driver = "org.sqlite.JDBC"
    )
    // set sqlite compatible isolation level
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
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

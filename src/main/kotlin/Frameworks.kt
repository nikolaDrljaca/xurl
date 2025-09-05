package com.drbrosdev

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun Application.configureDatabase() {
    val path = environment.config.propertyOrNull("database.url")?.getString()
    requireNotNull(path) { "Database URL not specified!" }
    val url = "$path?journal_mode=WAL&busy_timeout=5000&foreign_keys=true"

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

package com.drbrosdev

import glide.api.GlideClient
import glide.api.models.configuration.GlideClientConfiguration
import glide.api.models.configuration.NodeAddress
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.future.await
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

data class ValkeyConfiguration(
    val host: String,
    val port: Int
)

fun ApplicationEnvironment.valkeyConfiguration(): ValkeyConfiguration {
    val hostProp = requireNotNull(config.propertyOrNull("valkey.host")) {
        "Valkey Host not set! Check environment variables."
    }
    val portProp = requireNotNull(config.propertyOrNull("valkey.port")) {
        "Valkey Port not set! Check environment variables."
    }
    return ValkeyConfiguration(
        host = hostProp.getString(),
        port = portProp.getString().toInt()
    )
}

fun Application.configureFrameworks() {
    val valkeyConfig = environment.valkeyConfiguration()

    dependencies {
        provide<CreateHop> { CreateHopImpl() }
        provide<FindHopByKey> { FindHopByKeyImpl() }

        provide<GlideClient> {
            val config = GlideClientConfiguration.builder()
                .address(NodeAddress.builder()
                    .host(valkeyConfig.host)
                    .port(valkeyConfig.port)
                    .build())
                .requestTimeout(500)
                .build()

            GlideClient.createClient(config).await()
        } cleanup { it.close() }
    }
}

package com.drbrosdev

import io.ktor.http.*
import io.ktor.util.logging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomStringUtils
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

object HopTable: Table() {
    val id = uuid("id")
        .autoGenerate()

    val hopKey = text("hop_key")
        .uniqueIndex("hop_key_index")

    val longUrl = text("long_url")

    val createdAt = text("created_at")
        .default(LocalDate.now().format(DateTimeFormatter.ISO_DATE))

    override val primaryKey = PrimaryKey(id)
}

data class Hop(
    val id: UUID,
    val key: String,
    val url: String,
    val createdAt: LocalDate
)

fun Hop.dto() = HopDto(
    id = id.toString(),
    key = key,
    url = url,
    createdAt = createdAt.format(DateTimeFormatter.ISO_DATE)
)

@Serializable
data class HopDto(
    val id: String,
    val key: String,
    val url: String,
    val createdAt: String
)

fun interface CreateHop {
    suspend fun execute(url: String): Hop
}

internal val LOG = KtorSimpleLogger("HopLogger")

class CreateHopImpl: CreateHop {
    override suspend fun execute(url: String): Hop = query {
        // verify url is valid -> move to a domain model and smart constructor
        requireNotNull(parseUrl(url)) { "$url is not a valid URL." }
        // create new key and insert
        val row = retry {
            HopTable.insert {
                it[HopTable.hopKey] = createKey()
                it[HopTable.longUrl] = url
            }
        }
        requireNotNull(row) { "Unable to generate unique hop key!" }
        Hop(
            id = row[HopTable.id],
            key = row[HopTable.hopKey],
            url = row[HopTable.longUrl],
            createdAt = LocalDate.parse(row[HopTable.createdAt])
        ).also {
            LOG.info("Generated ${it.key} for ${it.url}.")
        }
    }

    private fun <T> retry(
        attempts: Int = 5,
        block: () -> T,
    ): T? {
        var result = runCatching { block() }
        var hasFailed = result.isFailure
        if (result.isSuccess) {
            return result.getOrNull()
        }

        var count = 0
        while (count < attempts && hasFailed) {
            count++
            result = runCatching { block() }
                .onFailure { println(it.localizedMessage) }
            hasFailed = result.isFailure
        }
        return result.getOrNull()
    }

    private fun createKey(): String {
        return RandomStringUtils.secure()
            .nextAlphabetic(7)
    }
}

fun interface FindHopByKey {
    suspend fun execute(key: String): Hop?
}

class FindHopByKeyImpl: FindHopByKey {
    override suspend fun execute(key: String): Hop? = query {
        HopTable.selectAll()
            .where { HopTable.hopKey eq key }
            .map { it.asHop() }
            .singleOrNull()
    }

    private fun ResultRow.asHop() = Hop(
        id = this[HopTable.id],
        key = this[HopTable.hopKey],
        url = this[HopTable.longUrl],
        createdAt = LocalDate.parse(this[HopTable.createdAt])
    )
}

internal suspend fun <T> query(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

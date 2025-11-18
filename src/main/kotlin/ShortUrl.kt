package com.drbrosdev

import io.ktor.http.*
import org.apache.commons.lang3.RandomStringUtils
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// DATA

object ShortUrlTable : Table() {
    val id = uuid("id")
        .autoGenerate()

    val key = text("key")
        .uniqueIndex("key_index")

    val longUrl = text("long_url")

    val createdAt = text("created_at")
        .default(LocalDate.now().format(DateTimeFormatter.ISO_DATE))

    override val primaryKey = PrimaryKey(id)
}

// Domain

data class ShortUrl(
    val id: UUID,
    val key: String,
    val url: String,
    val createdAt: LocalDate
)

// Use Cases

fun interface CreateShortUrl {
    suspend fun execute(url: String): ShortUrl
}

class CreateShortUrlImpl : CreateShortUrl {
    override suspend fun execute(url: String): ShortUrl = transaction {
        // verify url is valid -> move to a domain model and smart constructor
        requireNotNull(parseUrl(url)) { "$url is not a valid URL." }
        // create new key and insert
        val row = retry {
            ShortUrlTable.insert {
                it[ShortUrlTable.key] = createKey()
                it[ShortUrlTable.longUrl] = url
            }
        }
        requireNotNull(row) { "Unable to generate unique hop key!" }
        ShortUrl(
            id = row[ShortUrlTable.id],
            key = row[ShortUrlTable.key],
            url = row[ShortUrlTable.longUrl],
            createdAt = LocalDate.parse(row[ShortUrlTable.createdAt])
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

fun interface FindShortUrlByKey {
    suspend fun execute(key: String): ShortUrl?
}

class FindShortUrlByKeyImpl : FindShortUrlByKey {
    override suspend fun execute(key: String): ShortUrl? = transaction {
        ShortUrlTable.selectAll()
            .where { ShortUrlTable.key eq key }
            .map { it.asHop() }
            .singleOrNull()
    }

    private fun ResultRow.asHop() = ShortUrl(
        id = this[ShortUrlTable.id],
        key = this[ShortUrlTable.key],
        url = this[ShortUrlTable.longUrl],
        createdAt = LocalDate.parse(this[ShortUrlTable.createdAt])
    )
}

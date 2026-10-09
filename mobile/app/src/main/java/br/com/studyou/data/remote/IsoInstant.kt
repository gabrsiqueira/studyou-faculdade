package br.com.studyou.data.remote

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val ISO_INSTANT = Regex("""^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(\.\d+)?(Z|[+-]\d{2}:\d{2})?$""")

/**
 * Parses timestamps like "2026-10-17T18:09:18.364140Z" or "...+00:00" as sent by the backend.
 * Written by hand because java.time isn't available below API 26 (minSdk is 24).
 */
fun parseIsoInstantMillis(value: String): Long {
    val match = ISO_INSTANT.matchEntire(value.trim()) ?: throw IllegalArgumentException("Data inválida: $value")
    val (dateTime, fraction, offset) = match.destructured

    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val baseMillis = format.parse(dateTime)!!.time
    val fractionMillis = fraction.drop(1).padEnd(3, '0').take(3).toLongOrNull() ?: 0L

    val offsetMillis = if (offset.isEmpty() || offset == "Z") {
        0L
    } else {
        val sign = if (offset.startsWith("-")) -1 else 1
        val (hours, minutes) = offset.drop(1).split(":").map { it.toLong() }
        sign * (hours * 3_600_000 + minutes * 60_000)
    }
    return baseMillis + fractionMillis - offsetMillis
}

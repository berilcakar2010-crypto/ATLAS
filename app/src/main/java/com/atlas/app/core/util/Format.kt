package com.atlas.app.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    val turkish: Locale = Locale.forLanguageTag("tr-TR")

    private val timestampFormatter = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", turkish)

    fun timestamp(epochMillis: Long): String =
        timestampFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Günaydın."
        in 12..17 -> "İyi günler."
        in 18..22 -> "İyi akşamlar."
        else -> "İyi geceler."
    }
}

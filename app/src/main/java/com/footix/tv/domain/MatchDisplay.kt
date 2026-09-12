package com.footix.tv.domain

import com.footix.tv.domain.model.Match
import java.util.Calendar
import java.util.Locale

/** Titre de carte : "Domicile - Exterieur", ou le seul nom disponible. */
val Match.displayTitle: String
    get() = when {
        home.name.isBlank() -> away.name
        away.name.isBlank() -> home.name
        else -> "${home.name} - ${away.name}"
    }

/** Sous-titre de carte : jour (si ce n'est pas aujourd'hui), heure et chaine. */
val Match.displaySubtitle: String
    get() = listOf(dayLabel, time, channel)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

private val Match.dayLabel: String
    get() {
        if (date.isBlank() || date == isoToday()) return ""
        val parts = date.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}" else date
    }

private fun isoToday(): String {
    val calendar = Calendar.getInstance()
    return String.format(
        Locale.ROOT,
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

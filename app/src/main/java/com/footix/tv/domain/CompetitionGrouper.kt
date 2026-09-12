package com.footix.tv.domain

import com.footix.tv.core.AppConfig
import com.footix.tv.domain.model.CompetitionSection
import com.footix.tv.domain.model.Match
import java.util.Locale

object CompetitionGrouper {

    const val LIVE_SECTION_TITLE = "En direct"
    const val FALLBACK_SECTION_TITLE = "Autres competitions"

    /**
     * Regroupe les matchs par competition. Les competitions qui ont un match en
     * direct remontent en tete, puis l'ordre est alphabetique.
     */
    fun group(
        matches: List<Match>,
        withLiveSection: Boolean = AppConfig.SHOW_LIVE_ROW
    ): List<CompetitionSection> {
        if (matches.isEmpty()) return emptyList()

        val sections = matches
            .groupBy { it.competition.trim().ifBlank { FALLBACK_SECTION_TITLE } }
            .map { (title, group) ->
                CompetitionSection(
                    title = title,
                    logoUrl = group.firstOrNull { it.competitionLogoUrl.isNotBlank() }?.competitionLogoUrl.orEmpty(),
                    matches = group.sortedWith(matchOrder)
                )
            }
            .sortedWith(sectionOrder)

        val live = matches.filter { it.isLive }.sortedWith(matchOrder)
        return if (withLiveSection && live.isNotEmpty()) {
            listOf(CompetitionSection(LIVE_SECTION_TITLE, "", live)) + sections
        } else {
            sections
        }
    }

    private val matchOrder: Comparator<Match> =
        compareByDescending<Match> { it.isLive }
            .thenBy { it.date }
            .thenBy { it.time }
            .thenBy { it.displayTitle.lowercase(Locale.ROOT) }

    private val sectionOrder: Comparator<CompetitionSection> =
        compareByDescending<CompetitionSection> { section -> section.matches.any { it.isLive } }
            .thenBy { it.title.lowercase(Locale.ROOT) }
}

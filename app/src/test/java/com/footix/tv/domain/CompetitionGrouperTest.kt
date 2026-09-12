package com.footix.tv.domain

import com.footix.tv.domain.model.Match
import com.footix.tv.domain.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompetitionGrouperTest {

    @Test
    fun `regroupe les matchs par competition`() {
        val sections = CompetitionGrouper.group(
            listOf(
                match(slug = "a", competition = "La Liga"),
                match(slug = "b", competition = "Ligue 1"),
                match(slug = "c", competition = "La Liga")
            ),
            withLiveSection = false
        )

        assertEquals(listOf("La Liga", "Ligue 1"), sections.map { it.title })
        assertEquals(2, sections.first().matches.size)
    }

    @Test
    fun `place la rangee direct en tete`() {
        val sections = CompetitionGrouper.group(
            listOf(
                match(slug = "a", competition = "La Liga"),
                match(slug = "b", competition = "Ligue 1", isLive = true)
            )
        )

        assertEquals(CompetitionGrouper.LIVE_SECTION_TITLE, sections.first().title)
        assertEquals(listOf("b"), sections.first().matches.map { it.slug })
    }

    @Test
    fun `fait remonter les competitions qui ont un direct`() {
        val sections = CompetitionGrouper.group(
            listOf(
                match(slug = "a", competition = "Serie A"),
                match(slug = "b", competition = "Ligue 1", isLive = true)
            ),
            withLiveSection = false
        )

        assertEquals("Ligue 1", sections.first().title)
    }

    @Test
    fun `trie les matchs par heure`() {
        val sections = CompetitionGrouper.group(
            listOf(
                match(slug = "tard", competition = "Ligue 1", time = "21:00"),
                match(slug = "tot", competition = "Ligue 1", time = "17:00")
            ),
            withLiveSection = false
        )

        assertEquals(listOf("tot", "tard"), sections.first().matches.map { it.slug })
    }

    @Test
    fun `range les matchs sans competition dans une section de repli`() {
        val sections = CompetitionGrouper.group(
            listOf(match(slug = "a", competition = "")),
            withLiveSection = false
        )

        assertTrue(sections.single().title == CompetitionGrouper.FALLBACK_SECTION_TITLE)
    }

    private fun match(
        slug: String,
        competition: String,
        isLive: Boolean = false,
        time: String = "20:00"
    ) = Match(
        slug = slug,
        date = "2026-09-11",
        time = time,
        home = Team("Domicile", ""),
        away = Team("Exterieur", ""),
        isLive = isLive,
        sport = "football",
        competition = competition,
        competitionLogoUrl = "",
        posterUrl = "",
        channel = "",
        hasSources = true
    )
}

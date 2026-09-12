package com.footix.tv.data.remote

import com.footix.tv.core.ApiException
import com.footix.tv.domain.MatchFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseParserTest {

    private val catalogJson = """
        {
          "today": [
            {
              "slug": "one-friday-fights-170",
              "date": "2026-09-11",
              "time": "15:30",
              "home": { "name": "ONE Friday Fights 170", "logo": "" },
              "away": { "name": "Exterieur", "logo": "" },
              "isLive": false,
              "sport": "boxe",
              "competition": "boxe",
              "poster": "/img/scraped/boxe-card.webp",
              "channel": "Rmc 2",
              "hasSources": true
            },
            {
              "slug": "sevilla-vs-valencia-2026-09-11",
              "date": "2026-09-11",
              "time": "21:00",
              "home": { "name": "Sevilla", "logo": "https://cdn.test/teams/sevilla.png" },
              "away": { "name": "Valencia", "logo": "https://cdn.test/teams/valencia.png" },
              "isLive": true,
              "sport": "football",
              "competition": "La Liga",
              "compLogo": "https://cdn.test/laliga.png",
              "poster": "/img/scraped/sevilla-card.webp",
              "channel": "Liga 1",
              "tvChannel": "Canal+",
              "hasSources": true
            }
          ],
          "tomorrow": [
            {
              "slug": "psg-vs-lyon-2026-09-12",
              "date": "2026-09-12",
              "time": "20:45",
              "home": { "name": "PSG", "logo": "" },
              "away": { "name": "Lyon", "logo": "" },
              "isLive": false,
              "sport": "football",
              "competition": "Ligue 1",
              "poster": "",
              "channel": "Ligue 1+",
              "hasSources": false
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `fusionne toutes les journees du catalogue`() {
        assertEquals(3, ResponseParser.parseMatches(catalogJson).size)
    }

    @Test
    fun `ne garde que le football`() {
        val football = MatchFilter.keepFootball(ResponseParser.parseMatches(catalogJson))

        assertEquals(listOf("La Liga", "Ligue 1"), football.map { it.competition })
    }

    @Test
    fun `rend absolues les urls relatives et prefere la chaine tv`() {
        val match = ResponseParser.parseMatches(catalogJson).first { it.slug.startsWith("sevilla") }

        assertTrue(match.posterUrl.endsWith("/img/scraped/sevilla-card.webp"))
        assertTrue(match.posterUrl.startsWith("https://"))
        assertEquals("Canal+", match.channel)
    }

    @Test
    fun `lit les sources d'un flux`() {
        val json = """
            {
              "success": true,
              "sources": [
                { "label": "Source 1", "url": "https://cdn.test/hls/valencia/index.m3u8?md5=x", "iframeNoSandbox": false }
              ],
              "expiresIn": 14400,
              "channel": "Liga 1",
              "competition": "La Liga"
            }
        """.trimIndent()

        val stream = ResponseParser.parseStream(json)

        assertEquals(1, stream.sources.size)
        assertEquals("La Liga", stream.competition)
    }

    @Test(expected = ApiException::class)
    fun `refuse un flux marque en echec`() {
        ResponseParser.parseStream("""{ "success": false, "sources": [] }""")
    }
}

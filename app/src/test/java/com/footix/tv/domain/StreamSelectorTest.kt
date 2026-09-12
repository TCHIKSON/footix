package com.footix.tv.domain

import com.footix.tv.domain.model.StreamSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamSelectorTest {

    @Test
    fun `coupe l'url juste apres m3u8`() {
        val url = "https://cdn.test/hls/valencia/index.m3u8?md5=abc&expires=1789167548"

        assertEquals(
            "https://cdn.test/hls/valencia/index.m3u8",
            StreamSelector.sanitize(url)
        )
    }

    @Test
    fun `conserve l'url complete quand le nettoyage est desactive`() {
        val url = "https://cdn.test/hls/index.m3u8?md5=abc"

        assertEquals(url, StreamSelector.sanitize(url, stripQuery = false))
    }

    @Test
    fun `laisse intacte une url sans m3u8`() {
        val url = "https://cdn.test/live/stream"

        assertEquals(url, StreamSelector.sanitize(url))
    }

    @Test
    fun `rejette une url vide`() {
        assertNull(StreamSelector.sanitize("   "))
    }

    @Test
    fun `retient la premiere source hls du tableau`() {
        val sources = listOf(
            StreamSource("Source 1", "https://cdn.test/embed.html"),
            StreamSource("Source 2", "https://cdn.test/index.m3u8?token=1")
        )

        assertEquals("Source 2", StreamSelector.pick(sources)?.label)
    }
}

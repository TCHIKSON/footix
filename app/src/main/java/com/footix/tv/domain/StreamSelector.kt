package com.footix.tv.domain

import com.footix.tv.core.AppConfig
import com.footix.tv.domain.model.StreamSource

object StreamSelector {

    private const val MARKER = ".m3u8"

    /** Retient la premiere source HLS exploitable du tableau "sources". */
    fun pick(sources: List<StreamSource>): StreamSource? =
        sources.firstOrNull { it.url.contains(MARKER, ignoreCase = true) }
            ?: sources.firstOrNull { it.url.isNotBlank() }

    /**
     * Conserve l'URL jusqu'a ".m3u8" inclus et abandonne la query string.
     * Comportement desactivable via [AppConfig.STRIP_STREAM_QUERY].
     */
    fun sanitize(url: String, stripQuery: Boolean = AppConfig.STRIP_STREAM_QUERY): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        if (!stripQuery) return trimmed

        val marker = trimmed.indexOf(MARKER, ignoreCase = true)
        return if (marker >= 0) trimmed.substring(0, marker + MARKER.length) else trimmed
    }
}

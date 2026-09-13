package com.footix.tv.data.repository

import com.footix.tv.core.Logger
import com.footix.tv.core.StreamUnavailableException
import com.footix.tv.data.remote.FootixApi
import com.footix.tv.domain.StreamSelector
import com.footix.tv.domain.model.Match
import com.footix.tv.domain.model.PlayableStream

/** Resout le flux jouable d'un match a partir de son slug. */
class StreamRepository(private val api: FootixApi) {

    suspend fun resolve(match: Match): PlayableStream {
        if (!match.hasSources) throw StreamUnavailableException()
        val stream = resolve(match.slug)
        return if (stream.channel.isBlank()) stream.copy(channel = match.channel) else stream
    }

    /** Variante pour le lecteur, qui ne transporte que le slug du match. */
    suspend fun resolve(slug: String): PlayableStream {
        val stream = api.stream(slug)
        val source = StreamSelector.pick(stream.sources) ?: throw StreamUnavailableException()
        val url = StreamSelector.sanitize(source.url) ?: throw StreamUnavailableException()
        Logger.d("Flux retenu pour $slug: $url")

        return PlayableStream(url = url, label = source.label, channel = stream.channel)
    }
}

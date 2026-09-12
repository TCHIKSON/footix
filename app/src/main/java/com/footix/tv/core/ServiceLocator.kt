package com.footix.tv.core

import com.footix.tv.data.remote.FootixApi
import com.footix.tv.data.remote.HttpClientFactory
import com.footix.tv.data.repository.MatchRepository
import com.footix.tv.data.repository.StreamRepository

/**
 * Cablage manuel des dependances : suffisant pour une application mono-ecran
 * et evite d'embarquer un framework d'injection sur une box TV.
 */
object ServiceLocator {

    private val httpClient by lazy { HttpClientFactory.create() }

    private val api by lazy { FootixApi(httpClient) }

    val matchRepository by lazy { MatchRepository(api) }

    val streamRepository by lazy { StreamRepository(api) }
}

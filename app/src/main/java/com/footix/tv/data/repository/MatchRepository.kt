package com.footix.tv.data.repository

import com.footix.tv.core.AppConfig
import com.footix.tv.core.Logger
import com.footix.tv.data.remote.FootixApi
import com.footix.tv.domain.CompetitionGrouper
import com.footix.tv.domain.MatchFilter
import com.footix.tv.domain.model.CompetitionSection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Charge le catalogue, ne garde que le football et le regroupe par competition.
 * Le resultat est conserve en memoire pendant [AppConfig.CATALOG_TTL_MS].
 */
class MatchRepository(
    private val api: FootixApi,
    private val now: () -> Long = System::currentTimeMillis
) {

    private val mutex = Mutex()
    private var cache: List<CompetitionSection> = emptyList()
    private var cachedAt = 0L

    suspend fun sections(forceRefresh: Boolean = false): List<CompetitionSection> = mutex.withLock {
        val isFresh = cachedAt != 0L && now() - cachedAt < AppConfig.CATALOG_TTL_MS
        if (!forceRefresh && isFresh) return cache

        val all = api.matches()
        val football = MatchFilter.keepFootball(all)
        cache = CompetitionGrouper.group(football)
        Logger.d("Catalogue: ${all.size} matchs, ${football.size} football, ${cache.size} rangees")
        cachedAt = now()
        return cache
    }

    fun invalidate() {
        cachedAt = 0L
    }
}

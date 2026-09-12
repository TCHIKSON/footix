package com.footix.tv.domain

import com.footix.tv.core.AppConfig
import com.footix.tv.domain.model.Match
import java.util.Locale

object MatchFilter {

    fun isFootball(match: Match, keyword: String = AppConfig.SPORT_KEYWORD): Boolean =
        match.sport.lowercase(Locale.ROOT).contains(keyword.lowercase(Locale.ROOT))

    fun keepFootball(matches: List<Match>, keyword: String = AppConfig.SPORT_KEYWORD): List<Match> =
        matches.filter { isFootball(it, keyword) }
}

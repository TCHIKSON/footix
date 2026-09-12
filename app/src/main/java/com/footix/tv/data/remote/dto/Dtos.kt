package com.footix.tv.data.remote.dto

/**
 * Reflet direct du JSON : tous les champs sont optionnels pour absorber les
 * variations de l'API sans faire echouer le parsing.
 */
data class MatchDto(
    val slug: String? = null,
    val date: String? = null,
    val time: String? = null,
    val home: TeamDto? = null,
    val away: TeamDto? = null,
    val isLive: Boolean? = null,
    val sport: String? = null,
    val competition: String? = null,
    val sportLogo: String? = null,
    val leagueLogo: String? = null,
    val compLogo: String? = null,
    val poster: String? = null,
    val channel: String? = null,
    val tvChannel: String? = null,
    val hasSources: Boolean? = null
)

data class TeamDto(
    val name: String? = null,
    val logo: String? = null
)

data class StreamResponseDto(
    val success: Boolean? = null,
    val sources: List<StreamSourceDto>? = null,
    val channel: String? = null,
    val tvChannel: String? = null,
    val competition: String? = null
)

data class StreamSourceDto(
    val label: String? = null,
    val url: String? = null
)

package com.footix.tv.data.remote

import com.footix.tv.data.remote.dto.MatchDto
import com.footix.tv.data.remote.dto.StreamResponseDto
import com.footix.tv.data.remote.dto.StreamSourceDto
import com.footix.tv.data.remote.dto.TeamDto
import com.footix.tv.domain.model.Match
import com.footix.tv.domain.model.MatchStream
import com.footix.tv.domain.model.StreamSource
import com.footix.tv.domain.model.Team

object DtoMapper {

    /** Un match sans slug ne peut pas etre lu : il est ecarte. */
    fun toMatch(dto: MatchDto): Match? {
        val slug = dto.slug?.trim().orEmpty()
        if (slug.isEmpty()) return null

        return Match(
            slug = slug,
            date = dto.date.orEmpty(),
            time = dto.time.orEmpty(),
            home = toTeam(dto.home),
            away = toTeam(dto.away),
            isLive = dto.isLive ?: false,
            sport = dto.sport.orEmpty(),
            competition = dto.competition.orEmpty(),
            competitionLogoUrl = UrlResolver.absolute(
                dto.compLogo ?: dto.leagueLogo ?: dto.sportLogo
            ),
            posterUrl = UrlResolver.absolute(dto.poster),
            channel = dto.tvChannel?.takeIf { it.isNotBlank() } ?: dto.channel.orEmpty(),
            hasSources = dto.hasSources ?: true
        )
    }

    fun toStream(dto: StreamResponseDto): MatchStream = MatchStream(
        sources = dto.sources.orEmpty().mapNotNull(::toSource),
        channel = dto.tvChannel?.takeIf { it.isNotBlank() } ?: dto.channel.orEmpty(),
        competition = dto.competition.orEmpty()
    )

    private fun toTeam(dto: TeamDto?): Team = Team(
        name = dto?.name.orEmpty(),
        logoUrl = UrlResolver.absolute(dto?.logo)
    )

    private fun toSource(dto: StreamSourceDto): StreamSource? {
        val url = dto.url?.trim().orEmpty()
        if (url.isEmpty()) return null
        return StreamSource(label = dto.label.orEmpty(), url = url)
    }
}

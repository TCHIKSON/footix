package com.footix.tv.domain.model

data class Team(
    val name: String,
    val logoUrl: String
)

data class Match(
    val slug: String,
    val date: String,
    val time: String,
    val home: Team,
    val away: Team,
    val isLive: Boolean,
    val sport: String,
    val competition: String,
    val competitionLogoUrl: String,
    val posterUrl: String,
    val channel: String,
    val hasSources: Boolean
)

/** Une rangee de l'ecran d'accueil : une competition et ses matchs. */
data class CompetitionSection(
    val title: String,
    val logoUrl: String,
    val matches: List<Match>
)

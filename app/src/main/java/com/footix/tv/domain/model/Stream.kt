package com.footix.tv.domain.model

data class StreamSource(
    val label: String,
    val url: String
)

/** Reponse brute de l'endpoint stream, une fois nettoyee. */
data class MatchStream(
    val sources: List<StreamSource>,
    val channel: String,
    val competition: String
)

/** Flux pret a etre envoye au lecteur. */
data class PlayableStream(
    val url: String,
    val label: String,
    val channel: String
)

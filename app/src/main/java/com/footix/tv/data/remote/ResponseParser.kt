package com.footix.tv.data.remote

import com.footix.tv.core.ApiException
import com.footix.tv.data.remote.dto.MatchDto
import com.footix.tv.data.remote.dto.StreamResponseDto
import com.footix.tv.domain.model.Match
import com.footix.tv.domain.model.MatchStream
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

object ResponseParser {

    private val gson = Gson()
    private val matchListType = object : TypeToken<List<MatchDto>>() {}.type

    /**
     * La racine est un objet dont chaque cle est un jour ("today", ...). Toutes
     * les listes rencontrees sont fusionnees ; une racine deja sous forme de
     * tableau est egalement acceptee.
     */
    fun parseMatches(json: String): List<Match> {
        val arrays = when (val root = parseRoot(json)) {
            is JsonArray -> listOf(root)
            else -> root.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.entrySet()
                ?.mapNotNull { entry -> entry.value.takeIf(JsonElement::isJsonArray)?.asJsonArray }
                .orEmpty()
        }

        return arrays
            .flatMap { array -> gson.fromJson<List<MatchDto>>(array, matchListType).orEmpty() }
            .mapNotNull(DtoMapper::toMatch)
    }

    fun parseStream(json: String): MatchStream {
        val dto = runCatching { gson.fromJson(json, StreamResponseDto::class.java) }
            .getOrElse { throw ApiException("Reponse stream illisible", it) }
            ?: throw ApiException("Reponse stream vide")

        if (dto.success == false) throw ApiException("Le serveur a refuse le flux")
        return DtoMapper.toStream(dto)
    }

    private fun parseRoot(json: String): JsonElement =
        try {
            JsonParser.parseString(json)
        } catch (e: JsonSyntaxException) {
            throw ApiException("Reponse catalogue illisible", e)
        }
}

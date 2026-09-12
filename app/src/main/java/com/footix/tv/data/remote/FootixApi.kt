package com.footix.tv.data.remote

import com.footix.tv.core.ApiException
import com.footix.tv.core.AppConfig
import com.footix.tv.core.Logger
import com.footix.tv.domain.model.Match
import com.footix.tv.domain.model.MatchStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

/** Les deux seuls appels reseau de l'application. */
class FootixApi(
    private val client: OkHttpClient,
    private val baseUrl: String = AppConfig.BASE_URL
) {

    suspend fun matches(): List<Match> = withContext(Dispatchers.IO) {
        ResponseParser.parseMatches(get(baseUrl.trimEnd('/') + AppConfig.MATCHES_PATH))
    }

    suspend fun stream(slug: String): MatchStream = withContext(Dispatchers.IO) {
        val url = baseUrl.trimEnd('/') + AppConfig.STREAM_PATH + URLEncoder.encode(slug, "UTF-8")
        ResponseParser.parseStream(get(url))
    }

    private fun get(url: String): String {
        Logger.d("GET $url")
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ApiException("HTTP ${response.code}")
                val body = response.body?.string()?.takeIf { it.isNotBlank() }
                    ?: throw ApiException("Reponse vide")
                Logger.d("HTTP ${response.code}, ${body.length} caracteres")
                return body
            }
        } catch (e: IOException) {
            Logger.w("Echec de l'appel $url", e)
            throw e
        }
    }
}

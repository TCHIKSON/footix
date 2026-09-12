package com.footix.tv.data.remote

import com.footix.tv.core.AppConfig

/** Transforme les chemins relatifs renvoyes par l'API en URL absolues. */
object UrlResolver {

    fun absolute(path: String?, baseUrl: String = AppConfig.BASE_URL): String {
        val value = path?.trim().orEmpty()
        return when {
            value.isEmpty() -> ""
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.startsWith("//") -> "https:$value"
            value.startsWith("/") -> baseUrl.trimEnd('/') + value
            else -> baseUrl.trimEnd('/') + "/" + value
        }
    }
}

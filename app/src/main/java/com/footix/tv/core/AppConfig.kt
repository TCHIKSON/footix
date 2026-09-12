package com.footix.tv.core

/**
 * Point unique de configuration. Tout ce qui peut changer cote serveur ou cote
 * confort de lecture se regle ici, sans toucher au reste du code.
 */
object AppConfig {

    // --- API ---------------------------------------------------------------
    const val BASE_URL = "https://kzreborn.space"
    const val MATCHES_PATH = "/api/index.php?table=matches"
    const val STREAM_PATH = "/api/?action=stream&slug="

    /** Seuls les matchs dont le champ "sport" contient ce mot sont affiches. */
    const val SPORT_KEYWORD = "football"

    // --- Reseau ------------------------------------------------------------
    const val HTTP_TIMEOUT_SECONDS = 20L
    const val HTTP_USER_AGENT = "FootixTV/1.0 (Android TV)"

    /** Duree de validite du catalogue en memoire avant nouvel appel reseau. */
    const val CATALOG_TTL_MS = 5 * 60 * 1000L

    // --- Catalogue ---------------------------------------------------------
    /** Ajoute une premiere rangee regroupant les matchs en direct. */
    const val SHOW_LIVE_ROW = true

    // --- Lecture -----------------------------------------------------------
    /**
     * Tronque l'URL du flux juste apres ".m3u8". A passer a false si le serveur
     * exige les parametres de signature (md5, expires) presents apres le "?".
     */
    const val STRIP_STREAM_QUERY = true

    const val PLAYER_NETWORK_CACHING_MS = 1500
    const val PLAYER_MAX_RETRIES = 5
    const val PLAYER_RETRY_DELAY_MS = 3_000L
    const val PLAYER_SEEK_STEP_MS = 10_000L
    const val PLAYER_OVERLAY_TIMEOUT_MS = 5_000L

    /** En-tetes optionnels exiges par certains CDN. null = non envoye. */
    val PLAYER_USER_AGENT: String? = null
    val PLAYER_REFERER: String? = null
}

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

    // --- Interface ---------------------------------------------------------
    /** Duree d'un battement du logo pendant les chargements. */
    const val LOADING_PULSE_DURATION_MS = 700L

    // --- Lecture -----------------------------------------------------------
    /**
     * Tronque l'URL du flux juste apres ".m3u8". A passer a false si le serveur
     * exige les parametres de signature (md5, expires) presents apres le "?".
     */
    const val STRIP_STREAM_QUERY = true

    /**
     * Demarre plus pres du bord du direct. A repasser a false si l'image se fige
     * souvent : la marge d'avance qui absorbait les a-coups du reseau se reduit.
     */
    const val PLAYER_LOW_LATENCY = true

    /**
     * Retard vise par rapport au direct, en millisecondes. Ne peut pas descendre
     * sous la duree d'un segment de la source (4 s environ ici) : le serveur ne
     * publie un segment qu'une fois ecrit en entier.
     */
    const val PLAYER_LIVE_DELAY_MS = 8000

    /**
     * Retard maximum tolere face au direct. Chaque gel d'image et chaque pause
     * eloignent la lecture du bord du direct sans jamais la rattraper : passe ce
     * seuil, le flux est recharge pour repartir au plus pres, au prix d'une a
     * deux secondes de rechargement. Mettre 0 pour desactiver la surveillance.
     */
    const val PLAYER_MAX_DELAY_MS = 12_000L

    /** Frequence de controle du retard accumule. */
    const val PLAYER_DELAY_CHECK_INTERVAL_MS = 5_000L

    /** Memoire tampon reseau, selon le mode de latence choisi ci-dessus. */
    const val PLAYER_NETWORK_CACHING_MS = 1500
    const val PLAYER_LOW_LATENCY_CACHING_MS = 1000
    const val PLAYER_MAX_RETRIES = 4

    /**
     * Tentative a partir de laquelle on redemande l'URL au serveur au lieu de
     * rejouer la meme : le flux d'un match peut changer de machine en cours de
     * route. Un dernier appel a lieu aussi apres la derniere tentative.
     */
    const val PLAYER_REFRESH_URL_AT_ATTEMPT = 3
    const val PLAYER_RETRY_DELAY_MS = 3_000L
    const val PLAYER_SEEK_STEP_MS = 10_000L
    const val PLAYER_OVERLAY_TIMEOUT_MS = 5_000L

    /** En-tetes optionnels exiges par certains CDN. null = non envoye. */
    val PLAYER_USER_AGENT: String? = null
    val PLAYER_REFERER: String? = null
}

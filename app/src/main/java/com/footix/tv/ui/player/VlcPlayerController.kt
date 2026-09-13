package com.footix.tv.ui.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.footix.tv.core.AppConfig
import com.footix.tv.core.Logger
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Encapsule libVLC : creation du moteur, lecture HLS, relance automatique.
 * L'activite ne manipule jamais LibVLC directement.
 *
 * A construire depuis le thread principal : les evenements libVLC y sont
 * ensuite delivres, ce qui permet de toucher l'interface sans repostage.
 */
class VlcPlayerController(context: Context) {

    interface Listener {
        fun onBuffering(percent: Float)
        fun onPlaying()
        fun onPaused()
        fun onRetrying(attempt: Int, maxAttempts: Int)
        fun onResolvingFreshUrl()
        fun onUnrecoverableError()
    }

    /**
     * Redemande au serveur l'URL du flux en cours. Le controleur ignore tout du
     * reseau : c'est l'activite qui fournit cette resolution. Le rappel recoit
     * null si l'appel echoue.
     */
    fun interface UrlResolver {
        fun resolve(onResult: (String?) -> Unit)
    }

    var urlResolver: UrlResolver? = null

    private val libVlc = LibVLC(context.applicationContext, engineOptions())
    private val player = MediaPlayer(libVlc)
    private val handler = Handler(Looper.getMainLooper())

    var listener: Listener? = null

    private var currentUrl: String? = null
    private var retryCount = 0
    private var finalResolveDone = false

    // Reference de mesure du retard : horloge murale et position dans le flux au
    // moment ou la lecture a demarre. L'ecart entre les deux est la derive.
    private var syncWallClock = 0L
    private var syncMediaTime = 0L
    private var awaitingSyncReset = false

    // Accesseur et non propriete initialisee : engineOptions() est appele avant
    // que les proprietes de la classe ne soient construites.
    private val cachingMs: Int
        get() = if (AppConfig.PLAYER_LOW_LATENCY) AppConfig.PLAYER_LOW_LATENCY_CACHING_MS
        else AppConfig.PLAYER_NETWORK_CACHING_MS

    init {
        player.setEventListener(MediaPlayer.EventListener { event -> onVlcEvent(event) })
    }

    val isPlaying: Boolean get() = player.isPlaying

    fun attach(videoLayout: VLCVideoLayout) {
        player.attachViews(videoLayout, null, false, false)
    }

    fun detach() {
        handler.removeCallbacksAndMessages(null)
        player.stop()
        player.detachViews()
    }

    fun play(url: String) {
        currentUrl = url
        resetRetryState()
        start(url)
    }

    /** Relance manuelle apres abandon (compteur remis a zero). */
    fun retry() {
        val url = currentUrl ?: return
        handler.removeCallbacksAndMessages(null)
        resetRetryState()
        start(url)
    }

    private fun resetRetryState() {
        retryCount = 0
        finalResolveDone = false
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekBy(deltaMs: Long) {
        if (!player.isSeekable) return
        player.time = (player.time + deltaMs).coerceAtLeast(0L)
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        listener = null
        player.setEventListener(null as MediaPlayer.EventListener?)
        player.release()
        libVlc.release()
    }

    private fun start(url: String) {
        awaitingSyncReset = true
        val media = Media(libVlc, Uri.parse(url)).apply {
            setHWDecoderEnabled(true, false)
            addOption(":network-caching=$cachingMs")
            addOption(":live-caching=$cachingMs")
            addOption(":file-caching=$cachingMs")
            addOption(":clock-jitter=0")
            addOption(":clock-synchro=0")
            AppConfig.PLAYER_USER_AGENT?.let { addOption(":http-user-agent=$it") }
            AppConfig.PLAYER_REFERER?.let { addOption(":http-referrer=$it") }
        }
        player.media = media
        media.release()
        player.play()
    }

    private fun onVlcEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Buffering -> listener?.onBuffering(event.buffering)

            MediaPlayer.Event.Playing -> {
                resetRetryState()
                // Une reprise apres pause ne remet pas la reference a zero : le
                // temps passe en pause fait partie du retard a rattraper.
                if (awaitingSyncReset) {
                    awaitingSyncReset = false
                    syncWallClock = SystemClock.elapsedRealtime()
                    syncMediaTime = player.time
                }
                scheduleDelayCheck()
                listener?.onPlaying()
            }

            MediaPlayer.Event.Paused -> listener?.onPaused()

            // Sur un direct, EndReached signifie que la source a coupe.
            MediaPlayer.Event.EncounteredError, MediaPlayer.Event.EndReached -> scheduleRetry()
        }
    }

    private val delayWatchdog = object : Runnable {
        override fun run() {
            checkDelay()
            handler.postDelayed(this, AppConfig.PLAYER_DELAY_CHECK_INTERVAL_MS)
        }
    }

    private fun scheduleDelayCheck() {
        if (AppConfig.PLAYER_MAX_DELAY_MS <= 0L) return
        handler.removeCallbacks(delayWatchdog)
        handler.postDelayed(delayWatchdog, AppConfig.PLAYER_DELAY_CHECK_INTERVAL_MS)
    }

    /**
     * Retard estime = retard de depart + derive accumulee. La derive est le temps
     * ecoule en plus de ce que la lecture a reellement consomme : gels, pauses,
     * relances. Au-dela du plafond, on recharge le flux pour repartir au bord.
     */
    private fun checkDelay() {
        val url = currentUrl ?: return
        if (!player.isPlaying) return

        val drift = (SystemClock.elapsedRealtime() - syncWallClock) - (player.time - syncMediaTime)
        val estimatedDelay = AppConfig.PLAYER_LIVE_DELAY_MS + drift
        if (estimatedDelay < AppConfig.PLAYER_MAX_DELAY_MS) return

        Logger.d("Retard estime $estimatedDelay ms, resynchronisation sur le direct")
        start(url)
    }

    private fun scheduleRetry() {
        val url = currentUrl ?: return

        if (retryCount >= AppConfig.PLAYER_MAX_RETRIES) {
            // Toutes les tentatives ont echoue : un dernier appel au serveur, au
            // cas ou l'adresse du flux aurait change, puis on abandonne.
            if (finalResolveDone) {
                listener?.onUnrecoverableError()
                return
            }
            finalResolveDone = true
            handler.postDelayed({ restartWithFreshUrl() }, AppConfig.PLAYER_RETRY_DELAY_MS)
            return
        }

        retryCount++
        listener?.onRetrying(retryCount, AppConfig.PLAYER_MAX_RETRIES)
        val relaunch = if (retryCount == AppConfig.PLAYER_REFRESH_URL_AT_ATTEMPT) {
            Runnable { restartWithFreshUrl() }
        } else {
            Runnable { start(url) }
        }
        handler.postDelayed(relaunch, AppConfig.PLAYER_RETRY_DELAY_MS)
    }

    private fun restartWithFreshUrl() {
        val resolver = urlResolver
        if (resolver == null) {
            replayCurrentUrl()
            return
        }
        listener?.onResolvingFreshUrl()
        resolver.resolve { fresh ->
            if (fresh == null) {
                Logger.w("Aucune URL fraiche obtenue")
                replayCurrentUrl()
            } else {
                Logger.d("Nouvelle URL obtenue, relance de la lecture")
                currentUrl = fresh
                start(fresh)
            }
        }
    }

    /** Repli quand le serveur n'a pas rendu d'URL utilisable. */
    private fun replayCurrentUrl() {
        val url = currentUrl
        if (url == null || retryCount >= AppConfig.PLAYER_MAX_RETRIES) {
            listener?.onUnrecoverableError()
        } else {
            start(url)
        }
    }

    private fun engineOptions(): ArrayList<String> {
        val options = arrayListOf(
            "--network-caching=$cachingMs",
            "--http-reconnect",
            "--adaptive-logic=rate",
            "--no-sub-autodetect-file",
            "--no-video-title-show",
            "-v"
        )
        if (AppConfig.PLAYER_LOW_LATENCY) {
            // Retard de depart sur un direct. lowlatency ne sert que si la source
            // publie du LL-HLS, livedelay s'applique a toutes les playlists.
            options.add("--adaptive-livedelay=${AppConfig.PLAYER_LIVE_DELAY_MS}")
            options.add("--adaptive-lowlatency=1")
        }
        return options
    }
}

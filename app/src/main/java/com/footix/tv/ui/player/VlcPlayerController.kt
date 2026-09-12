package com.footix.tv.ui.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.footix.tv.core.AppConfig
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
        fun onUnrecoverableError()
    }

    private val libVlc = LibVLC(context.applicationContext, engineOptions())
    private val player = MediaPlayer(libVlc)
    private val handler = Handler(Looper.getMainLooper())

    var listener: Listener? = null

    private var currentUrl: String? = null
    private var retryCount = 0

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
        retryCount = 0
        start(url)
    }

    /** Relance manuelle apres abandon (compteur remis a zero). */
    fun retry() {
        val url = currentUrl ?: return
        handler.removeCallbacksAndMessages(null)
        retryCount = 0
        start(url)
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
        val media = Media(libVlc, Uri.parse(url)).apply {
            setHWDecoderEnabled(true, false)
            addOption(":network-caching=${AppConfig.PLAYER_NETWORK_CACHING_MS}")
            addOption(":live-caching=${AppConfig.PLAYER_NETWORK_CACHING_MS}")
            addOption(":file-caching=${AppConfig.PLAYER_NETWORK_CACHING_MS}")
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
                retryCount = 0
                listener?.onPlaying()
            }

            MediaPlayer.Event.Paused -> listener?.onPaused()

            // Sur un direct, EndReached signifie que la source a coupe.
            MediaPlayer.Event.EncounteredError, MediaPlayer.Event.EndReached -> scheduleRetry()
        }
    }

    private fun scheduleRetry() {
        val url = currentUrl ?: return
        if (retryCount >= AppConfig.PLAYER_MAX_RETRIES) {
            listener?.onUnrecoverableError()
            return
        }
        retryCount++
        listener?.onRetrying(retryCount, AppConfig.PLAYER_MAX_RETRIES)
        handler.postDelayed({ start(url) }, AppConfig.PLAYER_RETRY_DELAY_MS)
    }

    private fun engineOptions(): ArrayList<String> = arrayListOf(
        "--network-caching=${AppConfig.PLAYER_NETWORK_CACHING_MS}",
        "--http-reconnect",
        "--adaptive-logic=rate",
        "--no-sub-autodetect-file",
        "--no-video-title-show",
        "-v"
    )
}

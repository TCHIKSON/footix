package com.footix.tv.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.footix.tv.R
import com.footix.tv.core.AppConfig
import com.footix.tv.databinding.ActivityPlayerBinding
import com.footix.tv.domain.displayTitle
import com.footix.tv.domain.model.Match
import com.footix.tv.domain.model.PlayableStream

class PlayerActivity : FragmentActivity(), VlcPlayerController.Listener {

    private lateinit var binding: ActivityPlayerBinding
    private var controller: VlcPlayerController? = null
    private var streamUrl: String = ""

    private val overlayHandler = Handler(Looper.getMainLooper())
    private val hideOverlayTask = Runnable { binding.overlay.isVisible = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        streamUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (streamUrl.isBlank()) {
            finish()
            return
        }

        binding.title.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.subtitle.text = intent.getStringExtra(EXTRA_SUBTITLE).orEmpty()
        binding.liveBadge.isVisible = intent.getBooleanExtra(EXTRA_LIVE, false)

        controller = VlcPlayerController(this).apply { listener = this@PlayerActivity }
        showOverlay()
    }

    override fun onStart() {
        super.onStart()
        val player = controller ?: return
        player.attach(binding.videoLayout)
        binding.buffering.isVisible = true
        player.play(streamUrl)
    }

    override fun onStop() {
        super.onStop()
        controller?.detach()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayHandler.removeCallbacksAndMessages(null)
        controller?.release()
        controller = null
    }

    // --- Evenements du lecteur --------------------------------------------

    override fun onBuffering(percent: Float) {
        binding.buffering.isVisible = percent < 100f
    }

    override fun onPlaying() {
        binding.buffering.isVisible = false
        binding.errorPanel.isVisible = false
        binding.status.isVisible = false
        scheduleOverlayHide()
    }

    override fun onPaused() {
        showOverlay(autoHide = false)
    }

    override fun onRetrying(attempt: Int, maxAttempts: Int) {
        binding.buffering.isVisible = true
        binding.status.isVisible = true
        binding.status.text = getString(R.string.player_retrying, attempt, maxAttempts)
        showOverlay(autoHide = false)
    }

    override fun onUnrecoverableError() {
        binding.buffering.isVisible = false
        binding.status.isVisible = false
        binding.errorPanel.isVisible = true
        showOverlay(autoHide = false)
    }

    // --- Telecommande ------------------------------------------------------

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val player = controller ?: return super.onKeyDown(keyCode, event)
        showOverlay()

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                onSelectPressed(player)
                true
            }

            KeyEvent.KEYCODE_MEDIA_STOP -> {
                finish()
                true
            }

            // Sans fenetre DVR, un direct n'est pas deplacable : la touche est
            // simplement absorbee.
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                player.seekBy(AppConfig.PLAYER_SEEK_STEP_MS)
                true
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                player.seekBy(-AppConfig.PLAYER_SEEK_STEP_MS)
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun onSelectPressed(player: VlcPlayerController) {
        if (binding.errorPanel.isVisible) {
            binding.errorPanel.isVisible = false
            binding.buffering.isVisible = true
            player.retry()
            return
        }
        player.togglePlayPause()
        if (player.isPlaying) scheduleOverlayHide() else showOverlay(autoHide = false)
    }

    // --- Habillage ---------------------------------------------------------

    private fun showOverlay(autoHide: Boolean = true) {
        binding.overlay.isVisible = true
        overlayHandler.removeCallbacks(hideOverlayTask)
        if (autoHide) scheduleOverlayHide()
    }

    private fun scheduleOverlayHide() {
        overlayHandler.removeCallbacks(hideOverlayTask)
        overlayHandler.postDelayed(hideOverlayTask, AppConfig.PLAYER_OVERLAY_TIMEOUT_MS)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_SUBTITLE = "extra_subtitle"
        private const val EXTRA_LIVE = "extra_live"

        fun intent(context: Context, match: Match, stream: PlayableStream): Intent {
            val subtitle = listOf(match.competition, stream.channel, stream.label)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" · ")

            return Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_URL, stream.url)
                .putExtra(EXTRA_TITLE, match.displayTitle)
                .putExtra(EXTRA_SUBTITLE, subtitle)
                .putExtra(EXTRA_LIVE, match.isLive)
        }
    }
}

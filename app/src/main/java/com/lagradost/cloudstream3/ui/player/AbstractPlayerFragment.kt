package com.lagradost.cloudstream3.ui.player

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.SubtitleView
import androidx.viewbinding.ViewBinding
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.BaseFragment

enum class PlayerResize(@StringRes val nameRes: Int) {
    Fit(R.string.resize_fit),
    Fill(R.string.resize_fill),
    Zoom(R.string.resize_zoom),
}

const val SKIP_OP_VIDEO_PERCENTAGE = 50
const val PRELOAD_NEXT_EPISODE_PERCENTAGE = 80
const val NEXT_WATCH_EPISODE_PERCENTAGE = 90
const val UPDATE_SYNC_PROGRESS_PERCENTAGE = 80

@OptIn(UnstableApi::class)
abstract class AbstractPlayerFragment<T : ViewBinding>(
    bindingCreator: BindingCreator<T>
) : BaseFragment<T>(bindingCreator), PlayerView.Callbacks {

    private var _player: IPlayer = CS3IPlayer()

    protected var playerHostView: PlayerView? = null

    var player: IPlayer
        get() = playerHostView?.player ?: _player
        set(value) {
            _player = value
            playerHostView?.player = value
        }

    val subView: SubtitleView? get() = playerHostView?.subView
    val playerPausePlay: ImageView? get() = playerHostView?.playerPausePlay

    val playerView: androidx.media3.ui.PlayerView?
        get() = playerHostView?.exoPlayerView

    var currentPlayerStatus: CSPlayerLoading
        get() = playerHostView?.currentPlayerStatus ?: CSPlayerLoading.IsBuffering
        set(value) { playerHostView?.currentPlayerStatus = value }

    protected var mMediaSession: MediaSession?
        get() = playerHostView?.mMediaSession
        set(value) { playerHostView?.mMediaSession = value }

    // 👇 স্ক্যানার লঞ্চার 👇
    private val scanLauncher = registerForActivityResult(com.journeyapps.barcodescanner.ScanContract()) { result ->
        val context = activity
        if (context != null && result.contents != null) {
            val currentUrl = try {
                (this as? GeneratorPlayer)?.currentSelectedLink?.first?.url ?: ""
            } catch (e: Exception) {
                ""
            }
            
            if (currentUrl.isNotBlank()) {
                com.lagradost.cloudstream3.utils.RemoteCastHelper.handleScanResult(result.contents, currentUrl, context)
            } else {
                android.widget.Toast.makeText(context, "No active video link found.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun nextEpisode() { throw NotImplementedError() }
    override fun prevEpisode() { throw NotImplementedError() }
    override fun playerPositionChanged(position: Long, duration: Long) { throw NotImplementedError() }
    override fun playerDimensionsLoaded(width: Int, height: Int) { throw NotImplementedError() }
    override fun subtitlesChanged() { throw NotImplementedError() }
    override fun embeddedSubtitlesFetched(subtitles: List<SubtitleData>) { throw NotImplementedError() }
    override fun onTracksInfoChanged() { throw NotImplementedError() }
    override fun exitedPipMode() { throw NotImplementedError() }
    override fun hasNextMirror(): Boolean { throw NotImplementedError() }
    override fun nextMirror() { throw NotImplementedError() }

    override fun playerError(exception: Throwable) {
        playerHostView?.playerError(exception)
    }

    override fun fixLayout(view: View) = Unit

    override fun onBindingCreated(binding: T, savedInstanceState: Bundle?) {
        val ctx = context ?: return
        playerHostView = PlayerView(ctx)
        playerHostView?.player = _player
        playerHostView?.callbacks = this
        playerHostView?.bindViews(binding.root)
        playerHostView?.initialize()

        // 👇 নতুন যোগ করা ভাসমান (Floating) স্ক্যানার বাটন 👇
        try {
            val rootView = binding.root as? android.view.ViewGroup
            if (rootView != null) {
                val castButton = ImageView(ctx).apply {
                    setImageResource(android.R.drawable.ic_menu_share) // শেয়ার বা কাস্টিং আইকন
                    setBackgroundColor(Color.parseColor("#80000000")) // হালকা কালো ব্যাকগ্রাউন্ড
                    setPadding(30, 30, 30, 30)
                    setColorFilter(Color.WHITE)
                    // বাটনে ক্লিক করলে স্ক্যানার ওপেন হবে
                    setOnClickListener {
                        com.lagradost.cloudstream3.utils.RemoteCastHelper.launchScanner(scanLauncher)
                    }
                }
                
                // স্ক্রিনের উপরের ডানদিকে বাটনটি বসানো হচ্ছে
                val params = FrameLayout.LayoutParams(
                    150, 150, // বাটনের সাইজ
                    Gravity.TOP or Gravity.END
                )
                params.setMargins(40, 40, 40, 40) // চারপাশে একটু ফাঁকা জায়গা
                
                rootView.addView(castButton, params)
                castButton.bringToFront()
            }
        } catch (e: Exception) {
            // যদি কোনো কারণে বাটন বসাতে সমস্যা হয়, অ্যাপ ক্র্যাশ করবে না
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        playerHostView?.onPictureInPictureModeChanged(isInPictureInPictureMode, activity)
    }

    override fun onDestroy() {
        playerHostView?.release()
        super.onDestroy()
    }

    override fun onPause() {
        playerHostView?.releaseKeyEventListener()
        super.onPause()
    }

    override fun onStop() {
        playerHostView?.onStop()
        super.onStop()
    }

    override fun onResume() {
        context?.let { ctx ->
            playerHostView?.onResume(ctx)
        }
        super.onResume()
    }

    fun nextResize() {
        playerHostView?.nextResize()
    }

    open fun resize(resize: PlayerResize, showToast: Boolean) {
        playerHostView?.resize(resize, showToast)
    }
}

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

    // 👇 Null চেক ঠিক করা হয়েছে 👇
    private val scanLauncher = registerForActivityResult(com.journeyapps.barcodescanner.ScanContract()) { result ->
        val ctx = activity ?: return@registerForActivityResult
        if (result.contents != null) {
            val currentUrl = try {
                (this as? GeneratorPlayer)?.currentSelectedLink?.first?.url ?: ""
            } catch (e: Exception) {
                ""
            }
            
            if (currentUrl.isNotBlank()) {
                com.lagradost.cloudstream3.utils.RemoteCastHelper.handleScanResult(result.contents, currentUrl, null, ctx)
            } else {
                android.widget.Toast.makeText(ctx, "No active video link found.", android.widget.Toast.LENGTH_SHORT).show()
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

        // 👇 নতুন যোগ করা স্মার্ট (Floating) স্ক্যানার বাটন 👇
        try {
            val rootView = binding.root as? android.view.ViewGroup
            if (rootView != null) {
                val castButton = ImageView(ctx).apply {
                    setImageResource(android.R.drawable.ic_menu_share)
                    setBackgroundColor(Color.parseColor("#80000000"))
                    setPadding(40, 40, 40, 40)
                    setColorFilter(Color.WHITE)
                    visibility = View.GONE // শুরুতে লুকানো থাকবে
                    setOnClickListener {
                        com.lagradost.cloudstream3.utils.RemoteCastHelper.launchScanner(scanLauncher)
                    }
                }
                
                val params = FrameLayout.LayoutParams(
                    150, 150, 
                    Gravity.TOP or Gravity.END
                )
                params.setMargins(40, 40, 40, 40)
                
                rootView.addView(castButton, params)
                castButton.bringToFront()

                // 👇 প্লেয়ারের কন্ট্রোলের সাথে বাটনটি সিঙ্ক করা হলো 👇
                playerView?.setControllerVisibilityListener { visibility ->
                    if (visibility == View.VISIBLE) {
                        castButton.visibility = View.VISIBLE
                        castButton.bringToFront() // প্লেয়ারের কন্ট্রোলের সামনে নিয়ে আসা
                    } else {
                        castButton.visibility = View.GONE
                    }
                }
                // 👆 নতুন কোড শেষ 👆
            }
        } catch (e: Exception) {
            // কোনো এরর হলে অ্যাপ ক্র্যাশ করবে না
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

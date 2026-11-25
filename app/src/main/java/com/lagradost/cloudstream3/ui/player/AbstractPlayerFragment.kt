package com.lagradost.cloudstream3.ui.player

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
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

    // 👇 নতুন যোগ করা স্ক্যানার লঞ্চার (আসল লিংকসহ) 👇
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
                android.widget.Toast.makeText(context, "No active video link found. Are you playing a stream?", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    // 👆 নতুন কোড শেষ 👆

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

        // 👇 নতুন যোগ করা টাচ ডিটেক্টর 👇
        val gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                // ভিডিও প্লেয়ারের স্পিড বাড়ানোর কাজটি ব্লক না করে শুধু আমাদের স্ক্যানারটি ওপেন করবে
                com.lagradost.cloudstream3.utils.RemoteCastHelper.launchScanner(scanLauncher)
            }
        })

        // পুরো প্লেয়ার স্ক্রিনে টাচ ডিটেক্টরটি সেট করা হলো
        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // false ফেরত দেওয়ায় প্লেয়ারের আগের সব ফিচার (প্লে/পজ, 2x স্পিড) ঠিক থাকবে
        }
        // 👆 নতুন কোড শেষ 👆
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

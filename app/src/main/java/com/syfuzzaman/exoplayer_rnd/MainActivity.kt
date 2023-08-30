package com.syfuzzaman.exoplayer_rnd

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.DefaultTimeBar
import com.syfuzzaman.exoplayer_rnd.databinding.ActivityMainBinding
import okhttp3.OkHttpClient

@UnstableApi
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var exoPlayer: ExoPlayer? = null
    private var httpDataSourceFactory: OkHttpDataSource.Factory? = null
    private var mediaItemIndex = 0
    private val playbackStateListener: Player.Listener = playbackStateListener()
    private val playingChangeListener: Player.Listener = playingChangeListener()

    private lateinit var forwardBtn: ImageButton
    private lateinit var rewindBtn: ImageButton
    private lateinit var timeBar: DefaultTimeBar
    private lateinit var position: TextView
    private lateinit var duration: TextView
    private lateinit var imageViewFullScreen: ImageButton

    private var isFullscreen = false
    private var playbackPosition = 0L
    private var playWhenReady = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setFindViewById()
        preparePlayer()
        Log.d("EXOPLAYER___", "onCreate Called")
    }

    private fun preparePlayer() {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val modifiedRequest = originalRequest.newBuilder()
                    .header("Cookie",  "user_id=45343; Path=/; Domain=iptv-isp.nexdecade.com/; Expires=Wed 30 Aug 2023 06:14:20 GMT; SameSite=None; Secure; expire=1693383318; Path=/; Domain=iptv-isp.nexdecade.com/; Expires=Wed 30 Aug 2023 06:14:20 GMT;  SameSite=None; Secure;asset_hash=6be8b97caf81c6b4afdbf40fcf2865c7; Path=/; Domain=iptv-isp.nexdecade.com/; Expires=Wed 30 Aug 2023 06:14:20 GMT;  SameSite=None; Secure;")
                    .build()
                chain.proceed(modifiedRequest)
            }
            .build()

        // Create a HttpDataSourceFactory with the custom OkHttpClient
        httpDataSourceFactory = OkHttpDataSource.Factory(client)

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory!!)

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build()

        exoPlayer?.playWhenReady = playWhenReady
        binding.playerView.player = exoPlayer
        setFullScreen()

        val mediaItem = MediaItem.Builder()
            .setUri(URL)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        // Set the media item to be played.
        exoPlayer?.setMediaItem(mediaItem)

        exoPlayer?.apply {
            addListener(playbackStateListener)
            addListener(playingChangeListener)
            seekTo(playbackPosition)
            prepare()
            play()
        }

        exoPlayer?.addAnalyticsListener(
            object : AnalyticsListener {
                override fun onPlaybackStateChanged(
                    eventTime: AnalyticsListener.EventTime, @Player.State state: Int
                ) {
                    Log.d("EXOPLAYER___ Analytics", eventTime.toString())
                }

                override fun onDroppedVideoFrames(
                    eventTime: AnalyticsListener.EventTime,
                    droppedFrames: Int,
                    elapsedMs: Long,
                ) {
                }
            }
        )

        exoPlayer!!
            .createMessage { messageType: Int, payload: Any? ->
//                Toast.makeText(this, "Are you enjoying?", Toast.LENGTH_SHORT).show()
            }
            .setLooper(Looper.getMainLooper())
            .setPosition(mediaItemIndex, playbackPosition)
            .setPayload(mediaItem)
            .setDeleteAfterDelivery(false)
            .send()

        exoPlayer!!.addAnalyticsListener(
            PlaybackStatsListener(/* keepHistory= */ true) { eventTime, playbackStats ->
                // Analytics data for the session started at `eventTime` is ready.
                Log.d(
                    "EXOPLAYER___ Playback Stat",
                    "Playback summary: " +
                            "play time = ${playbackStats.totalPlayTimeMs}, " +
                            "rebuffers = ${playbackStats.totalRebufferCount}, " +
                            "totalPaused time = ${playbackStats.totalPausedTimeMs}"
                )
                Log.d(
                    "EXOPLAYER___ Playback Stat",
                    "Additional calculated summary metrics: " +
                            "average video bitrate = ${playbackStats.meanVideoFormatBitrate}, " +
                            "mean time between rebuffers = ${playbackStats.meanTimeBetweenRebuffers}"
                )
            }
        )
    }

    private fun playingChangeListener() = object : Player.Listener {
        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            @Player.MediaItemTransitionReason reason: Int,
        ) {
            Log.d("EXOPLAYER___", "NEXT")
        }
    }

    // Inside playbackStateListener
    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                ExoPlayer.STATE_BUFFERING -> binding.playerView.useController = false
                ExoPlayer.STATE_READY -> {
                    binding.playerView.useController = true

                    // Update playWhenReady based on the player's state
                    playWhenReady = exoPlayer?.playWhenReady ?: true
                }
            }
        }
    }


    private fun releasePlayer() {
        exoPlayer?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            mediaItemIndex = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        exoPlayer = null
    }

    private fun setFullScreen() {
        imageViewFullScreen.visibility = View.VISIBLE
        val orientation = this.resources.configuration.orientation

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isFullscreen = true
            imageViewFullScreen.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.ic_fullscreen_exit
                )
            )
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            isFullscreen = false
            imageViewFullScreen.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.ic_fullscreen
                )
            )
        }

        imageViewFullScreen.setOnClickListener {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }

    private fun setFindViewById() {
        // Restore playback position and playWhenReady state if transitioning from fullscreen to normal mode
        if (!isFullscreen) {
            playbackPosition = exoPlayer?.currentPosition ?: 0L
            playWhenReady = exoPlayer?.playWhenReady ?: true
        }

        forwardBtn = binding.root.findViewById(R.id.exo_ffwd)
        rewindBtn = binding.root.findViewById(R.id.exo_rew)
        timeBar = binding.root.findViewById(R.id.exo_progress)
        duration = binding.root.findViewById(androidx.media3.ui.R.id.exo_duration)
        position = binding.root.findViewById(androidx.media3.ui.R.id.exo_position)
        imageViewFullScreen =
            binding.root.findViewById(androidx.media3.ui.R.id.exo_minimal_fullscreen)
    }

    private fun hideSystemUi() {
        val fullscreenFlags = (View.SYSTEM_UI_FLAG_LOW_PROFILE or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        if (isPlayerFullscreen()) {
            binding.playerView.systemUiVisibility = fullscreenFlags
        } else {
            binding.playerView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE

            // Restore playback position and playWhenReady state if transitioning from fullscreen to normal mode
            if (isFullscreen) {
                exoPlayer?.seekTo(playbackPosition)
                exoPlayer?.playWhenReady = playWhenReady
            }
        }
    }

    private fun isPlayerFullscreen(): Boolean {
        val orientation = this.resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE
    }


    companion object {
        private const val LIVE_URL = "YOUR_LIVE_STREAM_URL"
        private const val URL = "https://iptv-isp.nexdecade.com/vod/Going%20the%20Distance%20(2010)%20720p/Going.the.Distance.2010.720p.BrRip.x264.YIFY.mp4/playlist.m3u8"
        private const val FIRST_URL = "YOUR_FIRST_MEDIA_URL"
        private const val SECOND_URL = "YOUR_SECOND_MEDIA_URL"
    }



    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            preparePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT < 24) {
            preparePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            // Pause playback and release the player
            playWhenReady = exoPlayer?.playWhenReady ?: true
            exoPlayer?.removeMediaItems(0, exoPlayer!!.mediaItemCount)
            exoPlayer?.clearMediaItems()
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            // Pause playback and release the player
            playWhenReady = exoPlayer?.playWhenReady ?: true
            exoPlayer?.removeMediaItems(0, exoPlayer!!.mediaItemCount)
            exoPlayer?.clearMediaItems()
            releasePlayer()
        }
    }

}

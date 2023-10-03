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
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.DefaultTimeBar
import com.syfuzzaman.exoplayer_rnd.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import java.io.File

@UnstableApi
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var exoPlayer: ExoPlayer? = null
    private var httpDataSourceFactory: OkHttpDataSource.Factory? = null

    private lateinit var forwardBtn: ImageButton
    private lateinit var rewindBtn: ImageButton
    private lateinit var timeBar: DefaultTimeBar
    private lateinit var position: TextView
    private lateinit var duration: TextView
    private lateinit var imageViewFullScreen: ImageButton

    private var isFullscreen = false
    private var playbackPosition = 0L
    private var playWhenReady = true

    private lateinit var simpleCache: SimpleCache


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // player media caching
        val evict = LeastRecentlyUsedCacheEvictor((100 * 1024 * 1024).toLong())
        val databaseProvider: DatabaseProvider = StandaloneDatabaseProvider(this)
        simpleCache = SimpleCache(File(this.cacheDir, "media"), evict, databaseProvider)

        setFindViewById()
        preparePlayer()
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

        httpDataSourceFactory = OkHttpDataSource.Factory(client)
        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory!!)

        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        val mediaItem = MediaItem.Builder()
            .setUri(URL)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { exoPlayer ->
                binding.playerView.player = exoPlayer
                val httpDataSourceFactory =
                    DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
                val defaultDataSourceFactory =
                    DefaultDataSourceFactory(this, httpDataSourceFactory)
                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(simpleCache)
                    .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                val mediaSource = ProgressiveMediaSource
                    .Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem)
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.prepare()
            }
    }

    private fun release(){
        binding.playerView.player?.release()
        exoPlayer?.release()
        simpleCache.release()
    }

    private fun pause() {
        binding.playerView.player?.pause()
    }

    private fun resume() {
        binding.playerView.player?.play()
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
        private const val FIRST_URL = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
        private const val SECOND_URL = "YOUR_SECOND_MEDIA_URL"
    }

    override fun onPause() {
        super.onPause()
        pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

}

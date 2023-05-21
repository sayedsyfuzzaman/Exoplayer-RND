package com.syfuzzaman.exoplayer_rnd

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.syfuzzaman.exoplayer_rnd.databinding.ActivityMainBinding


@UnstableApi
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var exoPlayer: ExoPlayer? = null

    private var playbackPosition = 0L
    private var playWhenReady = true
    private val playbackStateListener: Player.Listener = playbackStateListener()
    private val playingChangeListener: Player.Listener = playingChangeListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        preparePlayer()
    }

    private fun preparePlayer(){
        exoPlayer = ExoPlayer.Builder(this)
            .build()

        exoPlayer?.playWhenReady = true
        binding.playerView.player = exoPlayer

        // Build the media item with specific media extension
        val mediaItem = MediaItem.Builder()
            .setUri(URL)
            .setMimeType(MimeTypes.APPLICATION_MP4)
            .build()

//        mediaItem = MediaItem.fromUri(URL)

        // Set the media item to be played.
        exoPlayer?.setMediaItem(mediaItem)

        // Build the media items.
        val firstItem = MediaItem.fromUri(FIRST_URL)
        val secondItem = MediaItem.fromUri(SECOND_URL)

        // Add the media items to be played.
        exoPlayer?.addMediaItem(firstItem)
        exoPlayer?.addMediaItem(secondItem)

//        // Prepare the player.
//        exoPlayer?.prepare()
//
//        // Start the playback.
//        exoPlayer?.play()
//

        exoPlayer?.apply {
            addListener(playbackStateListener)
            addListener(playingChangeListener)
            prepare()
            play()
        }
    }

    private fun playingChangeListener() = object : Player.Listener{
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
//                        Log.d("EXOPLAYER___", "Playing: SCREEN ON")
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                // Not playing because playback is paused, ended, suppressed, or the player
                // is buffering, stopped or failed. Check player.playWhenReady,
                // player.playbackState, player.playbackSuppressionReason and
                // player.playerError for details.
//                        Log.d("EXOPLAYER___", "Playing OFF: SCREEN ON DISABLED")
                getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            }
        }
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.d("EXOPLAYER___", "changed state to $stateString")
        }
    }

    private fun releasePlayer(){
        exoPlayer.let { player ->
            playbackPosition = player!!.currentPosition
            playWhenReady = player.playWhenReady
            player.release()
            exoPlayer = null
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }


    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    companion object{
        const val URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        const val FIRST_URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        const val SECOND_URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
    }
}
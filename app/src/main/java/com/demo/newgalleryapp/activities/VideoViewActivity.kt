package com.demo.newgalleryapp.activities

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.demo.newgalleryapp.R

class VideoViewActivity : AppCompatActivity() {
    private lateinit var videoViewForSlider: PlayerView
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_view)

        videoViewForSlider = findViewById(R.id.videoView)

        val videoPath = intent.getStringExtra("path")

        val uri = Uri.parse(videoPath)
        player = ExoPlayer.Builder(this).build()
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        // Prepare the player
        player.prepare()
        // Attach the player to the player view
        videoViewForSlider.player = player
        // Start playback if needed
        player.play()
    }

    override fun onStart() {
        super.onStart()
        player.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
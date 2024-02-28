package com.demo.newgalleryapp.activities

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.classes.StyledPlayerViewLatest
import com.demo.newgalleryapp.databinding.ActivityVideoViewBinding
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.utilities.CommonFunctions.setNavigationColor
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer

class VideoViewActivity : AppCompatActivity(), StyledPlayerViewLatest.ControllerVisibilityListener {

    private lateinit var binding: ActivityVideoViewBinding
    private var exoPlayer: SimpleExoPlayer? = null
    private var myPosition = 0
    private var mList = ArrayList<MediaModel>()
    private lateinit var scaleDetector: ScaleDetector
    private var ok = 0
    private var popupWindow: PopupWindow? = null
    private var isFromNewIntent = false
    private var isPlayingInPiP = false

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (!isInPictureInPictureMode) {
            binding.playerView.player?.pause()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }


        setNavigationColor(window, Color.BLACK)

        binding = ActivityVideoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)


        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        myPosition = intent.getIntExtra("currentVideoPosition", 0)

        if (intent.hasExtra("isFromFolderList")) {
            // Retrieve the model object from the intent extras
            val model = intent.getSerializableExtra("isFromFolderList") as MediaModel
            val tempList = mutableListOf<MediaModel>()
            tempList.add(model)
            mList.addAll(tempList)

            initPlayer()
            prepare(mList[0].path)

        } else {

            val modelList: List<MediaModel> = (application as AppClass).mainViewModel.tempVideoList
            mList.addAll(modelList)

            initPlayer()
            prepare(mList[myPosition].path)
        }

        scaleDetector = ScaleDetector(binding)
        val scaleGestureDetector = ScaleGestureDetector(this, scaleDetector)


        binding.imgBack.setOnClickListener {
            finish()
        }


        binding.playerView.controllerHideOnTouch = true
        binding.playerView.controllerAutoShow = true
        binding.playerView.controllerShowTimeoutMs = Long.MAX_VALUE.toInt()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)


        val videoBack = binding.playerView.findViewById<ImageView>(R.id.imgPre)
        val videoNext = binding.playerView.findViewById<ImageView>(R.id.imgNext)
        val playerControlFitCropView = binding.playerView.findViewById<ImageView>(R.id.imgCrop)
        val exoLock = binding.playerView.findViewById<ImageView>(R.id.exo_lock)
        val exoVolume = binding.playerView.findViewById<ImageView>(R.id.exo_volume)
        val imgRotate = binding.playerView.findViewById<ImageView>(R.id.imgRotate)


        binding.imgController.setOnClickListener {
            showPopup(binding.root)
        }

        playerControlFitCropView.setOnClickListener {

            when (ok) {
                0 -> {
                    scaleDetector.setScaleFactor(-0.5f)
                    ok = 1
                    return@setOnClickListener
                }

                1 -> {
                    scaleDetector.setScaleFactor(1.0f)
                    ok = 2
                    return@setOnClickListener
                }

                2 -> {
                    scaleDetector.setScaleFactor(2.5f)
                    ok = 3
                    return@setOnClickListener
                }

                3 -> {
                    scaleDetector.setScaleFactor(4.2f)
                    ok = 0
                    return@setOnClickListener
                }
            }
        }

        videoNext.setOnClickListener {
            nextVideo()
        }

        videoBack.setOnClickListener {
            previousVideo()
        }

        imgRotate.setOnClickListener {

            requestedOrientation =
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }

            adjustLayoutForFullScreen()
        }

        exoLock.setOnClickListener {
            binding.playerView.useController = false
            binding.playerView.hideController()
            binding.playerView.isClickable = false
            binding.imgLock.visibility = View.VISIBLE
        }

        exoVolume.setOnClickListener {
            if (isMediaStreamMuted()) {
                exoVolume.setImageResource(R.drawable.ic_volume_icon)
                setStreamMusicMute(false)
            } else {
                exoVolume.setImageResource(R.drawable.ic_mute)
                setStreamMusicMute(true)
            }
        }

        binding.imgLock.setOnClickListener {
            binding.playerView.useController = true
            binding.playerView.showController()
            binding.playerView.isClickable = true
            binding.imgLock.visibility = View.GONE
        }
        register()
    }

    private fun hideSystemUI() {
        window.decorView.apply {
            // Hide both the navigation bar and the status bar.
            systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    private fun adjustLayoutForFullScreen() {
        // Update your video player layout to match the new screen dimensions
        val layoutParams = binding.playerView.layoutParams as RelativeLayout.LayoutParams
        layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
        layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT
        binding.playerView.layoutParams = layoutParams
    }


    private fun showPopup(anchorView: View) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupWindowRestoreOne: View = inflater.inflate(R.layout.popup_video, null)

        popupWindow = PopupWindow(
            popupWindowRestoreOne,
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.MATCH_PARENT,
            true
        )

        popupWindow?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val selectedColumns = (application as AppClass).mainViewModel.sharedPreferencesHelper.getDefaultControlLer()
        var tempColumnVideos = selectedColumns

        val text025 = popupWindowRestoreOne.findViewById<TextView>(R.id.id1)
        val text05 = popupWindowRestoreOne.findViewById<TextView>(R.id.id2)
        val text075 = popupWindowRestoreOne.findViewById<TextView>(R.id.id3)
        val normal = popupWindowRestoreOne.findViewById<TextView>(R.id.id4)


        val imageView025 = popupWindowRestoreOne.findViewById<ImageView>(R.id.zeroTwo_image_view)
        val imageView05 = popupWindowRestoreOne.findViewById<ImageView>(R.id.zeroFive_image_view)
        val imageView075 = popupWindowRestoreOne.findViewById<ImageView>(R.id.zeroSeven_image_view)
        val imageViewNormal = popupWindowRestoreOne.findViewById<ImageView>(R.id.normal_image_view)


        when (selectedColumns) {
            "normal" -> {
                imageViewNormal.visibility = View.VISIBLE
            }

            "text025" -> {
                imageView025.visibility = View.VISIBLE
            }

            "text05" -> {
                imageView05.visibility = View.VISIBLE
            }

            "text075" -> {
                imageView075.visibility = View.VISIBLE
            }
        }

        text025.setOnClickListener {

            imageView05.visibility = View.GONE
            imageView075.visibility = View.GONE
            imageViewNormal.visibility = View.GONE
            imageView025.visibility = View.VISIBLE

            tempColumnVideos = "text025"
            (application as AppClass).mainViewModel.sharedPreferencesHelper.saveDefaultControlLer(
                tempColumnVideos!!
            )

            exoPlayer?.setPlaybackSpeed(0.25f)
            popupWindow?.dismiss()
        }

        text05.setOnClickListener {

            imageView075.visibility = View.GONE
            imageViewNormal.visibility = View.GONE
            imageView025.visibility = View.GONE
            imageView05.visibility = View.VISIBLE

            tempColumnVideos =  "text05"
            (application as AppClass).mainViewModel.sharedPreferencesHelper.saveDefaultControlLer(
                tempColumnVideos!!
            )
            exoPlayer?.setPlaybackSpeed(0.5f)
            popupWindow?.dismiss()
        }

        text075.setOnClickListener {

            imageViewNormal.visibility = View.GONE
            imageView025.visibility = View.GONE
            imageView05.visibility = View.GONE
            imageView075.visibility = View.VISIBLE

            tempColumnVideos = "text075"
            (application as AppClass).mainViewModel.sharedPreferencesHelper.saveDefaultControlLer(
                tempColumnVideos!!
            )
            exoPlayer?.setPlaybackSpeed(0.75f)
            popupWindow?.dismiss()
        }

        normal.setOnClickListener {

            imageView025.visibility = View.GONE
            imageView05.visibility = View.GONE
            imageView075.visibility = View.GONE
            imageViewNormal.visibility = View.VISIBLE

            tempColumnVideos = "normal"
            (application as AppClass).mainViewModel.sharedPreferencesHelper.saveDefaultControlLer(
                tempColumnVideos!!
            )
            exoPlayer?.setPlaybackSpeed(1.0f)
            popupWindow?.dismiss()
        }

    }


    class ob1(var context: Context, private var binding: ActivityVideoViewBinding) :
        ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            var audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            Log.d("TAG", "onChange: $currentVolume")
            val exo_volume = binding.playerView.findViewById<ImageView>(R.id.exo_volume)
            if (currentVolume == 0) {
                exo_volume.setImageResource(R.drawable.ic_mute)
            } else {
                exo_volume.setImageResource(R.drawable.ic_volume_icon)
            }
        }
    }

    private lateinit var myObserver: ob1
    private fun register() {
        myObserver = ob1(this, binding)
        val resolver = contentResolver
        resolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, myObserver
        )
    }

    open class ScaleDetector(private var bin: ActivityVideoViewBinding) :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var scale_factor = 1.0f
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val customLayout: View = bin.playerView.findViewById(R.id.zoom_layout)
            scale_factor *= detector.scaleFactor
            scale_factor = Math.max(0.5f, Math.min(scale_factor, 6.0f))

            customLayout.scaleX = scale_factor
            customLayout.scaleY = scale_factor
            val percentage = (scale_factor * 100).toInt()


//            scale_factor *= detector.scaleFactor
//            scale_factor = scale_factor.coerceIn(0.5f, 6.0f)
//            updateScale()
//            val customLayout: View = bin.playerView.findViewById(R.id.zoom_layout)
//            customLayout.scaleX = scale_factor
//            customLayout.scaleY = scale_factor
//            val percentage = (scale_factor * 100).toInt()
            return true
        }

        fun setScaleFactor(newScaleFactor: Float) {
            scale_factor = newScaleFactor
            updateScale()
        }


        private fun updateScale() {
            val customLayout: View = bin.playerView.findViewById(R.id.zoom_layout)
            customLayout.scaleX = scale_factor
            customLayout.scaleY = scale_factor
            val percentage = (scale_factor * 100).toInt()
            // Update UI or perform additional actions based on the scale factor
        }

    }

    private fun setStreamMusicMute(isMute: Boolean) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, isMute)
    }

    private fun isMediaStreamMuted(): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        } else {
            false
        }
    }

    private fun initPlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        binding.playerView.setControllerVisibilityListener(this)
        exoPlayer?.playWhenReady = true
        binding.playerView.showController()

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        Log.e("TAG", "onPlaybackStateChanged: STATE_ENDED")
                        nextVideo()
                    }

                    Player.STATE_BUFFERING -> {
                        //Log.e(tag, "onPlaybackStateChanged: STATE_BUFFERING")
                    }

                    Player.STATE_IDLE -> {
                        // Log.e(tag, "onPlaybackStateChanged: STATE_IDLE")
                    }

                    Player.STATE_READY -> {

                        // Log.e(tag, "onPlaybackStateChanged: STATE_READY")
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)

                if (isPlaying) {
                    binding.playerView.controllerShowTimeoutMs = 2500
                } else {
                    binding.playerView.controllerShowTimeoutMs = -1
                }

            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("onPlayerError", "onPlayerError: message ${error.message}")
                // Handle playback errors here (e.g., log or display an error message)
            }
        })

    }

    private fun prepare(path: String) {

        if (exoPlayer == null) {
            exoPlayer = SimpleExoPlayer.Builder(this).build()
            binding.playerView.player = exoPlayer
            exoPlayer?.playWhenReady = true
        }

        val mediaItem = MediaItem.fromUri(path)

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            //seekTo(playbackPosition)
            playWhenReady = playWhenReady
            prepare()
        }
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        isFromNewIntent = true
        mList.clear()
        prepare(mList[0].path)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        showMenu()
        binding.playerView.useController = true
        binding.playerView.showController()
        exoPlayer?.playWhenReady = true
        exoPlayer?.play()
    }

    private fun showMenu() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        binding.relTop.visibility = View.VISIBLE
    }

    private fun hideMenu() {
        val window: Window = window
        // window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LOW_PROFILE or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE
        binding.relTop.visibility = View.GONE
        popupWindow?.dismiss()
    }

    override fun onResume() {
        super.onResume()
        if (!isPlayingInPiP) {
            exoPlayer?.playWhenReady = true
        } else {
            if (isFromNewIntent) {
                isFromNewIntent = false
                exoPlayer?.playWhenReady = true
                exoPlayer?.play()
            }
        }
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            releasePlayer()
        } catch (e: Exception) {

        }

    }

    override fun onPause() {
        super.onPause()
        Log.e("TAG", "onStop: onPause")
        if (!isPlayingInPiP) {
            exoPlayer?.playWhenReady = false
        }
    }

    private fun releasePlayer() {
        exoPlayer?.let { player ->
            player.release()
            exoPlayer = null
        }
    }

    private fun nextVideo() {
        if (mList.size == 1) {
            myPosition = 0
        } else {
            if (myPosition < mList.size - 1) {
                myPosition++
            } else {
                myPosition = 0
            }
        }
        if (intent.hasExtra("isFromFolderList")) {
            prepare(mList[0].path)
        } else {
            prepare(mList[myPosition].path)
        }
    }

    private fun previousVideo() {
        if (mList.size == 1) {
            myPosition = 0
        } else {
            if (myPosition > 0) {
                myPosition--
                //onStop()
            } else {
                myPosition = 0
            }
        }
        if (intent.hasExtra("isFromFolderList")) {
            prepare(mList[0].path)
        } else {
            prepare(mList[myPosition].path)
        }
    }

    override fun onVisibilityChanged(visibility: Int) {
        if (visibility === View.VISIBLE) {
            showMenu()
        } else {
            hideMenu()
        }
    }
}
package com.demo.newgalleryapp.activities

import android.app.WallpaperManager
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.utilities.CommonFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream


class WallpaperActivity : AppCompatActivity() {

    private lateinit var wallpaperImageView: ImageView
    private lateinit var wallpaperText: CardView
    private lateinit var progressBar: ProgressBar
    private lateinit var selectedImageUri: Uri
    private var popupWindow: PopupWindow? = null
    private lateinit var backBtn: ImageView
    private var scopeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper)

        wallpaperImageView = findViewById(R.id.wallpaperImageView)
        wallpaperText = findViewById(R.id.wallpaper_card_view_text)
        progressBar = findViewById(R.id.wallpaper_progress_bar)
        backBtn = findViewById(R.id.back_btn_wallpaper)

        backBtn.setOnClickListener {
            finish()
        }

        val getWallpaperUri = intent.getStringExtra("wallpaperUri")

        selectedImageUri = Uri.fromFile(File(getWallpaperUri!!))

        Glide.with(this).load(getWallpaperUri).into(wallpaperImageView)

        wallpaperText.setOnClickListener {
            showPopupForWallpaperItems(wallpaperImageView)
            wallpaperText.visibility = View.GONE
        }

//
//        bottomNavigationView.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.action_main_screen -> setWallpaper(
//                    selectedImageUri, WallpaperManager.FLAG_SYSTEM
//                )
//
//                R.id.action_lock_screen -> setWallpaper(
//                    selectedImageUri, WallpaperManager.FLAG_LOCK
//                )
//
//                R.id.action_both_screens -> setWallpaper(
//                    selectedImageUri, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
//                )
//            }
//            true
//        }
    }

    private fun setWallpaper(imageUri: Uri, whichWallpaper: Int) {

        scopeJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(this@WallpaperActivity)

                val bitmap = loadBitmapFromUri(contentResolver, imageUri)
                wallpaperManager.setStream(
                    contentResolver.openInputStream(imageUri), null, true, whichWallpaper
                )
//                wallpaperManager.setBitmap(MediaStore.Images.Media.getBitmap(contentResolver, imageUri), null, true, whichWallpaper)

                withContext(Dispatchers.Main) {
                    CommonFunctions.showToast(this@WallpaperActivity, "Wallpaper set successfully!")
                    progressBar.visibility = View.GONE
                    wallpaperText.visibility = View.VISIBLE
                }

                finish()
            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    CommonFunctions.showToast(this@WallpaperActivity, "Wallpaper failed!")
                }
                Log.e("error", "setWallpaper: ${e.message}")
            }
        }

    }

    // Function to load bitmap from URI
    private fun loadBitmapFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        return try {
            inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            inputStream?.close()
        }
    }

    private fun showPopupForWallpaperItems(anchorView: View) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupWindowWallpaper: View = inflater.inflate(R.layout.wallpaper_popup_menu, null)

        popupWindow = PopupWindow(
            popupWindowWallpaper,
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.MATCH_PARENT,
            true
        )

        popupWindow?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val setAsLockScreen =
            popupWindowWallpaper.findViewById<TextView>(R.id.wallpaper_lock_screen_text)
        val setAsHomeScreen =
            popupWindowWallpaper.findViewById<TextView>(R.id.wallpaper_home_screen_text)
        val setAsBoth =
            popupWindowWallpaper.findViewById<TextView>(R.id.wallpaper_set_both_screen_text)
        val popupItem = popupWindowWallpaper.findViewById<LinearLayout>(R.id.popupItem_wallpaper)

        setAsLockScreen.setOnClickListener {
            scopeJob?.cancel()
            progressBar.visibility = View.VISIBLE
            setWallpaper(selectedImageUri, WallpaperManager.FLAG_LOCK)
            popupWindow?.dismiss()
        }

        setAsHomeScreen.setOnClickListener {
            scopeJob?.cancel()
            progressBar.visibility = View.VISIBLE
            setWallpaper(selectedImageUri, WallpaperManager.FLAG_SYSTEM)
            popupWindow?.dismiss()
        }

        setAsBoth.setOnClickListener {
            scopeJob?.cancel()
            progressBar.visibility = View.VISIBLE
            setWallpaper(
                selectedImageUri, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            )
            popupWindow?.dismiss()
        }

        popupItem.setOnClickListener {
            popupWindow?.dismiss()
            wallpaperText.visibility = View.VISIBLE
        }
        // Set dismiss listener to nullify the reference
        popupWindow?.setOnDismissListener {
            popupWindow = null
        }

    }
}

package com.demo.newgalleryapp.activities

import android.app.WallpaperManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.demo.newgalleryapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.util.concurrent.Executors


class WallpaperActivity : AppCompatActivity() {

    private lateinit var wallpaperImageView: ImageView
    private lateinit var selectedImageUri: Uri


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper)

        wallpaperImageView = findViewById(R.id.wallpaperImageView)

        val getWallpaperUri = intent.getStringExtra("wallpaperUri")

        selectedImageUri = Uri.fromFile(File(getWallpaperUri!!))


        Glide.with(this).load(getWallpaperUri).into(wallpaperImageView)
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

        Executors.newSingleThreadExecutor().execute {
            try {
                val wallpaperManager = WallpaperManager.getInstance(this)
                wallpaperManager.setBitmap(
                    MediaStore.Images.Media.getBitmap(contentResolver, imageUri),
                    null,
                    true,
                    whichWallpaper
                )
                runOnUiThread {
                    Toast.makeText(this, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Wallpaper failed!", Toast.LENGTH_SHORT).show()
                }
                Log.d("error", "setWallpaper: ${e.message}")
            }
        }


    }
}

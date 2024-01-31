package com.demo.newgalleryapp

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.viewmodel.MainViewModel
import java.io.File
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AppClass : Application() {
    lateinit var mainViewModel: MainViewModel
    private val timer: Timer = Timer()
    override fun onCreate() {

        super.onCreate()
        if (permissionCheck()) {
            mainViewModel = MainViewModel(this)

            Executors.newSingleThreadExecutor().execute {
                mainViewModel.allData
                mainViewModel.allMediaList
                mainViewModel.photosData
                mainViewModel.videosData
                mainViewModel.allTrashData
            }
        }
        Executors.newSingleThreadExecutor().execute {
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    val timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)

                    val imagesToDelete =
                        ImagesDatabase.getDatabase(applicationContext).favoriteImageDao()
                            .selectImages(timestamp)

                    imagesToDelete.forEach {
                        File(it.destinationImagePath).deleteRecursively()
                        ImagesDatabase.getDatabase(applicationContext).favoriteImageDao()
                            .deleteImages(it)
                    }
                }

            }, 100 * 30, 10000 * 30)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        timer.cancel()
    }


    private fun permissionCheck(): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val readMediaImagesPermission = Manifest.permission.READ_MEDIA_IMAGES
            val readMediaVideoPermission = Manifest.permission.READ_MEDIA_VIDEO

            (ActivityCompat.checkSelfPermission(
                this, readMediaImagesPermission
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, readMediaVideoPermission
            ) == PackageManager.PERMISSION_GRANTED)
        } else {
            val readMediaPermission = Manifest.permission.READ_EXTERNAL_STORAGE
            val writeMediaPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE

            (ActivityCompat.checkSelfPermission(
                this, readMediaPermission
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, writeMediaPermission
            ) == PackageManager.PERMISSION_GRANTED)
        }
    }
}
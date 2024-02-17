package com.demo.newgalleryapp.classes

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.utilities.CommonFunctions.isMainSelection
import com.demo.newgalleryapp.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AppClass : Application() {
    lateinit var mainViewModel: MainViewModel
    private val timer: Timer = Timer()
    private var job: Job? = null
    override fun onCreate() {


        super.onCreate()
        if (permissionCheck()) {
            mainViewModel = MainViewModel(this)


            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
            //videoContentObserverExample = VideoContentObserverExample(this)
            // videoContentObserverExample.registerVideoContentObserver()

            Executors.newSingleThreadExecutor().execute {
                mainViewModel.allData
                mainViewModel.allMediaList
                mainViewModel.photosData
                mainViewModel.videosData
                mainViewModel.tempAllTrashData
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
                        File(it.path).deleteRecursively()
                        ImagesDatabase.getDatabase(applicationContext).favoriteImageDao()
                            .deleteImages(it)
                    }
                }

            }, 100 * 30, 10000 * 30)
        }
    }


    private var lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                launchIfNotActive()
            }

            Lifecycle.Event.ON_START -> {
                launchIfNotActive()
                Log.d("TAG", "onChange: :::= ON_START")
            }

            else -> {}
        }
    }

    private fun launchIfNotActive() {

        try {
            if (job == null || job?.isActive == false && !isMainSelection) {
                job = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        mainViewModel.getMediaFromInternalStorage()
                    } catch (e: Exception) {
                        // Handle exception specific to fetchgroupNormal()
                        Log.e("TAG", "Exception in fetchgroupNormal(): ${e.message}")
                    }
                }
            } else {
                Log.d("TAG", "onChange: ::: Already")
            }
        } catch (e: Exception) {
            Log.e("TAG", "Exception outside coroutine: ${e.message}")
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
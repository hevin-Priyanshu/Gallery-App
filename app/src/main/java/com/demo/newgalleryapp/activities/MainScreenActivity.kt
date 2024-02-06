package com.demo.newgalleryapp.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.demo.newgalleryapp.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.fragments.AlbumsFragment
import com.demo.newgalleryapp.fragments.MediaFragment
import com.demo.newgalleryapp.fragments.MediaFragment.Companion.viewPager
import com.demo.newgalleryapp.fragments.PhotosFragment
import com.demo.newgalleryapp.fragments.SettingFragment
import com.demo.newgalleryapp.fragments.VideosFragment
import com.demo.newgalleryapp.interfaces.FolderClickListener
import com.demo.newgalleryapp.interfaces.ImageClickListener
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_CHANGES_IN_OPEN_IMAGE_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_CHANGES_IN_TRASH_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_PERMISSION
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_TRASH_PERMISSION_IN_MAIN_SCREEN_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.resetVisibilityForDeleteItem
import com.demo.newgalleryapp.utilities.CommonFunctions.showAppSettings
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupForMoveToTrashBin
import com.demo.newgalleryapp.utilities.CommonFunctions.showToast
import com.demo.newgalleryapp.viewmodel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainScreenActivity : AppCompatActivity(), ImageClickListener, FolderClickListener {

    private lateinit var settingFragment: SettingFragment
    private lateinit var service: ExecutorService
    private lateinit var progressBar: ProgressBar
    private var checkBoxList: ArrayList<MediaModel> = ArrayList()
    lateinit var mediaFragment: MediaFragment
    private lateinit var albumsFragment: AlbumsFragment
    private var currentFragment: Fragment? = null
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    companion object {
        lateinit var bottomNavigationViewForLongSelect: BottomNavigationView
        lateinit var bottomNavigationView: BottomNavigationView
        lateinit var photosFragment: PhotosFragment
        lateinit var videosFragment: VideosFragment
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CODE_FOR_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMediaData()
            } else {
                Toast.makeText(this, "Permission Required!!!", Toast.LENGTH_SHORT).show()
                showRationaleOrOpenSettings()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == REQ_CODE_FOR_TRASH_PERMISSION_IN_MAIN_SCREEN_ACTIVITY && resultCode == Activity.RESULT_OK) || (resultCode == REQ_CODE_FOR_CHANGES_IN_TRASH_ACTIVITY) || (requestCode == REQ_CODE_FOR_CHANGES_IN_OPEN_IMAGE_ACTIVITY && resultCode == Activity.RESULT_OK)) {
            (application as AppClass).mainViewModel.getMediaFromInternalStorage()
            photosFragment.imagesAdapter?.notifyDataSetChanged()
            videosFragment.imagesAdapter?.notifyDataSetChanged()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main_screen)
        bottomNavigationView = findViewById(R.id.bottomNavigationDefault)
        bottomNavigationViewForLongSelect = findViewById(R.id.bottomNavigationSelect)
        progressBar = findViewById(R.id.progressBar)

        service = Executors.newSingleThreadExecutor()
        sharedPreferencesHelper = SharedPreferencesHelper(this)

        progressBar.visibility = View.VISIBLE
        if (permissionCheck()) {
            service.execute {
                loadMediaData()
            }
        } else {
            askForPermission()
        }

    }

    // Refactored function to load media data
    private fun loadMediaData() {

        (application as AppClass).mainViewModel = MainViewModel(application)

        mediaFragment = MediaFragment.newInstance()
        albumsFragment = AlbumsFragment.newInstance()
        settingFragment = SettingFragment.newInstance()
        photosFragment = PhotosFragment.newInstance(0)
        videosFragment = VideosFragment.newInstance(1)

        loadData()
    }

    private fun setupFragments() {
        val fragmentTag = mediaFragment.javaClass.simpleName
        val existingFragment = supportFragmentManager.findFragmentByTag(fragmentTag)

        if (existingFragment == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.frameLayoutView, mediaFragment, fragmentTag).commit()
        }
        currentFragment = mediaFragment
    }

    private fun loadData() {

        runOnUiThread {
            progressBar.visibility = View.GONE

//            setupFragments()
            bottomNavigationView.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.mediaItem -> {
                        loadFragment(mediaFragment)
                    }

                    R.id.albumsItem -> {
                        loadFragment(albumsFragment)
                    }

                    R.id.settingsItem -> {
                        loadFragment(settingFragment)
                    }
                }
                true
            }

            bottomNavigationView.selectedItemId = R.id.mediaItem
        }

        bottomNavigationViewForLongSelect.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.shareItem -> shareSelectedImages()
                R.id.favoriteItem -> handleFavoriteAction()
                R.id.deleteItem -> handleDeleteAction()
                R.id.moreItem -> handleMoreAction()
            }
            true
        }

    }

    private fun handleMoreAction() {}

    private fun handleDeleteAction() {
        checkBoxList.clear()

        val fragmentList = if (viewPager.currentItem == 0) {
            // if photo is selected, get selected items from photosFragment
            photosFragment.imagesAdapter?.checkSelectedList
        } else {
            // If video is selected, get selected items from videoFragment
            videosFragment.imagesAdapter?.checkSelectedList
        }
//        val photosFragmentList = photosFragment.imagesAdapter!!.checkSelectedList

        checkBoxList.addAll(fragmentList!!)
        val paths = checkBoxList.map { it.path }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (paths.isNotEmpty()) {
                val arrayList: ArrayList<Uri> = ArrayList()
                MediaScannerConnection.scanFile(
                    this, paths.toTypedArray(), null
                ) { _, uri ->
                    arrayList.add(uri)
                    try {
                        if (arrayList.size == paths.size) {
                            val pendingIntent: PendingIntent =
                                MediaStore.createTrashRequest(contentResolver, arrayList, true)
                            startIntentSenderForResult(
                                pendingIntent.intentSender,
                                REQ_CODE_FOR_TRASH_PERMISSION_IN_MAIN_SCREEN_ACTIVITY,
                                null,
                                0,
                                0,
                                0
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("TAG", "000AAA: $e")
                    }
                }
                resetVisibilityForDeleteItem()
            } else {
                showToast(this, "No images Selected to delete")
            }
        } else {
            if (paths.isNotEmpty()) {
                showDeleteConfirmationDialog(paths)
            } else {
                showToast(this, "No images Selected to delete")
            }
        }
    }

    private fun showDeleteConfirmationDialog(paths: List<String>) {
        if (paths.isNotEmpty()) {
            showPopupForMoveToTrashBin(
                bottomNavigationViewForLongSelect,
                paths, this@MainScreenActivity, paths, 0
            )
        } else {
            showToast(this, "Error: Image not found")
        }
    }

    private fun handleFavoriteAction() {
        checkBoxList.clear()
        checkBoxList.addAll(photosFragment.imagesAdapter!!.checkSelectedList)
//        val paths = checkBoxList.map { it.path }

        if (checkBoxList.isEmpty()) {
            showToast(this, "No images selected to add favorites!!")
        } else {
            for (addFavorite in checkBoxList) {
//            val roomModel =  ImagesDatabase.getDatabase(this@MainScreenActivity).favoriteImageDao().getModelByFile(addFavorite)
                ImagesDatabase.getDatabase(this@MainScreenActivity).favoriteImageDao()
                    .insertFavorite(addFavorite)
            }
            showToast(this, "Favorite Added")
            resetVisibilityForDeleteItem()
        }
    }

    private fun shareSelectedImages() {
        checkBoxList.clear()
        checkBoxList.addAll(photosFragment.imagesAdapter?.checkSelectedList.orEmpty())

        val selectedImages = checkBoxList.map { it.path }

        if (selectedImages.isNotEmpty()) {
            val uris = ArrayList<Uri>()

            // Convert file paths to Uri using FileProvider
            for (file in selectedImages) {
                val uri = FileProvider.getUriForFile(
                    this, "com.demo.newgalleryapp.fileprovider", File(file)
                )
                uris.add(uri)
            }
            // Handle share item
            (application as AppClass).mainViewModel.shareMultipleImages(uris, this)
        } else {
            showToast(this, "No images selected to share")
        }
    }

    private fun askForPermission() {

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        ActivityCompat.requestPermissions(this, permission, REQ_CODE_FOR_PERMISSION)
    }

    private fun permissionCheck(): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            Environment.isExternalStorageManager()
            val readMediaImagesPermission = Manifest.permission.READ_MEDIA_IMAGES
            val readMediaVideoPermission = Manifest.permission.READ_MEDIA_VIDEO

            (ContextCompat.checkSelfPermission(
                this, readMediaImagesPermission
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
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

    private fun showRationaleOrOpenSettings() {
        val rationaleDialog = AlertDialog.Builder(this).setTitle("Permission Required")
            .setMessage("This feature requires storage permission. Please grant the permission in settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                showAppSettings()
                finish()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish()
            }.create()

        rationaleDialog.show()
    }

//    private fun loadFragment(fragment: Fragment, flag: Boolean) {
//        val fragmentManager = supportFragmentManager
//        val fragmentTransaction = fragmentManager.beginTransaction()
//
//        val currentFragment = fragmentManager.findFragmentById(R.id.frameLayoutView)
//        if (currentFragment != null) {
//            fragmentTransaction.remove(currentFragment)
//        }
//
//        if (flag) {
//            fragmentTransaction.add(R.id.frameLayoutView, fragment)
//        } else {
//            fragmentTransaction.replace(R.id.frameLayoutView, fragment)
//        }
//        fragmentTransaction.addToBackStack(null)
//        fragmentTransaction.commit()
//    }


    private fun loadFragment(fragment: Fragment) {

        val transaction = supportFragmentManager.beginTransaction()

        // Check if the currentFragment is not null and added
        if (currentFragment != null && currentFragment?.isAdded == true) {
            transaction.hide(currentFragment!!)
        }

        val fragmentTag = fragment.javaClass.simpleName
        val existingFragment = supportFragmentManager.findFragmentByTag(fragmentTag)

        if (existingFragment == null) {
            transaction.add(R.id.frameLayoutView, fragment, fragmentTag)
        } else {
            transaction.show(existingFragment)
        }

        transaction.addToBackStack(null)
        transaction.commit()

        currentFragment = fragment
    }

    override fun onClick(folderPath: String) {

    }

    override fun onLongClick() {

    }

    override fun counter(select: Int) {
    }


}
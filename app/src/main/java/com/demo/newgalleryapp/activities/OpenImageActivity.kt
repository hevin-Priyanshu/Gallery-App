package com.demo.newgalleryapp.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.view.get
import androidx.viewpager.widget.ViewPager
import com.demo.newgalleryapp.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.photosFragment
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.videosFragment
import com.demo.newgalleryapp.adapters.ImageSliderAdapter
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.utilities.CommonFunctions
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_CHANGES_IN_EDIT_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_IMAGE_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_UPDATES_IN_OPEN_IMAGE_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.formatDate
import com.demo.newgalleryapp.utilities.CommonFunctions.formatTime
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupForMoveToTrashBinForOpenActivityOnlyOne
import com.demo.newgalleryapp.utilities.CommonFunctions.showRenamePopup
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class OpenImageActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var textView: TextView
    private lateinit var backBtn: ImageView
    private lateinit var timeOfImage: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var originalFilePath: File
    private lateinit var newFile: File
    private var tempList: ArrayList<MediaModel> = ArrayList()
    private var popupWindow: PopupWindow? = null
    private var fabCount = 0
    private var anyChanges: Boolean = false

    companion object {
        lateinit var models: List<MediaModel>
        lateinit var imagesSliderAdapter: ImageSliderAdapter
    }

    private val handler = Handler()

    private fun copyFiles(file: File, destinationDir: File) {
//        val copiedFiles = mutableListOf<TrashBin>()

//        files.forEach { trash ->

//            val file = File(trash.destinationImagePath)
        // Create a new File object for the destination file
        try {
            val destinationFile = File(destinationDir, file.name)

            // Open an input stream for reading from the source file
            val inputStream = FileInputStream(file)

            // Open an output stream for writing to the destination file
            val outputStream = FileOutputStream(destinationFile)
            try {
                // Define a buffer for reading from the input stream
                val buffer = ByteArray(1024)
                var length: Int


                // Read from the input stream and write to the output stream
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
//                copiedFiles.add(destinationFile) // Add the copied file to the list
//                copiedFiles.add(TrashBin(trash.id, destinationFile.path, trash.destinationImagePath, trash.deletionTimestamp)) // Add the copied file to the list

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Close the streams
                inputStream.close()
                outputStream.close()
            }
//        }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        Log.d("MyApp", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_IMAGE_ACTIVITY && resultCode == Activity.RESULT_OK) {
            (application as AppClass).mainViewModel.getMediaFromInternalStorage()
            imagesSliderAdapter.remove(viewPager.currentItem)
            imagesSliderAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Delete Success.", Toast.LENGTH_SHORT).show()
        } else if ((requestCode == REQ_CODE_FOR_CHANGES_IN_EDIT_ACTIVITY && resultCode == Activity.RESULT_OK) ||
            (requestCode == REQ_CODE_FOR_UPDATES_IN_OPEN_IMAGE_ACTIVITY && resultCode == Activity.RESULT_OK)
        ) {
            (application as AppClass).mainViewModel.getMediaFromInternalStorage()
            photosFragment.imagesAdapter?.notifyDataSetChanged()
            videosFragment.imagesAdapter?.notifyDataSetChanged()

        } else if (requestCode == 777 && resultCode == Activity.RESULT_OK) {
            try {
                originalFilePath.copyTo(newFile)
                originalFilePath.delete()
                Log.d("newFile", "onActivityResult: $newFile")
                Toast.makeText(this, "Image Rename Successfully.", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to copy image.", Toast.LENGTH_SHORT).show()
                Log.e("error12", "onActivityResult: ${e.message}")
            }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            val result = CropImage.getActivityResult(data)

            if (resultCode == Activity.RESULT_OK) {
                result.uri?.let { uri ->
//                    imagesSliderAdapter?.setImages(uri)
                    Log.d("cropView", "onActivityResult: $uri")
                    val intent = Intent(this, EditActivity::class.java)
                    intent.putExtra("Uri", uri.toString())
                    startActivityForResult(intent, REQ_CODE_FOR_CHANGES_IN_EDIT_ACTIVITY)
//                    saveImageToDestination(uri, File(File(models[viewPager.currentItem].path).parent, "edited_image_${System.currentTimeMillis()}.jpg").path)
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Log.e("cropView", "onActivityResult: ${result.error}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_image)

        initView()

        val isFolder = intent.hasExtra("folderPosition")
        fabCount = intent.getIntExtra("selectedImagePosition", 0)

        if (isFolder) {
            models = (application as AppClass).mainViewModel.folderList[intent.getIntExtra(
                "folderPosition", 0
            )].models
            setViewPagerAdapter(models as ArrayList<MediaModel>, fabCount)
        } else {
            val menuItem = bottomNavigationView.menu.findItem(R.id.editItem)
            when (intent.getIntExtra("currentState", 0)) {
                1 -> {
                    val sM: MediaModel = intent.extras?.get("selectedModel") as MediaModel

                    if (!sM.isVideo) {
                        fabCount = (application as AppClass).mainViewModel.tempPhotoList.indexOf(sM)
                        models = (application as AppClass).mainViewModel.tempPhotoList
                        setViewPagerAdapter(models as ArrayList<MediaModel>, fabCount)

                        menuItem.isVisible = true
                    } else {
                        fabCount = (application as AppClass).mainViewModel.tempVideoList.indexOf(sM)
                        models = (application as AppClass).mainViewModel.tempVideoList
                        setViewPagerAdapter(models as ArrayList<MediaModel>, fabCount)

                        menuItem.isVisible = false
                    }
                }

                2 -> {
                    sendFavoriteListToViewPager()
                }
            }
        }

        backBtnHandle()
        bottomNavigationViewItemSetter()
        viewPagerDataSetter()
    }

    private fun initView() {
        viewPager = findViewById(R.id.viewPager_slider)
        textView = findViewById(R.id.open_text_view_image_activity)
        backBtn = findViewById(R.id.back_btn)
        toolbar = findViewById(R.id.toolBar)
        timeOfImage = findViewById(R.id.image_time)
        bottomNavigationView = findViewById(R.id.bottomNavigation_for_images)

        // Enable marquee effect
        textView.isSelected = true
    }

    // ***************************  ALL OTHER METHODS  ************************** //

    private fun viewPagerDataSetter() {
        viewPager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {
                textView.text = models[position].displayName
                timeOfImage.text = CommonFunctions.formatTime(models[position].date)
                setFavoriteIcon(position)
                fabCount = position
            }

            override fun onPageSelected(position: Int) {
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

//    private fun setTapListener(photoView: PhotoView?) {
//        val attacher = PhotoViewAttacher(photoView)
//        attacher.setOnViewTapListener { _, _, _ ->
//            stopSlideShow()
//        }
//    }

//    private fun stopSlideShow() {
//        timer.cancel()
//        // Optionally, remove any OnPageChangeListener if added
//        viewPager.clearOnPageChangeListeners()
//    }

    private fun setViewPagerAdapter(model: ArrayList<MediaModel>, currentPosition: Int) {
        models = model
        imagesSliderAdapter = ImageSliderAdapter(this@OpenImageActivity, model)
        viewPager.adapter = imagesSliderAdapter
        viewPager.setCurrentItem(currentPosition, false)
    }

    private fun sendFavoriteListToViewPager() {

        ImagesDatabase.getDatabase(this).favoriteImageDao().getAllFavorites().observe(this) {
            tempList.addAll(it)
            models = tempList
            setViewPagerAdapter(models as ArrayList<MediaModel>, fabCount)
        }
    }

    private fun bottomNavigationViewItemSetter() {

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            val currentPosition = viewPager.currentItem
            when (menuItem.itemId) {
                R.id.shareItem -> handleShareItem(currentPosition)
                R.id.favoriteItemUnSelect -> handleFavoriteItem(currentPosition)
                R.id.editItem -> handleEditItem(currentPosition)
                R.id.deleteItem -> handleDeleteItem(currentPosition)
                R.id.moreItem -> handleMoreItem()
            }
            true
        }
    }

    private fun handleMoreItem() {
        moreItemClick(bottomNavigationView)
    }

    private fun handleEditItem(currentPosition: Int) {
//        val currentPosition = viewPager.currentItem
        val imagePath = models[currentPosition].path
        val file = File(imagePath)
        val uri = Uri.fromFile(file)
        launchImageCrop(uri)
    }

    private fun handleDeleteItem(currentPosition: Int) {
//        val currentPosition = viewPager.currentItem
//        val imageToDelete = models.getOrNull(currentPosition)?.path
        val imageToDelete = models[currentPosition].path

        val filePath = File(imageToDelete)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            val arrayList: ArrayList<Uri> = ArrayList()
            MediaScannerConnection.scanFile(this, arrayOf(filePath.path), null) { _, uri ->
                arrayList.add(uri)
                try {
                    copyFiles(
                        filePath, (application as AppClass).mainViewModel.createTrashDirectory()
                    )
                    val pendingIntent: PendingIntent =
                        MediaStore.createTrashRequest(contentResolver, arrayList, true)
                    startIntentSenderForResult(
                        pendingIntent.intentSender,
                        REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_IMAGE_ACTIVITY,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
//                    copyFiles(filePath, (application as AppClass).mainViewModel.createTrashDirectory())
                } catch (e: Exception) {
                    Log.e("TAG", "000AAA: $e")
                }
            }
            (application as AppClass).mainViewModel.flag = true
        } else {
            if (imageToDelete.isNotEmpty()) {
                showPopupForMoveToTrashBinForOpenActivityOnlyOne(
                    bottomNavigationView, imageToDelete, currentPosition
                )
                anyChanges = true
            } else {
                CommonFunctions.showToast(this, "Error: Image not found")
            }

//            if (!imageToDelete.isNullOrEmpty()) {
//                AlertDialog.Builder(this).setTitle("Delete 1 Item ?")
//                    .setMessage("Are you sure to move 1 file to the trash bin?")
//                    .setPositiveButton("Delete") { _, _ ->
//                        // User clicked "Yes", proceed with deletion
//                        (application as AppClass).mainViewModel.flag = true
//                        val deletedImagePath =
//                            // HERE I AM DELETING THE IMAGE WITH CURRENT PATH
//                            (application as AppClass).mainViewModel.moveImageInTrashBin(imageToDelete)
//                        deletedImagePath.let {
//                            imagesSliderAdapter.remove(
//                                currentPosition
//                            )
//                        }
//
//                    }.setNegativeButton("Cancel") { dialog, _ ->
//                        dialog.dismiss()
//                    }.show()
//            }
        }
    }

    private fun handleFavoriteItem(currentPosition: Int) {
//        val model = models[viewPager.currentItem]
        val model = models[currentPosition]
        val favoriteImageDao = ImagesDatabase.getDatabase(this).favoriteImageDao()

        val roomModel = favoriteImageDao.getModelByFile(model.path)

        if (roomModel == null) {
            favoriteImageDao.insertFavorite(model)
            Toast.makeText(this, "Favorite Added", Toast.LENGTH_SHORT).show()
        } else {
            favoriteImageDao.deleteFavorite(roomModel)
        }
        if (intent.getIntExtra("currentState", 0) == 2) {
            setFavoriteIcon(fabCount)
        } else {
            setFavoriteIcon(fabCount)
        }
    }

    private fun handleShareItem(currentPosition: Int) {
//        val currentPosition = viewPager.currentItem
        val share = models[currentPosition].path
        val uri =
            FileProvider.getUriForFile(this, "com.demo.newgalleryapp.fileprovider", File(share))
        // Handle share item
        (application as AppClass).mainViewModel.shareImage(uri, this)
    }

    private fun moreItemClick(bottomNavigationView: BottomNavigationView) {

        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.popup_more_item, null)

        popupWindow = PopupWindow(
            popupView, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT, true
        )

        popupWindow?.showAtLocation(
            bottomNavigationView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val popupTextWallpaper = popupView.findViewById<TextView>(R.id.setAsWallpaper)
        val popupTextCopy = popupView.findViewById<TextView>(R.id.Copy)
        val popupTextMove = popupView.findViewById<TextView>(R.id.Move)
        val popupTextRename = popupView.findViewById<TextView>(R.id.Rename)
        val popupTextSlideShow = popupView.findViewById<TextView>(R.id.Slideshow)
        val popupTextDetails = popupView.findViewById<TextView>(R.id.details)

        val selectedImagePath = models[viewPager.currentItem].path


        popupTextWallpaper.setOnClickListener {
//            setAsWallpaper(selectedImagePath)
            val intent = Intent(this, WallpaperActivity::class.java)
            intent.putExtra("wallpaperUri", selectedImagePath)
            startActivity(intent)
            popupWindow?.dismiss()
        }

        popupTextCopy.setOnClickListener {
            val intent = Intent(this, CopyOrMoveActivity::class.java)
            intent.putExtra("copyImagePath", selectedImagePath)
            startActivityForResult(intent, REQ_CODE_FOR_UPDATES_IN_OPEN_IMAGE_ACTIVITY)
            popupWindow?.dismiss()

        }

        popupTextMove.setOnClickListener {
            val intent = Intent(this, CopyOrMoveActivity::class.java)
            intent.putExtra("moveImagePath", selectedImagePath)
            startActivityForResult(intent, REQ_CODE_FOR_UPDATES_IN_OPEN_IMAGE_ACTIVITY)
            popupWindow?.dismiss()
        }

        popupTextRename.setOnClickListener {
            popupWindow?.dismiss()
            showRenamePopup(bottomNavigationView, selectedImagePath, this@OpenImageActivity)

//            val editText = EditText(this@OpenImageActivity)
//            val dialog = AlertDialog.Builder(this@OpenImageActivity).setTitle("Rename Image")
//                .setMessage("Enter new name:").setView(editText)
//                .setPositiveButton("Rename") { _, _ ->
//
//                    val newName = editText.text.toString().trim()
//                    val directory = File(selectedImagePath).parent
//                    val originalPath = File(selectedImagePath)
//                    val lastText = getImageFileExtension(selectedImagePath)
//                    val destinationPath = File(directory, "$newName.$lastText")
//
//                    if (newName.isNotEmpty()) {
//
//                        try {
//                            originalPath.renameTo(destinationPath)
//                            (application as AppClass).mainViewModel.scanFile(this, destinationPath)
//                            (application as AppClass).mainViewModel.scanFile(this, originalPath)
//                            Toast.makeText(this, "success", Toast.LENGTH_SHORT).show()
//                        } catch (e: IOException) {
//                            Log.e("CopyOrMoveActivity", "Error creating write request", e)
//                        }
////                            renameImage(selectedImagePath, newName)
//                        popupWindow?.dismiss()
//                    } else {
//                        Toast.makeText(this, "Please enter a valid name.", Toast.LENGTH_SHORT)
//                            .show()
//                    }
//                }.setNegativeButton("No") { dialog, _ ->
//                    // User clicked "No", do nothing
//                    popupWindow?.dismiss()
//                    dialog.dismiss()
//                }.show()
//
//            dialog.show()
        }

        popupTextSlideShow.setOnClickListener {
            val intent = Intent(this, SlideShowActivity::class.java)
            intent.putExtra("SlideImagePath", selectedImagePath)
            intent.putExtra("SlideImagePosition", viewPager.currentItem)
            startActivity(intent)
            popupWindow?.dismiss()
        }

        popupTextDetails.setOnClickListener {
            popupWindow?.dismiss()
            showDetails(toolbar)
        }

        val popupItem = popupView.findViewById<LinearLayout>(R.id.popupItem_more)


        popupItem.setOnClickListener {
            // Handle click on popup item
            popupWindow?.dismiss()
        }

        // Set dismiss listener to nullify the reference
        popupWindow?.setOnDismissListener {
            popupWindow = null
        }

    }

    private fun launchImageCrop(uri: Uri) {
        CropImage.activity(uri).setGuidelines(CropImageView.Guidelines.ON)
            .setAspectRatio(2000, 1500).setCropShape(CropImageView.CropShape.RECTANGLE).start(this)
    }

    private fun showDetails(anchorView: View) {
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.popup_three_dot, null)

        popupWindow = PopupWindow(
            popupView, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT, true
        )

        popupWindow?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val popupTextViewName = popupView.findViewById<TextView>(R.id.image_name_popup)
        val popupTextViewTime = popupView.findViewById<TextView>(R.id.time_popup)
        val popupTextSize = popupView.findViewById<TextView>(R.id.size_popup)
        val popupTextDate = popupView.findViewById<TextView>(R.id.date_popup)
        val popupTextViewPath = popupView.findViewById<TextView>(R.id.path_popup)

        popupTextViewPath.movementMethod = LinkMovementMethod.getInstance()

        val path = models[viewPager.currentItem].path
        popupTextViewPath.text = path
        popupTextViewName.text = models[viewPager.currentItem].displayName

        val size = models[viewPager.currentItem].size

        val date = formatDate(models[viewPager.currentItem].date)
        val time = formatTime(models[viewPager.currentItem].date)

        val sizeInBytes = size ?: 0L  // Default to 0 if size is null
        val formattedSize = CommonFunctions.formatSize(sizeInBytes)
        popupTextSize.text = formattedSize

        popupTextDate.text = date
        popupTextViewTime.text = time

        val popupItem = popupView.findViewById<LinearLayout>(R.id.popupItem)

        popupItem.setOnClickListener {
            // Handle click on popup item
            popupWindow?.dismiss()
        }

        // Set dismiss listener to nullify the reference
        popupWindow?.setOnDismissListener {
            popupWindow = null
        }
    }

    override fun onBackPressed() {

        if (anyChanges) {
            val intent = Intent()
            setResult(Activity.RESULT_OK, intent)
            anyChanges = false
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onBackPressed()
    }

    private fun backBtnHandle() {
        backBtn.setOnClickListener {
            if (anyChanges) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            anyChanges = false
        }
    }

    private fun setFavoriteIcon(currentItem: Int) {

        if (ImagesDatabase.getDatabase(this).favoriteImageDao()
                .getModelByFile(models[currentItem].path) == null
        ) {
            bottomNavigationView.menu[1].setIcon(R.drawable.favorite_icon_new)
        } else {
            bottomNavigationView.menu[1].setIcon(R.drawable.favorite_blue_color_icon)
        }
    }

    // Remove the runnable to prevent memory leaks
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onStop() {
        // Remove the callbacks to stop the slideshow when the activity is not visible
        handler.removeCallbacksAndMessages(null)
        super.onStop()
    }

}
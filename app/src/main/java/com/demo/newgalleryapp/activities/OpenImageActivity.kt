package com.demo.newgalleryapp.activities

import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.ImageSliderAdapter2
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.databinding.DialogLoadingBinding
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.utilities.CommonFunctions
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_CHANGES_IN_EDIT_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_IMAGE_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_UPDATES_IN_OPEN_IMAGE_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_WRITE_PERMISSION_IN_OPEN_IMAGE_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.formatDate
import com.demo.newgalleryapp.utilities.CommonFunctions.formatTime
import com.demo.newgalleryapp.utilities.CommonFunctions.inVisible
import com.demo.newgalleryapp.utilities.CommonFunctions.isAutoSlidingEnabled
import com.demo.newgalleryapp.utilities.CommonFunctions.positionForItem
import com.demo.newgalleryapp.utilities.CommonFunctions.setNavigationColor
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupForMoveToTrashBinForOpenActivityOnlyOne
import com.demo.newgalleryapp.utilities.CommonFunctions.showRenamePopup
import com.demo.newgalleryapp.utilities.CommonFunctions.showToast
import com.demo.newgalleryapp.utilities.CommonFunctions.visible
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File
import java.util.Timer
import java.util.TimerTask


class OpenImageActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var textView: TextView
    private lateinit var backBtn: ImageView
    private lateinit var timeOfImage: TextView
    private lateinit var openImageMainViewVisibility: RelativeLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private var tempList: ArrayList<MediaModel> = ArrayList()
    private var popupWindow: PopupWindow? = null
    private var fabCount = 0
    private var handler: Handler? = null
    private var currentState: Int? = null
    var modelsList: ArrayList<MediaModel> = arrayListOf()
    private var currentImageIndex = 0
    private var timer: Timer? = null
    private var isSlideShow: Boolean = false

    companion object {
        var anyChanges: Boolean = false
        lateinit var imagesSliderAdapter: ImageSliderAdapter2
    }

    private lateinit var dialogBinding: DialogLoadingBinding

    val progressDialogFragment by lazy {
        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.rounded_border_shape))
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialogBinding = DialogLoadingBinding.inflate(dialog.layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        Log.d("MyApp", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_IMAGE_ACTIVITY && resultCode == Activity.RESULT_OK) {
            anyChanges = true

            (application as AppClass).mainViewModel.getMediaFromInternalStorage()
            val imageToDelete = modelsList[viewPager.currentItem].path

            ImagesDatabase.getDatabase(this@OpenImageActivity).favoriteImageDao()
                .deleteFavorites(imageToDelete)

            imagesSliderAdapter.remove(viewPager.currentItem, this@OpenImageActivity)
            modelsList.removeAt(viewPager.currentItem)

            positionForItem = viewPager.currentItem
            imagesSliderAdapter.notifyDataSetChanged()
            showToast(this, "Deleted Successfully!!")

        } else if ((requestCode == REQ_CODE_FOR_CHANGES_IN_EDIT_ACTIVITY && resultCode == Activity.RESULT_OK)) {
            Log.d("true", "onActivityResult: true")
            // Changes In Edit Activity ,For adding new filter images
            (application as AppClass).mainViewModel.getMediaFromInternalStorage()
//            imagesSliderAdapter.notifyDataSetChanged()
//            photosFragment.imagesAdapter?.notifyDataSetChanged()
//            videosFragment.imagesAdapter?.notifyDataSetChanged()

        } else if ((requestCode == REQ_CODE_FOR_UPDATES_IN_OPEN_IMAGE_ACTIVITY && resultCode == Activity.RESULT_OK)) {
            // Changes In Copy or Move
            (application as AppClass).mainViewModel.getMediaFromInternalStorage()
//            imagesSliderAdapter.notifyDataSetChanged()
//            photosFragment.imagesAdapter?.notifyDataSetChanged()
//            videosFragment.imagesAdapter?.notifyDataSetChanged()
        } else if (requestCode == REQ_CODE_FOR_WRITE_PERMISSION_IN_OPEN_IMAGE_ACTIVITY && resultCode == Activity.RESULT_OK) {
            popupWindow?.dismiss()
            val selectedImagePath = modelsList[viewPager.currentItem].path

            // Here rename image in android 11 and above device
            showRenamePopup(bottomNavigationView, selectedImagePath, this@OpenImageActivity)
//            modelsList.set(viewPager.currentItem)
//
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
        handler = Handler(Looper.getMainLooper())

        val isFolder = intent.hasExtra("folderPosition")
        fabCount = intent.getIntExtra("selectedImagePosition", 0)
        currentImageIndex = intent.getIntExtra("SlideImagePosition", 0)
        isSlideShow = intent.hasExtra("FromSlideShow")

        if (isSlideShow) {
            modelsList = (application as AppClass).mainViewModel.tempPhotoList
            setViewPagerAdapter(modelsList, currentImageIndex, false)
            isAutoSlidingEnabled = true
            hideUiItem()
            startSlideshow()
        }

        if (isFolder) {
            // Here passing the folder list to viewpager , if isFolder is true (i.e Albums folder)
            modelsList = (application as AppClass).mainViewModel.folderList[intent.getIntExtra(
                "folderPosition", 0
            )].models
            setViewPagerAdapter(modelsList, fabCount, isFolder)

        } else {
            val menuItem = bottomNavigationView.menu.findItem(R.id.editItem)

            currentState = intent.getIntExtra("currentState", 0)

            when (currentState) {
                1 -> {
                    val sM: MediaModel = intent.getSerializableExtra("selectedModel") as MediaModel

                    if (!sM.isVideo) {
                        fabCount = (application as AppClass).mainViewModel.tempPhotoList.indexOf(sM)
                        modelsList = (application as AppClass).mainViewModel.tempPhotoList
                        setViewPagerAdapter(modelsList, fabCount, isFolder)

                        // Here showing edit button for images
                        menuItem.isVisible = true
                    } else {
                        fabCount = (application as AppClass).mainViewModel.tempVideoList.indexOf(sM)
                        modelsList = (application as AppClass).mainViewModel.tempVideoList
                        setViewPagerAdapter(modelsList, fabCount, isFolder)

                        // Here hiding edit button for videos
                        menuItem.isVisible = false
                    }
                }

                2 -> {
                    // here on that , we set the favorite images in view pager
                    sendFavoriteListToViewPager()
                }
            }
        }

        backBtnHandle()
        bottomNavigationViewItemSetter()
        viewPagerDataSetter()

        // This will do when, if you want to apply any other icon drawable for menu item
        bottomNavigationView.itemIconTintList = null
    }

    private fun initView() {

        viewPager = findViewById(R.id.viewPager_slider)
        openImageMainViewVisibility = findViewById(R.id.openImageMainView)
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

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                if (modelsList.isNotEmpty() && position >= 0 && position < modelsList.size) {
                    textView.text = modelsList[position].displayName
                    timeOfImage.text = formatTime(modelsList[position].date)
                    setFavoriteIcon(position)
                }

            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                fabCount = position
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })

//        viewPager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
//            override fun onPageScrolled(
//                position: Int, positionOffset: Float, positionOffsetPixels: Int
//            ) {
//                textView.text = models[position].displayName
//                timeOfImage.text = formatTime(models[position].date)
//                setFavoriteIcon(position)
//                fabCount = position
//            }
//
//            override fun onPageSelected(position: Int) {
//            }
//
//            override fun onPageScrollStateChanged(state: Int) {
//            }
//        })
    }

    fun animation() {
        stopSlideshow()
        if (toolbar.isVisible) {
            hideUiItem()
        } else {
            showUiItem()
        }
    }

    private fun showUiItem() {
        toolbar.visible()
        bottomNavigationView.visible()
        openImageMainViewVisibility.setBackgroundColor(getColor(R.color.white))
        setNavigationColor(window, Color.WHITE)
    }

    private fun hideUiItem() {
        toolbar.inVisible()
        bottomNavigationView.inVisible()
        setNavigationColor(window, ContextCompat.getColor(this, R.color.black))
        openImageMainViewVisibility.setBackgroundColor(getColor(R.color.black))
    }


    private fun setViewPagerAdapter(
        model: ArrayList<MediaModel>, currentPosition: Int, isFolder: Boolean
    ) {

        if (model.size == 0) {
            if (anyChanges) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
                anyChanges = false
            }
            finish()
        }

        modelsList.addAll(model)
        val temp = arrayListOf<MediaModel>()
        temp.addAll(model)
        imagesSliderAdapter = ImageSliderAdapter2(this@OpenImageActivity, temp, isFolder)
        viewPager.adapter = imagesSliderAdapter
        viewPager.setCurrentItem(currentPosition, false)

    }

    private fun sendFavoriteListToViewPager() {

        ImagesDatabase.getDatabase(this).favoriteImageDao().getAllFavorites().observe(this) {
            tempList.clear()
            tempList.addAll(it)
            modelsList.addAll(tempList)

            setViewPagerAdapter(modelsList, fabCount, false)
        }
    }

    private fun bottomNavigationViewItemSetter() {

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            val currentPosition = fabCount
            when (menuItem.itemId) {
                R.id.shareItem -> handleShareItem(currentPosition)
                R.id.favoriteItemUnSelect -> handleFavoriteItem(currentPosition)
                R.id.editItem -> handleEditItem(currentPosition)
                R.id.deleteItem -> handleDeleteItem(currentPosition)
                R.id.moreItem -> handleMoreItem(currentPosition)
            }
            true
        }
    }

    private fun handleMoreItem(currentPosition: Int) {
        val isVideoOrNot = modelsList[currentPosition].isVideo
        moreItemClick(bottomNavigationView, isVideoOrNot, currentPosition)
    }

    private fun handleEditItem(currentPosition: Int) {
//        val currentPosition = viewPager.currentItem
        val imagePath = modelsList[currentPosition].path
        val file = File(imagePath)
        val uri = Uri.fromFile(file)
        launchImageCrop(uri)
    }

    private fun handleDeleteItem(currentPosition: Int) {

        val imageToDelete = modelsList[currentPosition].path

        val filePath = File(imageToDelete)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            val arrayList: ArrayList<Uri> = ArrayList()

            MediaScannerConnection.scanFile(this, arrayOf(filePath.path), null) { _, uri ->
                arrayList.clear()
                arrayList.add(uri)
                try {
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

                } catch (e: Exception) {
                    Log.e("TAG", "000AAA: $e")
                }
            }
        } else {
            if (imageToDelete.isNotEmpty()) {
                showPopupForMoveToTrashBinForOpenActivityOnlyOne(
                    bottomNavigationView,
                    imageToDelete,
                    currentPosition,
                    modelsList[currentPosition].isVideo,
                    currentState,
                    this@OpenImageActivity
                )
            } else {
                showToast(this, "Error: Image not found")
            }
        }
    }

    private fun handleFavoriteItem(currentPosition: Int) {
//        val model = models[viewPager.currentItem]
        val model = modelsList[currentPosition]
        val favoriteImageDao = ImagesDatabase.getDatabase(this).favoriteImageDao()

        val roomModel = favoriteImageDao.getModelByFile(model.path)

        if (roomModel == null) {
            favoriteImageDao.insertFavorite(model)
//            showToast(this, "Favorite Added")
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
        val share = modelsList[currentPosition].path
        val uri =
            FileProvider.getUriForFile(this, "com.demo.newgalleryapp.fileprovider", File(share))
        // Handle share item
        (application as AppClass).mainViewModel.shareImage(uri, this)
    }

    private fun moreItemClick(
        bottomNavigationView: BottomNavigationView, isVideoOrNot: Boolean, currentPosition: Int
    ) {

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

        val selectedImagePath = modelsList[currentPosition].path

        if (isVideoOrNot) {
            popupTextWallpaper.visibility = View.GONE
        } else {
            popupTextWallpaper.visibility = View.VISIBLE
        }

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                if (selectedImagePath.isNotEmpty()) {
                    try {
                        val arrayList: ArrayList<Uri> = ArrayList()
                        MediaScannerConnection.scanFile(
                            this, arrayOf(selectedImagePath), null
                        ) { file, uri ->

                            arrayList.add(uri)
                            val pendingIntent: PendingIntent =
                                MediaStore.createWriteRequest(contentResolver, arrayList)
                            startIntentSenderForResult(
                                pendingIntent.intentSender,
                                REQ_CODE_FOR_WRITE_PERMISSION_IN_OPEN_IMAGE_ACTIVITY,
                                null,
                                0,
                                0,
                                0,
                                null
                            )

                        }

                    } catch (e: Exception) {
                        Log.e("TAG", "bottomNavigationViewItemSetter: $e")
                    }
                } else {
                    showToast(this, "Error: Image not found!!!")
                }
            } else {

                showRenamePopup(
                    bottomNavigationView, selectedImagePath, this@OpenImageActivity
                )
                popupWindow?.dismiss()
            }
            //////////////////
        }

        popupTextSlideShow.setOnClickListener {
            val intent = Intent(this, OpenImageActivity::class.java)
            intent.putExtra("FromSlideShow", true)
            intent.putExtra("SlideImagePosition", currentPosition)
            startActivity(intent)
            finish()
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
            .setCropShape(CropImageView.CropShape.RECTANGLE).start(this)
    }

    private fun showDetails(anchorView: View) {
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.popup_menu_details, null)

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

        val path = modelsList[viewPager.currentItem].path
        popupTextViewPath.text = path
        popupTextViewName.text = modelsList[viewPager.currentItem].displayName

        val size = modelsList[viewPager.currentItem].size

        val date = formatDate(modelsList[viewPager.currentItem].date)
        val time = formatTime(modelsList[viewPager.currentItem].date)

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
        }
        anyChanges = false
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onBackPressed()
    }

    private fun backBtnHandle() {
        backBtn.setOnClickListener {
            if (anyChanges) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
            }
            anyChanges = false
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun setFavoriteIcon(currentItem: Int) {
        val favoriteMenuItem = bottomNavigationView.menu.findItem(R.id.favoriteItemUnSelect)

        val favoriteImageDao = ImagesDatabase.getDatabase(this).favoriteImageDao()
        val isFavorite = favoriteImageDao.getModelByFile(modelsList[currentItem].path) != null

        // Get the drawable resource based on the favorite status
        val drawableResId = if (isFavorite) R.drawable.favorite_blue_color_icon
        else R.drawable.favorite_icon_new

        // Get the drawable from the resource ID
        val drawable = ContextCompat.getDrawable(this, drawableResId)

        // Apply tint color to the drawable
        val iconTint = if (isFavorite) ContextCompat.getColor(this, R.color.color_main)
        else ContextCompat.getColor(this, R.color.icon_color)
        drawable?.let {
            val wrappedDrawable = DrawableCompat.wrap(it)
            DrawableCompat.setTint(wrappedDrawable, iconTint)
            favoriteMenuItem.icon = wrappedDrawable
        }
    }

    override fun onDestroy() {
        handler?.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onStop() {
        // Remove the callbacks to stop the slideshow when the activity is not visible
        handler?.removeCallbacksAndMessages(null)
        super.onStop()
    }

    private fun startSlideshow() {

        val handler = Handler()
        val update = Runnable {
            if (currentImageIndex == tempList.size - 1) {
                currentImageIndex = 0
            } else {
                currentImageIndex++
            }
            viewPager.setCurrentItem(currentImageIndex, true)
        }
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                if (isAutoSlidingEnabled) {
                    handler.post(update)
                }
            }
        }, 1000, 3000) // Auto-slide interval (3 seconds)


    }

    private fun stopSlideshow() {
        isSlideShow = false
        isAutoSlidingEnabled = false
        timer?.cancel()
    }


}
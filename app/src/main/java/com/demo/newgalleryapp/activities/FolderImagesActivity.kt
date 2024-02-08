package com.demo.newgalleryapp.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.demo.newgalleryapp.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.ImagesAdapter
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.interfaces.ImageClickListener
import com.demo.newgalleryapp.models.Folder
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_TRASH_PERMISSION_IN_FOLDER_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupForMoveToTrashBin
import com.demo.newgalleryapp.utilities.CommonFunctions.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File

class FolderImagesActivity : AppCompatActivity(), ImageClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewPager: ViewPager
    private lateinit var backBtn: ImageView
    private lateinit var closeBtn: ImageView
    private lateinit var itemSelected: TextView
    private lateinit var setTextAccToData: TextView
    private lateinit var albumFolderSize: TextView
    private lateinit var selectAll: TextView
    private lateinit var deSelectAll: TextView
    private var checkBoxList: ArrayList<MediaModel> = ArrayList()
    private var newList: ArrayList<MediaModel> = ArrayList()
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
    var position: Int = 0
    private var updated: Boolean = false

    companion object {
        lateinit var adapter: ImagesAdapter
        lateinit var bottomNavigationView: BottomNavigationView
        lateinit var select_top_menu_bar: LinearLayout
        lateinit var unselect_top_menu_bar: LinearLayout

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 11 && resultCode == Activity.RESULT_OK) {
            if (intent.hasExtra("folderPosition")) {
                val pathsToRemove = checkBoxList.map { it.path }
                removeItemsAtPosition(position, pathsToRemove)
                adapter.notifyDataSetChanged()
                updated = true
            }
        } else if (requestCode == REQ_CODE_FOR_TRASH_PERMISSION_IN_FOLDER_ACTIVITY && resultCode == Activity.RESULT_OK) {
            updated = true
            val pathsToRemove = checkBoxList.map { it.path }
            removeItemsAtPosition(position, pathsToRemove)
            adapter.notifyDataSetChanged()
            setAllVisibility()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_images)

        viewPager = findViewById(R.id.viewPager_slider_album)
        recyclerView = findViewById(R.id.recycler_view_album_activity)
        backBtn = findViewById(R.id.back_btn_album)
        closeBtn = findViewById(R.id.close_btn_album)
        setTextAccToData = findViewById(R.id.open_text_view_album)
        bottomNavigationView = findViewById(R.id.bottomNavigation_folder_images)
        itemSelected = findViewById(R.id.on_item_select)
        albumFolderSize = findViewById(R.id.album_folder_size)
        select_top_menu_bar = findViewById(R.id.select_top_menu_bar)
        unselect_top_menu_bar = findViewById(R.id.unselect_top_menu_bar)

        selectAll = findViewById(R.id.folder_selectAll_textView)
        deSelectAll = findViewById(R.id.folder_DeselectAll_textView)

        backBtn.setOnClickListener {
            if (updated) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
                updated = false
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        closeBtn.setOnClickListener {
            setAllVisibility()
        }

        if (intent.hasExtra("folderName")) {
            val name = intent.getStringExtra("folderName")
            setTextAccToData.text = name
        }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.shareItem -> handleShareAction()
                R.id.favoriteItem -> handleFavoriteAction()
                R.id.deleteItem -> handleDeleteAction()
                R.id.moreItem -> handleMoreAction()
            }
            true
        }


        selectAll.setOnClickListener {

            adapter.isSelected = true
            adapter.updateSelectionState(true)
            adapter.checkSelectedList.clear()
            adapter.checkSelectedList.addAll(newList)
            counter(adapter.checkSelectedList.size)

            selectAll.visibility = View.GONE
            deSelectAll.visibility = View.VISIBLE
        }


        deSelectAll.setOnClickListener {

            adapter.isSelected = false
            adapter.updateSelectionState(false)
            adapter.checkSelectedList.clear()
            adapter.checkSelectedList.removeAll(newList)
            counter(adapter.checkSelectedList.size)

            deSelectAll.visibility = View.GONE
            selectAll.visibility = View.VISIBLE
        }

        // IF USER SLIDE THE SCREEN ON FOLDER THEN THE BELOW CODE GIVES THE FOLDER LIST OF IMAGES
        if (intent.hasExtra("folderPosition")) {

            position = intent.getIntExtra("folderPosition", 0)
            if (position >= 0 && position < (application as AppClass).mainViewModel.folderList.size) {

                newList = (application as AppClass).mainViewModel.folderList[position].models
                albumFolderSize.text = newList.size.toString()
                if (newList.isNotEmpty()) {
                    loadRecyclerViewNew(newList, position)
                }
            }
        }
        //  end
    }

    private fun handleShareAction() {
        checkBoxList.clear()
        checkBoxList.addAll(adapter.checkSelectedList)
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
            showToast(this@FolderImagesActivity, "No images selected to share")
        }
    }

    private fun handleFavoriteAction() {
        checkBoxList.clear()
        checkBoxList.addAll(adapter.checkSelectedList)

        if (checkBoxList.isEmpty()) {
            showToast(this, "No images selected to add favorites!!")
        } else {
            for (addFavorite in checkBoxList) {
                ImagesDatabase.getDatabase(this@FolderImagesActivity).favoriteImageDao()
                    .insertFavorite(addFavorite)
            }
            showToast(this, "Favorite Added")
            setAllVisibility()
        }
    }

    private fun handleDeleteAction() {

        checkBoxList.clear()
        checkBoxList.addAll(adapter.checkSelectedList)
//        val imageToDelete = models.getOrNull(currentPosition)?.path

        val paths = checkBoxList.map { it.path }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val arrayList: ArrayList<Uri> = ArrayList()
            MediaScannerConnection.scanFile(this, paths.toTypedArray(), null) { _, uri ->
                arrayList.add(uri)
                if (arrayList.size == paths.size) {
                    val pendingIntent: PendingIntent =
                        MediaStore.createTrashRequest(contentResolver, arrayList, true)
                    startIntentSenderForResult(
                        pendingIntent.intentSender,
                        REQ_CODE_FOR_TRASH_PERMISSION_IN_FOLDER_ACTIVITY,
                        null,
                        0,
                        0,
                        0
                    )
                }
            }
            setAllVisibility()
        } else {
            if (paths.isNotEmpty()) {
//                val numImagesToDelete = paths.size
                val pathsToRemove = checkBoxList.map { it.path }

                showPopupForMoveToTrashBin(
                    bottomNavigationView, paths, this@FolderImagesActivity, pathsToRemove, position
                )

//                setAllVisibility()

//                removeItemsAtPosition(position, pathsToRemove)
//                adapter.notifyDataSetChanged()


//                androidx.appcompat.app.AlertDialog.Builder(this)
//                    .setTitle("Delete $numImagesToDelete Item?")
//                    .setMessage("Are you sure to move $numImagesToDelete files to the trash bin?")
//                    .setPositiveButton("Delete") { _, _ ->
//                        // User clicked "Yes", proceed with deletion
//                        // HERE I AM DELETING THE IMAGE WITH CURRENT PATH
//                        (application as AppClass).mainViewModel.moveMultipleImagesInTrashBin(paths)
//
//
//                        setAllVisibility()
//
//                        val pathsToRemove = checkBoxList.map { it.path }
//                        removeItemsAtPosition(position, pathsToRemove)
//                        adapter.notifyDataSetChanged()
//
//                    }.setNegativeButton("Cancel") { dialog, _ ->
//                        // User clicked "No", do nothing
//                        dialog.dismiss()
//                    }.show()
            } else {
                showToast(this@FolderImagesActivity, "Error: Image not found")
            }
        }
    }

    private fun handleMoreAction() {

    }

//    private fun setBottomNavigationItem(itemId: Int) {
//        when (itemId) {
//            R.id.shareItem -> {
//                checkBoxList = this.adapter.checkSelectedList
//
//                val selectedImages = checkBoxList.map { it.path }
//
//                if (selectedImages.isNotEmpty()) {
//                    val uris = ArrayList<Uri>()
//
//                    // Convert file paths to Uri using FileProvider
//                    for (file in selectedImages) {
//                        val uri = FileProvider.getUriForFile(
//                            this, "com.demo.newgalleryapp.fileprovider", File(file)
//                        )
//                        uris.add(uri)
//                    }
//                    // Handle share item
//                    (application as AppClass).mainViewModel.shareMultipleImages(uris, this)
//                } else {
//                    Toast.makeText(this, "No images selected to share", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            R.id.deleteItem -> {
//
//                checkBoxList = this.adapter.checkSelectedList
////                val imageToDelete = models.getOrNull(currentPosition)?.path
//
//                val paths = checkBoxList.map {
//                    it.path
//                }
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    val arrayList: ArrayList<Uri> = ArrayList()
//                    MediaScannerConnection.scanFile(this, paths.toTypedArray(), null) { _, uri ->
//                        arrayList.add(uri)
//                        if (arrayList.size == paths.size) {
//                            val pendingIntent: PendingIntent =
//                                MediaStore.createTrashRequest(contentResolver, arrayList, true)
//                            startIntentSenderForResult(
//                                pendingIntent.intentSender, 123, null, 0, 0, 0
//                            )
//                        }
//                    }
////                    backBtn.visibility = View.VISIBLE
////                    setTextAccToData.visibility = View.VISIBLE
////                    bottomNavigationView.visibility = View.GONE
////                    closeBtn.visibility = View.GONE
////                    itemSelected.visibility = View.GONE
//                } else {
//                    if (paths.isNotEmpty()) {
//                        val numImagesToDelete = paths.size
//
//                        androidx.appcompat.app.AlertDialog.Builder(this)
//                            .setTitle("Delete $numImagesToDelete Item?")
//                            .setMessage("Are you sure to move $numImagesToDelete files to the trash bin?")
//                            .setPositiveButton("Delete") { _, _ ->
//                                // User clicked "Yes", proceed with deletion
//                                // HERE I AM DELETING THE IMAGE WITH CURRENT PATH
//                                (application as AppClass).mainViewModel.moveMultipleImagesInTrashBin(
//                                    paths
//                                )
//                                backBtn.visibility = View.VISIBLE
//                                setTextAccToData.visibility = View.VISIBLE
//                                bottomNavigationView.visibility = View.GONE
////                                closeBtn.visibility = View.GONE
//                                itemSelected.visibility = View.GONE
//
//                            }.setNegativeButton("Cancel") { dialog, _ ->
//                                // User clicked "No", do nothing
//                                dialog.dismiss()
//                            }.show()
//                    } else {
//                        Toast.makeText(this, "Error: Image not found", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }

    private fun loadRecyclerViewNew(list: ArrayList<MediaModel>, position: Int) {
        val screenWidth = resources.displayMetrics.widthPixels
        sharedPreferencesHelper = SharedPreferencesHelper(this)

        // Calculate the height of images according to the number of columns
        val imageViewWidth = screenWidth / sharedPreferencesHelper.getGridColumns()

        // Set layout parameters for the RecyclerView items
        RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageViewWidth)

        // Create a GridLayoutManager with the specified number of columns
        val gridLayoutManager = GridLayoutManager(
            this, sharedPreferencesHelper.getGridColumns(), LinearLayoutManager.VERTICAL, false
        )

        // Apply layout parameters and set the layout manager for the RecyclerView
//        recyclerView.layoutParams = layoutParams
        recyclerView.layoutManager = gridLayoutManager

        // Create and set the adapter for the RecyclerView
        adapter = ImagesAdapter(this, list, position, this@FolderImagesActivity)
        recyclerView.adapter = adapter

        // Ensure RecyclerView is visible
        recyclerView.visibility = View.VISIBLE
    }


    override fun onLongClick() {
        bottomNavigationView.visibility = View.VISIBLE
        unselect_top_menu_bar.visibility = View.VISIBLE
        select_top_menu_bar.visibility = View.GONE
    }

    override fun onBackPressed() {
        if (updated) {
            val intent = Intent()
            setResult(Activity.RESULT_OK, intent)
            updated = false
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onBackPressed()
    }

    override fun counter(select: Int) {
        if (select == 0) {
            adapter.updateSelectionState(false)
            setAllVisibility()
        }
        itemSelected.text = "Item Selected $select"
    }

    private fun removeItemsAtPosition(position: Int, pathsToRemove: List<String>) {

        val tempFolderList = (application as AppClass).mainViewModel.folderList
        if (position >= 0 && position < tempFolderList.size) {

            val folder = (application as AppClass).mainViewModel.folderList[position]
            val updatedList = folder.models.filterNot { pathsToRemove.contains(it.path) }
            val updatedFolder = Folder(folder.models[position].path, ArrayList(updatedList))

            // Update the mainViewModel.folderList
            (application as AppClass).mainViewModel.folderList[position] = updatedFolder

            // Notify the adapter about the data change
            adapter.updateList(updatedList)
        }
    }

    private fun setAllVisibility() {
        adapter.updateSelectionState(false)
        bottomNavigationView.visibility = View.GONE
        unselect_top_menu_bar.visibility = View.GONE
        select_top_menu_bar.visibility = View.VISIBLE
    }
}
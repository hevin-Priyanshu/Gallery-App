package com.demo.newgalleryapp.utilities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.CopyOrMoveActivity
import com.demo.newgalleryapp.activities.FavoriteImagesActivity
import com.demo.newgalleryapp.activities.FolderImagesActivity
import com.demo.newgalleryapp.activities.FolderImagesActivity.Companion.isUpdatedFolderActivity
import com.demo.newgalleryapp.activities.MainScreenActivity
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.bottomNavigationView
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.bottomNavigationViewForLongSelect
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.photosFragment
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.videosFragment
import com.demo.newgalleryapp.activities.OpenImageActivity
import com.demo.newgalleryapp.activities.OpenTrashImageActivity.Companion.imagesSliderAdapter
import com.demo.newgalleryapp.activities.TrashBinActivity
import com.demo.newgalleryapp.activities.TrashBinActivity.Companion.trashBinAdapter
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.fragments.MediaFragment.Companion.linearLayoutForMainText
import com.demo.newgalleryapp.fragments.MediaFragment.Companion.linearLayoutForSelectText
import com.demo.newgalleryapp.fragments.MediaFragment.Companion.viewPager
import com.demo.newgalleryapp.models.Folder
import com.demo.newgalleryapp.models.TrashBinAboveVersion
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CommonFunctions {
    private var popupWindow: PopupWindow? = null
    private var popupWindow_delete: PopupWindow? = null
    private var popupWindow_restore: PopupWindow? = null
    private var popupWindow_restore_trash: PopupWindow? = null
    private var popupForDeletePermanently: PopupWindow? = null
    private var popupWindowMore: PopupWindow? = null

    const val REQ_CODE_FOR_PERMISSION = 100
    const val REQ_CODE_FOR_WRITE_PERMISSION_IN_COPY_MOVE_ACTIVITY = 101
    const val REQ_CODE_FOR_TRASH_PERMISSION_IN_MAIN_SCREEN_ACTIVITY = 102
    const val REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_IMAGE_ACTIVITY = 103
    const val REQ_CODE_FOR_TRASH_PERMISSION_IN_OPEN_TRASH_ACTIVITY = 104
    const val REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_TRASH_ACTIVITY = 105
    const val REQ_CODE_FOR_RESTORE_PERMISSION_IN_TRASH_BIN_ACTIVITY = 106
    const val REQ_CODE_FOR_DELETE_PERMISSION_IN_TRASH_BIN_ACTIVITY = 107
    const val REQ_CODE_FOR_TRASH_PERMISSION_IN_FOLDER_ACTIVITY = 108
    const val REQ_CODE_FOR_UPDATES_IN_OPEN_IMAGE_ACTIVITY = 109
    const val REQ_CODE_FOR_CHANGES_IN_TRASH_ACTIVITY = 110
    const val REQ_CODE_FOR_CHANGES_IN_OPEN_TRASH_ACTIVITY = 111
    const val REQ_CODE_FOR_CHANGES_IN_OPEN_IMAGE_ACTIVITY = 112
    const val REQ_CODE_FOR_CHANGES_IN_EDIT_ACTIVITY = 113
    const val REQ_CODE_FOR_CHANGES_IN_MAIN_SCREEN_ACTIVITY = 114
    const val REQ_CODE_FOR_WRITE_PERMISSION_IN_OPEN_IMAGE_ACTIVITY = 115
    const val REQ_CODE_FOR_CHANGES_IN_FOLDER_ACTIVITY = 116

    const val ERROR_TAG = "Error"
    var FLAG_FOR_CHANGES_IN_RENAME: Boolean = false
//    var FLAG_FOR_MAIN_SCREEN_ACTIVITY: Boolean = false
//    var FLAG_IN_MAIN_SCREEN_ACTIVITY: Int = 0

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun logMessage(tag: String, message: String) {
        Log.d(tag, message)
    }

//    fun formatDate(dateAdded: Long): String {
//        val dateAddedInSeconds = dateAdded ?: 0L
//        val dateAddedInMillis = dateAddedInSeconds * 1000
//        val date = Date(dateAddedInMillis)
//        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
//        return dateFormat.format(date)
//    }

    fun formatDate(dateAdded: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateAdded * 1000 // Convert seconds to milliseconds
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun formatTime(timeAdded: Long): String {
        val dateAddedInSeconds = timeAdded ?: 0L
        val dateAddedInMillis = dateAddedInSeconds * 1000
        val date = Date(dateAddedInMillis)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return timeFormat.format(date)
    }

    fun formatSize(sizeInBytes: Long): String {
        val kiloBytes = sizeInBytes / 1024.0
        val megaBytes = kiloBytes / 1024.0
        val gigaBytes = megaBytes / 1024.0

        return when {
            gigaBytes >= 1.0 -> String.format("%.2f GB", gigaBytes)
            megaBytes >= 1.0 -> String.format("%.2f MB", megaBytes)
            kiloBytes >= 1.0 -> String.format("%.2f KB", kiloBytes)
            else -> String.format("%d B", sizeInBytes)
        }
    }


    fun Activity.showPopupForMainScreenMoreItem(
        anchorView: View, paths: List<String>
    ) {

        val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupWindow_for_main_screen_more: View =
            inflater.inflate(R.layout.popup_more_item_main_screen_activity, null)

        popupWindowMore = PopupWindow(
            popupWindow_for_main_screen_more,
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.MATCH_PARENT,
            true
        )

        popupWindowMore?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val copyBtn = popupWindow_for_main_screen_more.findViewById<TextView>(R.id.copy_more_item)
        val moveBtn = popupWindow_for_main_screen_more.findViewById<TextView>(R.id.move_more_item)
//        val slideShowBtn =
//            popupWindow_for_main_screen_more.findViewById<TextView>(R.id.slide_show_more_item)


        copyBtn.setOnClickListener {
            val intent = Intent(this, CopyOrMoveActivity::class.java)
            intent.putExtra("pathsList", ArrayList(paths))
            intent.putExtra("copySelectedMainScreenActivity", true)
            intent.putExtra("mainScreenActivity", true)
            startActivityForResult(intent, REQ_CODE_FOR_CHANGES_IN_MAIN_SCREEN_ACTIVITY)
            popupWindowMore?.dismiss()
            resetVisibilityForDeleteItem()
        }

        moveBtn.setOnClickListener {
            val intent = Intent(this, CopyOrMoveActivity::class.java)
            intent.putExtra("pathsList", ArrayList(paths))
            intent.putExtra("moveSelectedMainScreenActivity", true)
            intent.putExtra("mainScreenActivity", true)
            startActivityForResult(intent, REQ_CODE_FOR_CHANGES_IN_MAIN_SCREEN_ACTIVITY)
            popupWindowMore?.dismiss()
            resetVisibilityForDeleteItem()
        }


//        slideShowBtn.setOnClickListener {
//            val intent = Intent(this, SlideShowActivity::class.java)
//            intent.putExtra("pathsList", ArrayList(paths))
//            intent.putExtra("slideShowSelectedMainScreenActivity", true)
//            startActivityForResult(intent, REQ_CODE_FOR_CHANGES_IN_MAIN_SCREEN_ACTIVITY)
//            popupWindow?.dismiss()
//        }

        val popupItem =
            popupWindow_for_main_screen_more.findViewById<LinearLayout>(R.id.popupItem_more_main_screen_activity)

        popupItem.setOnClickListener {
            popupWindowMore?.dismiss()
        }
        // Set dismiss listener to nullify the reference
        popupWindowMore?.setOnDismissListener {
            popupWindowMore = null
        }
    }

    fun Context.showRenamePopup(
        anchorView: View, selectedImagePath: String, activity: OpenImageActivity
    ) {

        val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.rename_popup_menu, null)

        popupWindow = PopupWindow(
            popupView, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT, true
        )

        popupWindow?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val saveBtn = popupView.findViewById<TextView>(R.id.rename_save)
        val cancelBtn = popupView.findViewById<TextView>(R.id.rename_chancel)
        val searchEditText = popupView.findViewById<EditText>(R.id.rename_searchEditText)


        saveBtn.setOnClickListener {

            val newName = searchEditText.text.toString().trim()
            val directory = File(selectedImagePath).parent
            val originalPath = File(selectedImagePath)
            val lastText = getImageFileExtension(selectedImagePath)
            val destinationPath = File(directory, "$newName.$lastText")

            if (newName.isNotEmpty()) {

                try {
                    originalPath.renameTo(destinationPath)
                    (activity.application as AppClass).mainViewModel.scanFile(this, destinationPath)
                    (activity.application as AppClass).mainViewModel.scanFile(this, originalPath)
                    FLAG_FOR_CHANGES_IN_RENAME = true
                    showToast(this, "Rename Successful!!")
                } catch (e: IOException) {
                    Log.e("CopyOrMoveActivity", "Error creating write request", e)
                }
                popupWindow?.dismiss()
            } else {
                Toast.makeText(this, "Please enter a valid name.", Toast.LENGTH_SHORT).show()
            }
        }

        cancelBtn.setOnClickListener {
            popupWindow?.dismiss()
        }

        val popupItem = popupView.findViewById<LinearLayout>(R.id.popupItem_rename)

        popupItem.setOnClickListener {
            popupWindow?.dismiss()
        }
        // Set dismiss listener to nullify the reference
        popupWindow?.setOnDismissListener {
            popupWindow = null
        }
    }

    // here move multiple files in trash bin,  popup menu will show
    fun Context.showPopupForMoveToTrashBin(
        anchorView: View,
        paths: List<String>,
        activity: Activity,
        pathsToRemove: List<String>,
        position: Int
    ) {

        val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupWindowDelete: View = inflater.inflate(R.layout.delete_popup_menu, null)

        popupWindow_delete = PopupWindow(
            popupWindowDelete,
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.MATCH_PARENT,
            true
        )

        popupWindow_delete?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val saveBtn = popupWindowDelete.findViewById<TextView>(R.id.save_column_for_delete)
        val cancelBtn = popupWindowDelete.findViewById<TextView>(R.id.cancel_column_for_delete)
        val howManyItems =
            popupWindowDelete.findViewById<TextView>(R.id.setHowManyImagesWantToDeleteText)
        val mainDeleteText = popupWindowDelete.findViewById<TextView>(R.id.delete_main_text)

        mainDeleteText.text = "${"Delete ${paths.size} Item ?"}"
        howManyItems.text = "${"Are you sure to move ${paths.size} file to the trash bin ?"}"

        saveBtn.setOnClickListener {
            (applicationContext as AppClass).mainViewModel.moveMultipleImagesInTrashBin(paths)

            when (activity) {
                is FolderImagesActivity -> {
                    activity.removeItemsAtPosition(position, pathsToRemove)
//                    removeItemsAtPosition(position, pathsToRemove)
                    FolderImagesActivity.adapter.notifyDataSetChanged()
                    isUpdatedFolderActivity = true
                    setAllVisibilityFolderImagesActivity()
                    popupWindow_delete?.dismiss()
                }

                is FavoriteImagesActivity -> {
                    setAllVisibilityFavoriteImagesActivity()
                    FavoriteImagesActivity.favoriteAdapter.notifyDataSetChanged()
                    popupWindow_delete?.dismiss()
                }

                is MainScreenActivity -> {
                    if (viewPager.currentItem == 0) {
                        photosFragment.imagesAdapter?.removeItemsFromAdapter(paths)
                    } else {
                        videosFragment.imagesAdapter?.removeItemsFromAdapter(paths)
                    }
                    resetVisibilityForDeleteItem()
                }
            }

            popupWindow_delete?.dismiss()
        }

        cancelBtn.setOnClickListener {

            when (activity) {
                is FolderImagesActivity -> {
                    setAllVisibilityFolderImagesActivity()
                }

                is FavoriteImagesActivity -> {
                    setAllVisibilityFavoriteImagesActivity()
                }

                is MainScreenActivity -> {
                    resetVisibilityForDeleteItem()
                }
            }
            popupWindow_delete?.dismiss()
        }

    }


//    private fun Context.removeItemsAtPosition(position: Int, pathsToRemove: List<String>) {
//        if (position >= 0 && position < (applicationContext as AppClass).mainViewModel.folderList.size) {
//
//            val folder = (applicationContext as AppClass).mainViewModel.folderList[position]
//            val updatedList = folder.models.filterNot { pathsToRemove.contains(it.path) }
//            val updatedFolder = Folder(folder.models[position].path, ArrayList(updatedList))
//
//            // Update the mainViewModel.folderList
//            (applicationContext as AppClass).mainViewModel.folderList[position] = updatedFolder
//
//            // Notify the adapter about the data change
//            FolderImagesActivity.adapter.updateList(updatedList)
//        }
//    }

    // here move only one file in trash bin,  popup menu will show
    fun Context.showPopupForMoveToTrashBinForOpenActivityOnlyOne(
        anchorView: View, path: String, currentPosition: Int, isVideoOrNot: Boolean
    ) {
        val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupWindowDelete: View = inflater.inflate(R.layout.delete_popup_menu, null)

        popupWindow_delete = PopupWindow(
            popupWindowDelete,
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.MATCH_PARENT,
            true
        )

        popupWindow_delete?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val saveBtn = popupWindowDelete.findViewById<TextView>(R.id.save_column_for_delete)
        val cancelBtn = popupWindowDelete.findViewById<TextView>(R.id.cancel_column_for_delete)
        val howManyItems =
            popupWindowDelete.findViewById<TextView>(R.id.setHowManyImagesWantToDeleteText)
        val mainDeleteText = popupWindowDelete.findViewById<TextView>(R.id.delete_main_text)

        mainDeleteText.text = "${"Delete 1 Item ?"}"
        howManyItems.text = "${"Are you sure to move 1 file to the trash bin ?"}"

        saveBtn.setOnClickListener {
            (applicationContext as AppClass).mainViewModel.flag = true
            // HERE I AM DELETING THE IMAGE WITH CURRENT PATH
            (applicationContext as AppClass).mainViewModel.moveImageInTrashBin(path, isVideoOrNot)

            OpenImageActivity.imagesSliderAdapter.remove(currentPosition)
            popupWindow_delete?.dismiss()
            showToast(this, "Move To Trash bin Successfully!!")
        }

        cancelBtn.setOnClickListener {
            popupWindow_delete?.dismiss()
        }
    }

    // here restore multiple popup menu will show
    fun Context.showPopupRestoreMultiple(
        anchorView: View, selectedItemList: ArrayList<TrashBinAboveVersion>
    ) {

        val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupWindowRestoreTrash: View = inflater.inflate(R.layout.restore_popup_menu, null)

        popupWindow_restore_trash = PopupWindow(
            popupWindowRestoreTrash,
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.MATCH_PARENT,
            true
        )

        popupWindow_restore_trash?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val restoreBtn = popupWindowRestoreTrash.findViewById<TextView>(R.id.restore_btn)
        val cancelBtn = popupWindowRestoreTrash.findViewById<TextView>(R.id.restore_cancel_btn)

        val mainRestoreText = popupWindowRestoreTrash.findViewById<TextView>(R.id.restore_main_text)
        val howManyItems =
            popupWindowRestoreTrash.findViewById<TextView>(R.id.setHowManyImagesWantToRestoreText)

        mainRestoreText.text = "${"Restore ${selectedItemList.size} Items ?"}"
        howManyItems.text =
            "${"Are you sure to restore ${selectedItemList.size} files from trash bin ?"}"

        restoreBtn.setOnClickListener {
//            (anchorView.context.applicationContext as AppClass).mainViewModel.restoreMultipleImagesVideos(selectedItemList)

            (applicationContext as AppClass).mainViewModel.restoreMultipleImagesVideos(
                selectedItemList
            )
            (applicationContext as AppClass).mainViewModel.flagForTrashBinActivity = true
            showToast(this, "Restore Successful!!")
            popupWindow_restore_trash?.dismiss()
            trashBinActivityAllVisibility()
        }

        cancelBtn.setOnClickListener {
            popupWindow_restore_trash?.dismiss()
            trashBinActivityAllVisibility()
        }
    }

    fun Activity.showPopupRestoreOne(
        anchorView: View, paths: TrashBinAboveVersion, currentPosition: Int
    ) {

        val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupWindowRestoreOne: View = inflater.inflate(R.layout.restore_popup_menu, null)

        popupWindow_restore = PopupWindow(
            popupWindowRestoreOne,
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.MATCH_PARENT,
            true
        )

        popupWindow_restore?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val restoreBtn = popupWindowRestoreOne.findViewById<TextView>(R.id.restore_btn)
        val cancelBtn = popupWindowRestoreOne.findViewById<TextView>(R.id.restore_cancel_btn)

        val mainRestoreText = popupWindowRestoreOne.findViewById<TextView>(R.id.restore_main_text)
        val howManyItems =
            popupWindowRestoreOne.findViewById<TextView>(R.id.setHowManyImagesWantToRestoreText)

        mainRestoreText.text = "${"Restore ${1} Items ?"}"
        howManyItems.text = "${"Are you sure to restore ${1} file from trash bin ?"}"

        restoreBtn.setOnClickListener {
            (applicationContext as AppClass).mainViewModel.restoreImage(paths)
            popupWindow_restore?.dismiss()
            imagesSliderAdapter.remove(currentPosition)

            showToast(this, "Image restored successfully!!")
        }

        cancelBtn.setOnClickListener {
            popupWindow_restore?.dismiss()
        }
    }

    // here DeletePermanently multiple popup menu will show
    fun Context.showPopupForDeletePermanently(
        anchorView: View,
        deletePaths: ArrayList<TrashBinAboveVersion>,
    ) {
        val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupWindowDeletePermanently: View = inflater.inflate(R.layout.delete_popup_menu, null)

        popupForDeletePermanently = PopupWindow(
            popupWindowDeletePermanently,
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.MATCH_PARENT,
            true
        )

        popupForDeletePermanently?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val saveBtn =
            popupWindowDeletePermanently.findViewById<TextView>(R.id.save_column_for_delete)
        val cancelBtn =
            popupWindowDeletePermanently.findViewById<TextView>(R.id.cancel_column_for_delete)
        val howManyItems =
            popupWindowDeletePermanently.findViewById<TextView>(R.id.setHowManyImagesWantToDeleteText)
        val mainDeleteText =
            popupWindowDeletePermanently.findViewById<TextView>(R.id.delete_main_text)

        mainDeleteText.text = "${"Delete ${deletePaths.size} Item ?"}"
        howManyItems.text = "${"Are you sure to delete ${deletePaths.size} files Permanently ?"}"

        saveBtn.setOnClickListener {
            (applicationContext as AppClass).mainViewModel.deleteMultiple(deletePaths)
            popupForDeletePermanently?.dismiss()
            trashBinActivityAllVisibility()
        }

        cancelBtn.setOnClickListener {
            popupForDeletePermanently?.dismiss()
            trashBinActivityAllVisibility()
        }
    }

    // here DeletePermanently one only items popup menu will show
    fun Context.showPopupForDeletePermanentlyForOne(
        anchorView: View, paths: TrashBinAboveVersion, currentPosition: Int
    ) {

        val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupWindowDeletePermanentlyOne: View =
            inflater.inflate(R.layout.delete_popup_menu, null)

        popupForDeletePermanently = PopupWindow(
            popupWindowDeletePermanentlyOne,
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.MATCH_PARENT,
            true
        )

        popupForDeletePermanently?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val saveBtn =
            popupWindowDeletePermanentlyOne.findViewById<TextView>(R.id.save_column_for_delete)
        val cancelBtn =
            popupWindowDeletePermanentlyOne.findViewById<TextView>(R.id.cancel_column_for_delete)
        val howManyItems =
            popupWindowDeletePermanentlyOne.findViewById<TextView>(R.id.setHowManyImagesWantToDeleteText)
        val mainDeleteText =
            popupWindowDeletePermanentlyOne.findViewById<TextView>(R.id.delete_main_text)

        mainDeleteText.text = "${"Delete ${1} Item ?"}"
        howManyItems.text = "${"Are you sure to delete ${1} file Permanently ?"}"

        saveBtn.setOnClickListener {
            (applicationContext as AppClass).mainViewModel.deleteImage(paths)
            popupForDeletePermanently?.dismiss()
            imagesSliderAdapter.remove(currentPosition)
        }

        cancelBtn.setOnClickListener {
            popupForDeletePermanently?.dismiss()
        }
    }

    private fun getImageFileExtension(fileName: String): String {
        return if (fileName.contains(".")) {
            fileName.substringAfterLast(".", "")
        } else {
            ""
        }
    }

    fun resetVisibilityForDeleteItem() {
        photosFragment.imagesAdapter?.updateSelectionState(false)
        videosFragment.imagesAdapter?.updateSelectionState(false)
        bottomNavigationViewForLongSelect.visibility = View.GONE
        linearLayoutForSelectText.visibility = View.GONE
        bottomNavigationView.visibility = View.VISIBLE
        linearLayoutForMainText.visibility = View.VISIBLE
    }

    private fun setAllVisibilityFolderImagesActivity() {
        FolderImagesActivity.adapter.updateSelectionState(false)
        FolderImagesActivity.bottomNavigationView.visibility = View.GONE
        FolderImagesActivity.unselect_top_menu_bar.visibility = View.GONE
        FolderImagesActivity.select_top_menu_bar.visibility = View.VISIBLE
    }

    private fun setAllVisibilityFavoriteImagesActivity() {
        FavoriteImagesActivity.favoriteAdapter.updateSelectionState(false)
        FavoriteImagesActivity.favoriteBottomNavigationView.visibility = View.GONE
        FavoriteImagesActivity.mainLinearLayout.visibility = View.VISIBLE
        FavoriteImagesActivity.selectedTextViewLinearLayout.visibility = View.GONE
    }

    private fun trashBinActivityAllVisibility() {
        trashBinAdapter.updateSelectionState(false)
        TrashBinActivity.bottomNavigationView.visibility = View.GONE
        TrashBinActivity.linearLayoutForMainText.visibility = View.VISIBLE
        TrashBinActivity.linearLayoutForSelectText.visibility = View.GONE
    }

    fun Context.showAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts(
            "package", packageName, null
        )   // Create a Uri specifying the package details for the current app.

        intent.data =
            uri  // Set the Uri as data for the Intent. This specifies the details of the app whose settings should be shown.
        startActivity(intent)
//        finish()
    }

    fun setNavigationColor(window: Window, color: Int) {
        window.apply {
            navigationBarColor = color
            statusBarColor = color
            val decorView = window.decorView
            decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE /*or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN*/)
        }
    }

    ////////////////////
}
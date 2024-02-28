package com.demo.newgalleryapp.utilities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
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
import com.demo.newgalleryapp.activities.OpenTrashImageActivity
import com.demo.newgalleryapp.activities.OpenTrashImageActivity.Companion.handler
import com.demo.newgalleryapp.activities.OpenTrashImageActivity.Companion.imagesSliderAdapterTrash
import com.demo.newgalleryapp.activities.OpenTrashImageActivity.Companion.updated
import com.demo.newgalleryapp.activities.TrashBinActivity
import com.demo.newgalleryapp.activities.TrashBinActivity.Companion.trashBinAdapter
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.fragments.MediaFragment.Companion.linearLayoutForMainText
import com.demo.newgalleryapp.fragments.MediaFragment.Companion.linearLayoutForSelectText
import com.demo.newgalleryapp.fragments.MediaFragment.Companion.viewPager
import com.demo.newgalleryapp.models.MediaModel
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
    var isMainSelection: Boolean = false

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
    const val REQ_CODE = 117
    var positionForItem = -1

    const val ERROR_TAG = "Error"
    var FLAG_FOR_CHANGES_IN_RENAME: Boolean = false
    var isAutoSlidingEnabled: Boolean = false


    fun View.gone() {
        visibility = View.GONE
    }

    fun View.visible() {
        visibility = View.VISIBLE
    }

    fun View.inVisible() {
        visibility = View.INVISIBLE
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun logMessage(tag: String, message: String) {
        Log.d(tag, message)
    }

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
        anchorView: View, paths: List<String>, activity: Activity
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
//            if (activity is FolderImagesActivity) {
//                activity.finish()
//            }
            resetVisibilityForDeleteItem()
        }


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
        anchorView: View,
        selectedImagePath: String,
        activity: OpenImageActivity
    ) {

        val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.rename_popup_menu, null)

        popupWindow = PopupWindow(
            popupView, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT, true
        )

        popupWindow?.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

        popupWindow?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val saveBtn = popupView.findViewById<TextView>(R.id.rename_save)
        val cancelBtn = popupView.findViewById<TextView>(R.id.rename_chancel)
        val searchEditText = popupView.findViewById<EditText>(R.id.rename_searchEditText)

        val imageName = File(selectedImagePath).name
        val name = imageName.substringBeforeLast(".")
        searchEditText.setText(name)

        saveBtn.setOnClickListener {

            activity.progressDialogFragment.show()

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

                    Handler().postDelayed(Runnable {
                        activity.progressDialogFragment.cancel()
                        showToast(this, "Rename Successfully!!")
                        activity.finish()
                    }, 1000)

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

//        val popupItem = popupView.findViewById<LinearLayout>(R.id.popupItem_rename)
//
//        popupItem.setOnClickListener {
//            popupWindow?.dismiss()
//        }
//        // Set dismiss listener to nullify the reference
//        popupWindow?.setOnDismissListener {
//            popupWindow = null
//        }
    }

    // here move multiple files in trash bin,  popup menu will show
    fun Context.showPopupForMoveToTrashBin(
        anchorView: View,
        paths: List<String>,
        activity: Activity,
        pathsToRemove: List<String>,
        position: Int,
        isVideos: List<Boolean>
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
            (applicationContext as AppClass).mainViewModel.moveMultipleImagesInTrashBin(
                paths, isVideos
            )

            when (activity) {
                is FolderImagesActivity -> {
                    activity.progressDialogFragment.show()
                    activity.removeItemsAtPosition(position, pathsToRemove)
//                    removeItemsAtPosition(position, pathsToRemove)
                    FolderImagesActivity.adapter.notifyDataSetChanged()
                    isUpdatedFolderActivity = true
                    setAllVisibilityFolderImagesActivity()
                    activity.setHowManyItem()
                    popupWindow_delete?.dismiss()
                }

                is FavoriteImagesActivity -> {
                    setAllVisibilityFavoriteImagesActivity()
                    FavoriteImagesActivity.favoriteAdapter.notifyDataSetChanged()
                    popupWindow_delete?.dismiss()
                }

                is MainScreenActivity -> {
                    activity.progressDialogFragment.show()
                    if (viewPager.currentItem == 0) {
                        photosFragment.imagesAdapter?.removeItemsFromAdapter(paths)
                    } else {
                        videosFragment.imagesAdapter?.removeItemsFromAdapter(paths)
                    }
                    resetVisibilityForDeleteItem()

                    activity.handler?.postDelayed({
                        activity.progressDialogFragment.cancel()
                        (activity.application as AppClass).mainViewModel.getMediaFromInternalStorage()
                        showToast(this, "Deleted Successfully!!")
                    }, 1000)
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


    // here move only one file in trash bin,  popup menu will show
    fun Context.showPopupForMoveToTrashBinForOpenActivityOnlyOne(
        anchorView: View,
        path: String,
        currentPosition: Int,
        isVideoOrNot: Boolean,
        currentState: Int?,
        openImageActivity: OpenImageActivity
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
            // HERE I AM DELETING THE IMAGE WITH CURRENT PATH
            (applicationContext as AppClass).mainViewModel.moveImageInTrashBin(path, isVideoOrNot)


            if (currentState == 1) {
                OpenImageActivity.imagesSliderAdapter.remove(currentPosition, openImageActivity)
                OpenImageActivity.anyChanges = true
            } else {
                //FolderImagesActivity
                OpenImageActivity.imagesSliderAdapter.remove(currentPosition, openImageActivity)
                FolderImagesActivity.adapter.remove(currentPosition)

                isUpdatedFolderActivity = true
            }


            popupWindow_delete?.dismiss()
            (applicationContext as AppClass).mainViewModel.getMediaFromInternalStorage()
            showToast(this, "Move To Trash bin Successfully!!")
        }

        cancelBtn.setOnClickListener {
            popupWindow_delete?.dismiss()
        }
    }

    // here restore multiple popup menu will show
    fun Context.showPopupRestoreMultiple(
        anchorView: View,
        selectedItemList: ArrayList<TrashBinAboveVersion>,
        trashBinActivity: TrashBinActivity
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

            trashBinActivity.progressDialogFragment.show()

            (applicationContext as AppClass).mainViewModel.restoreMultipleImagesVideos(
                selectedItemList
            )
            (applicationContext as AppClass).mainViewModel.flagForTrashBinActivity = true

            trashBinActivity.handler?.postDelayed({
                showToast(this, "Restore Successfully!!")
                trashBinActivity.progressDialogFragment.cancel()
            }, 1000)

            popupWindow_restore_trash?.dismiss()
            trashBinActivityAllVisibility()
        }

        cancelBtn.setOnClickListener {
            popupWindow_restore_trash?.dismiss()
            trashBinActivityAllVisibility()
        }
    }

    fun Activity.showPopupRestoreOne(
        anchorView: View,
        paths: TrashBinAboveVersion,
        currentPosition: Int,
        openTrashImageActivity: OpenTrashImageActivity
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
            openTrashImageActivity.progressDialogFragment.show()

            updated = true
            imagesSliderAdapterTrash.remove(currentPosition, openTrashImageActivity)
//            trashBinAdapter.remove(currentPosition)

//            trashBinAdapter.notifyDataSetChanged()
//            openTrashImageActivity.imagesSliderAdapterTrash.notifyDataSetChanged()

            handler?.postDelayed({
                openTrashImageActivity.progressDialogFragment.cancel()
            }, 1000)
            popupWindow_restore?.dismiss()
            (application as AppClass).mainViewModel.getMediaFromInternalStorage()
            showToast(this, "Restore Successfully!!")
        }

        cancelBtn.setOnClickListener {
            popupWindow_restore?.dismiss()
        }
    }

    // here DeletePermanently multiple popup menu will show
    fun Context.showPopupForDeletePermanently(
        anchorView: View,
        deletePaths: ArrayList<TrashBinAboveVersion>,
        trashBinActivity: TrashBinActivity,
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

            trashBinActivity.progressDialogFragment.show()

            (applicationContext as AppClass).mainViewModel.deleteMultiple(deletePaths)

            trashBinActivity.handler?.postDelayed({
                trashBinActivity.progressDialogFragment.cancel()
                showToast(this, "Deleted Successfully!!")
            }, 1000)
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
        anchorView: View,
        paths: TrashBinAboveVersion,
        currentPosition: Int,
        openTrashImageActivity: OpenTrashImageActivity
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

            openTrashImageActivity.progressDialogFragment.show()

            imagesSliderAdapterTrash.remove(
                currentPosition, openTrashImageActivity
            )

            popupForDeletePermanently?.dismiss()

            trashBinAdapter.notifyDataSetChanged()
            imagesSliderAdapterTrash.notifyDataSetChanged()

            handler?.postDelayed({
                openTrashImageActivity.progressDialogFragment.cancel()
            }, 1000)
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
    }

    fun setNavigationColor(window: Window, color: Int) {
        window.apply {
            navigationBarColor = color
            statusBarColor = color

            if (color == Color.BLACK) {
                decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            } else {
                decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            }
//            val decorView = window.decorView
//            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE /*or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN*/)
        }
    }

    fun Context.shareApp(msg: String = "") {
        try {
            val shareMessage = if (msg.isEmpty()) {
                """
   
                     Please try this application
                     
                     https://play.google.com/store/apps/details?id=${packageName}
                     """.trimIndent() + "\n"
            } else {
                msg + "\n" + """
        
                     Please try this application
                     
                     https://play.google.com/store/apps/details?id=${packageName}
                     """.trimIndent() + "\n"
            }

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(shareIntent, "choose one"))

        } catch (ignored: Exception) {
        }
    }

    fun Activity.rateUs() {
        val uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivityForResult(goToMarket, 111)
        } catch (e: ActivityNotFoundException) {
            val goToMarket = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
            )
            startActivityForResult(goToMarket, 111)
        }
    }

    fun Activity.privacyPolicy(privacyPolicyUrl: String = "") {
        try {
            val browserIntent: Intent = if (!TextUtils.isEmpty(privacyPolicyUrl)) {
                Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
            } else {
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://visiontecprivacypolicy.blogspot.com/?m=1")
                )
            }
            startActivityForResult(browserIntent, 222)
        } catch (ignored: java.lang.Exception) {
        }
    }

//    private fun launchImageCrop(uri: Uri) {
//        CropImage.activity(uri).setGuidelines(CropImageView.Guidelines.ON)
//            .setAspectRatio(10000, 10000).setCropShape(CropImageView.CropShape.RECTANGLE).start(this)
//    }

    ///////////////////////

    /***********************************************/
}
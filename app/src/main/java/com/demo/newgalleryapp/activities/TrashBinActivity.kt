package com.demo.newgalleryapp.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.newgalleryapp.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.TrashBinAdapter
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.interfaces.ImageClickListener
import com.demo.newgalleryapp.models.TrashBin
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import com.demo.newgalleryapp.utilities.CommonFunctions
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_DELETE_PERMISSION_IN_TRASH_BIN_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_RESTORE_PERMISSION_IN_TRASH_BIN_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupForDeletePermanently
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupRestoreMultiple
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File

class TrashBinActivity : AppCompatActivity(), ImageClickListener {

    private lateinit var recyclerViewTrash: RecyclerView
    private lateinit var backBtn: ImageView
    private lateinit var closeBtnTrash: ImageView
    private var selectedItemList: ArrayList<TrashBin> = ArrayList()

    companion object {
        lateinit var trashBinAdapter: TrashBinAdapter
    }

    private lateinit var noData: LinearLayout
    private lateinit var linearLayoutForMainText: LinearLayout
    private lateinit var linearLayoutForSelectText: LinearLayout
    private lateinit var trashBinTxt: TextView
    private lateinit var howManyItemInTrash: TextView
    private lateinit var itemSelectedTrashBinTxt: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
    private var count: Int = 0
    private var updated: Boolean = false
    private var trashList: ArrayList<TrashBin> = ArrayList()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 99 && resultCode == Activity.RESULT_OK) {
            trashBinAdapter.updateSelectionState(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                (application as AppClass).mainViewModel.getAllTrashMedia()
                loadAllTrashData()
            }
        } else if ((requestCode == REQ_CODE_FOR_RESTORE_PERMISSION_IN_TRASH_BIN_ACTIVITY && resultCode == Activity.RESULT_OK) || (requestCode == 108 && resultCode == Activity.RESULT_OK)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                updated = true
                (application as AppClass).mainViewModel.getAllTrashMedia()
                loadAllTrashData()
                Toast.makeText(this, "Restore Successfully.", Toast.LENGTH_SHORT).show()
            }
        } else if ((requestCode == REQ_CODE_FOR_DELETE_PERMISSION_IN_TRASH_BIN_ACTIVITY && resultCode == Activity.RESULT_OK)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                updated = true
                (application as AppClass).mainViewModel.getAllTrashMedia()
                loadAllTrashData()
                Toast.makeText(this, "Delete Successfully.", Toast.LENGTH_SHORT).show()
            }
        } else if ((requestCode == 999 && resultCode == Activity.RESULT_OK)) {
            (application as AppClass).mainViewModel.getMediaFromInternalStorage()
        } else {
            trashBinAdapter.updateSelectionState(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash_bin)

        backBtn = findViewById(R.id.trashBin_back_btn)
        closeBtnTrash = findViewById(R.id.close_btn_trash)
        recyclerViewTrash = findViewById(R.id.trashBin_recycler_view)
        noData = findViewById(R.id.no_data)
        trashBinTxt = findViewById(R.id.trash_txt)
        howManyItemInTrash = findViewById(R.id.howManyItemInTrash)

        linearLayoutForMainText = findViewById(R.id.linearLayoutForMainText)
        linearLayoutForSelectText = findViewById(R.id.linearLayoutForSelectText)

        itemSelectedTrashBinTxt = findViewById(R.id.item_selected_text_view_trash)
        bottomNavigationView = findViewById(R.id.bottomNavigation_trashBin)

        recyclerIsEmptyOrNot()

        sharedPreferencesHelper = SharedPreferencesHelper(this@TrashBinActivity)
        count = sharedPreferencesHelper.getGridColumns()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //these code is for android 11 and above
            loadAllTrashData()
        } else {
            //this is for below android 10 version
            loadAllDeletedDatabaseData()
        }

        // here handling the back press btn
        backBtn.setOnClickListener {
            if (updated) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        closeBtnTrash.setOnClickListener {
            setAllVisibility()
        }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            setBottomNavigationViewForTrashBin(menuItem.itemId)
            true
        }
    }

    override fun onBackPressed() {
        if (updated) {
            val intent = Intent()
            setResult(Activity.RESULT_OK, intent)
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onBackPressed()
    }

    private fun loadAllDeletedDatabaseData() {
        ImagesDatabase.getDatabase(this).favoriteImageDao().getAllDeleteImages().observe(this) {
            recyclerViewTrash.layoutManager =
                GridLayoutManager(this, count, LinearLayoutManager.VERTICAL, false)
            trashBinAdapter = TrashBinAdapter(
                this@TrashBinActivity, it as ArrayList<TrashBin>, this@TrashBinActivity
            )
            recyclerViewTrash.adapter = trashBinAdapter
            howManyItemInTrash.text = trashBinAdapter.itemCount.toString()
        }
    }

    private fun loadAllTrashData() {
        (application as AppClass).mainViewModel.allTrashData.observe(this) {
            trashList.clear()
            trashList.addAll(it)
            recyclerViewTrash.layoutManager =
                GridLayoutManager(this, count, LinearLayoutManager.VERTICAL, false)
            trashBinAdapter = TrashBinAdapter(this, trashList, this@TrashBinActivity)
            recyclerViewTrash.adapter = trashBinAdapter
            howManyItemInTrash.text = trashBinAdapter.itemCount.toString()
        }
    }

    private fun setBottomNavigationViewForTrashBin(itemId: Int) {
        when (itemId) {
            R.id.restore_trash -> {

                selectedItemList = trashBinAdapter.checkTrashSelectedList

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                    val arrayList: ArrayList<Uri> = ArrayList()

                    // Create a copy of the list to avoid ConcurrentModificationException
                    val copiedList = ArrayList(selectedItemList)

                    copiedList.map { trashBin ->
                        val file = File(trashBin.destinationImagePath).path

                        if (copiedList.isNotEmpty()) {
                            MediaScannerConnection.scanFile(
                                this, arrayOf(file), null
                            ) { _, uri ->
                                arrayList.add(uri)
                                try {
                                    if (arrayList.size == copiedList.size) {
                                        val pendingIntent: PendingIntent =
                                            MediaStore.createTrashRequest(
                                                contentResolver, arrayList, false
                                            )
                                        startIntentSenderForResult(
                                            pendingIntent.intentSender,
                                            REQ_CODE_FOR_RESTORE_PERMISSION_IN_TRASH_BIN_ACTIVITY,
                                            null,
                                            0,
                                            0,
                                            0,
                                            null
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("TAG", "000AAA: $e")
                                }
                            }
                        }
                        setAllVisibility()
                        ///////////////
                    }
                } else {
                    if (selectedItemList.isNotEmpty()) {

                        (application as AppClass).mainViewModel.flag = true
                        showPopupRestoreMultiple(bottomNavigationView, selectedItemList)
//
//                        AlertDialog.Builder(this).setTitle("Restore ${selectedItemList.size} item?")
//                            .setMessage("Are you sure to restore these ${selectedItemList.size} files?")
//                            .setPositiveButton("Yes") { _, _ ->
//                                // HERE I AM DELETING THE IMAGE WITH CURRENT PATH
//                                (application as AppClass).mainViewModel.flag = true
//                                (application as AppClass).mainViewModel.restoreMultipleImagesVideos(selectedItemList)
//                                setAllVisibility()
//
//                            }.setNegativeButton("No") { dialog, _ ->
//                                setAllVisibility()
//                                dialog.dismiss()
//                            }.show()
                    } else {
                        Toast.makeText(this, "Error: Image not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            R.id.delete_trash -> {
                val selectedItemList = trashBinAdapter.checkTrashSelectedList

                val paths = selectedItemList.map { it.destinationImagePath }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                    val arrayList: ArrayList<Uri> = ArrayList()
                    MediaScannerConnection.scanFile(
                        this, paths.toTypedArray(), null
                    ) { _, uri ->
                        arrayList.add(uri)
                        if (arrayList.size == paths.size) {
                            val pendingIntent: PendingIntent =
                                MediaStore.createDeleteRequest(contentResolver, arrayList)
                            startIntentSenderForResult(
                                pendingIntent.intentSender,
                                REQ_CODE_FOR_DELETE_PERMISSION_IN_TRASH_BIN_ACTIVITY,
                                null,
                                0,
                                0,
                                0,
                                null
                            )
                        }
                    }
                    setAllVisibility()
                }
                else {

                    if (paths.isNotEmpty()) {
                        showPopupForDeletePermanently(bottomNavigationView, selectedItemList)
                    } else {
                        CommonFunctions.showToast(this, "Error: Image not found")
                    }

//                    if (paths.isNotEmpty()) {
//                        AlertDialog.Builder(this).setTitle("Delete Image")
//                            .setMessage("Are you sure you want to delete this image?")
//                            .setPositiveButton("Yes") { _, _ ->
//                                // User clicked "Yes", proceed with deletion
//                                val deletedImagePath =
//                                    // HERE I AM DELETING THE IMAGE WITH CURRENT PATH
//                                    (application as AppClass).mainViewModel.deleteMultiple(selectedItemList)
//                                deletedImagePath.let {
//                                    trashBinAdapter.remove(currentPosition)
//                                }
//
//                                setAllVisibility()
//                            }.setNegativeButton("No") { dialog, _ ->
//                                dialog.dismiss()
//                            }.show()
//                    } else {
//                        Toast.makeText(this, "Error: Image not found", Toast.LENGTH_SHORT).show()
//                    }
                }

            }
        }
    }

    private fun recyclerIsEmptyOrNot() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (application as AppClass).mainViewModel.allTrashData.observe(this, Observer {
                if (it.isEmpty()) {
                    noData.visibility = View.VISIBLE
                    recyclerViewTrash.visibility = View.GONE
                    trashBinTxt.visibility = View.GONE
                } else {
                    noData.visibility = View.GONE
                    trashBinTxt.visibility = View.VISIBLE
                    recyclerViewTrash.visibility = View.VISIBLE
                }

            })
        } else {
            ImagesDatabase.getDatabase(this).favoriteImageDao().getAllDeleteImages()
                .observe(this) { userNotes ->
                    if (userNotes.isEmpty()) {
                        noData.visibility = View.VISIBLE
                        recyclerViewTrash.visibility = View.GONE
                        trashBinTxt.visibility = View.GONE
                    } else {
                        noData.visibility = View.GONE
                        trashBinTxt.visibility = View.VISIBLE
                        recyclerViewTrash.visibility = View.VISIBLE
                    }
                }

        }
    }

    override fun onLongClick() {
        bottomNavigationView.visibility = View.VISIBLE
        linearLayoutForMainText.visibility = View.GONE
        linearLayoutForSelectText.visibility = View.VISIBLE
    }

    fun setAllVisibility() {
        trashBinAdapter.updateSelectionState(false)
        bottomNavigationView.visibility = View.GONE
        linearLayoutForMainText.visibility = View.VISIBLE
        linearLayoutForSelectText.visibility = View.GONE
    }

    override fun counter(select: Int) {
        if (select == 0) {
            setAllVisibility()
        }
        itemSelectedTrashBinTxt.text = "Item Selected $select"
    }

}
package com.demo.newgalleryapp.activities

import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.photosFragment
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.videosFragment
import com.demo.newgalleryapp.adapters.TrashBinAdapter
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.classes.CustomTypefaceSpan
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.databinding.DialogLoadingBinding
import com.demo.newgalleryapp.interfaces.ImageClickListener
import com.demo.newgalleryapp.models.TrashBinAboveVersion
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_CHANGES_IN_OPEN_TRASH_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_DELETE_PERMISSION_IN_TRASH_BIN_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_RESTORE_PERMISSION_IN_TRASH_BIN_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupForDeletePermanently
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupRestoreMultiple
import com.demo.newgalleryapp.utilities.CommonFunctions.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView

class TrashBinActivity : AppCompatActivity(), ImageClickListener {

    private lateinit var recyclerViewTrash: RecyclerView
    private lateinit var backBtn: ImageView
    private lateinit var threeDot: ImageView
    private lateinit var closeBtnTrash: ImageView
    private var selectedItemList: ArrayList<TrashBinAboveVersion> = ArrayList()
    private lateinit var noData: LinearLayout

    //    private lateinit var trashBinTxt: TextView
    private lateinit var howManyItemInTrash: TextView
    private lateinit var itemSelectedTrashBinTxt: TextView
    private lateinit var itemSelectedAllTrashBinTxt: TextView
    private lateinit var itemDeSelectedAllTrashBinTxt: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
    private var count: Int = 0
    private var updated: Boolean = false
    private var newTrashList: ArrayList<TrashBinAboveVersion> = ArrayList()
    var handler: Handler? = null
    private var popupWindow: PopupWindow? = null

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


    companion object {
        lateinit var trashBinAdapter: TrashBinAdapter
        lateinit var linearLayoutForMainText: LinearLayout
        lateinit var linearLayoutForSelectText: LinearLayout
        lateinit var bottomNavigationView: BottomNavigationView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_FOR_CHANGES_IN_OPEN_TRASH_ACTIVITY && resultCode == Activity.RESULT_OK) {

            updated = true
            (application as AppClass).mainViewModel.getMediaFromInternalStorage()
            (application as AppClass).mainViewModel.queryTrashedMediaOnDevice()
            photosFragment.imagesAdapter?.notifyDataSetChanged()
            videosFragment.imagesAdapter?.notifyDataSetChanged()

        } else if ((requestCode == REQ_CODE_FOR_RESTORE_PERMISSION_IN_TRASH_BIN_ACTIVITY && resultCode == Activity.RESULT_OK)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                progressDialogFragment.show()

                updated = true
                (application as AppClass).mainViewModel.queryTrashedMediaOnDevice()
                (application as AppClass).mainViewModel.getMediaFromInternalStorage()
                loadAllTrashData()

                handler?.postDelayed({
                    showToast(this, "Restore Successfully!!")
                    progressDialogFragment.cancel()
                }, 1000)
            }
        } else if ((requestCode == REQ_CODE_FOR_DELETE_PERMISSION_IN_TRASH_BIN_ACTIVITY && resultCode == Activity.RESULT_OK)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                updated = true
                (application as AppClass).mainViewModel.queryTrashedMediaOnDevice()
                loadAllTrashData()
                showToast(this, "Deleted Successfully.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash_bin)

        handler = Handler(Looper.getMainLooper())

        backBtn = findViewById(R.id.trashBin_back_btn)
        threeDot = findViewById(R.id.three_dot_item_trash)
        closeBtnTrash = findViewById(R.id.close_btn_trash)
        recyclerViewTrash = findViewById(R.id.trashBin_recycler_view)
        noData = findViewById(R.id.no_data)
//        trashBinTxt = findViewById(R.id.trash_txt)
        howManyItemInTrash = findViewById(R.id.howManyItemInTrash)

        linearLayoutForMainText = findViewById(R.id.linearLayoutForMainText)
        linearLayoutForSelectText = findViewById(R.id.linearLayoutForSelectText)

        itemSelectedTrashBinTxt = findViewById(R.id.item_selected_text_view_trash)
        itemSelectedAllTrashBinTxt = findViewById(R.id.trash_bin_selectAll_text)
        itemDeSelectedAllTrashBinTxt = findViewById(R.id.trash_bin_DeselectAll_text)
        bottomNavigationView = findViewById(R.id.bottomNavigation_trashBin)
        progressBar = findViewById(R.id.progressBar_trash)

        progressBar.visibility = View.VISIBLE

        // here handling the back press btn
        backBtn.setOnClickListener {
            if (updated) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
            } else if ((applicationContext as AppClass).mainViewModel.flagForTrashBinActivity) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        threeDot.setOnClickListener {
            showPopupSelect(recyclerViewTrash)
        }

        itemSelectedAllTrashBinTxt.setOnClickListener {

            trashBinAdapter.isSelected = true
            trashBinAdapter.updateSelectionState(true)
            trashBinAdapter.checkTrashSelectedList.clear()
            trashBinAdapter.checkTrashSelectedList.addAll(newTrashList)
            counter(trashBinAdapter.checkTrashSelectedList.size)
            itemSelectedAllTrashBinTxt.visibility = View.GONE
            itemDeSelectedAllTrashBinTxt.visibility = View.VISIBLE
        }

        itemDeSelectedAllTrashBinTxt.setOnClickListener {
            trashBinAdapter.isSelected = false
            trashBinAdapter.updateSelectionState(false)
            trashBinAdapter.checkTrashSelectedList.clear()
            counter(trashBinAdapter.checkTrashSelectedList.size)
            itemDeSelectedAllTrashBinTxt.visibility = View.GONE
            itemSelectedAllTrashBinTxt.visibility = View.VISIBLE
        }

        sharedPreferencesHelper = SharedPreferencesHelper(this@TrashBinActivity)
        count = sharedPreferencesHelper.getGridColumns()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (application as AppClass).mainViewModel.queryTrashedMediaOnDevice()
            //these code is for android 11 and above
            loadAllTrashData()
            progressBar.visibility = View.GONE
        } else {
            //this code is for below android 10 version
            loadAllDeletedDatabaseData()
            progressBar.visibility = View.GONE
        }


        closeBtnTrash.setOnClickListener {
            setAllVisibility()
        }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            setBottomNavigationViewForTrashBin(menuItem.itemId)
            true
        }

        val menu = bottomNavigationView.menu

//        for (i in 0 until menu.size()) {
//            val item = menu.getItem(i)
//            val spannableString = SpannableString(item.title)
//            val font = Typeface.createFromAsset(assets, "poppins_medium.ttf")
//            spannableString.setSpan(TypefaceSpan(font), 0, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//            item.title = spannableString
//        }

        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val spannableString = SpannableString(item.title)
            val font = Typeface.createFromAsset(assets, "poppins_medium.ttf")
            spannableString.setSpan(
                CustomTypefaceSpan(font),
                0,
                spannableString.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            item.title = spannableString
        }


    }

    override fun onBackPressed() {
        if (updated) {
            val intent = Intent()
            setResult(Activity.RESULT_OK, intent)
        } else if ((applicationContext as AppClass).mainViewModel.flagForTrashBinActivity) {
            val intent = Intent()
            setResult(Activity.RESULT_OK, intent)
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onBackPressed()
    }

    private fun loadAllDeletedDatabaseData() {
        ImagesDatabase.getDatabase(this).favoriteImageDao().getAllDeleteImages().observe(this) {

            newTrashList.clear()
            newTrashList.addAll(it)

            val temp = arrayListOf<TrashBinAboveVersion>()
            temp.addAll(newTrashList)
            recyclerIsEmptyOrNot(temp)

            recyclerViewTrash.layoutManager =
                GridLayoutManager(this, count, LinearLayoutManager.VERTICAL, false)
            trashBinAdapter = TrashBinAdapter(this@TrashBinActivity, temp, this@TrashBinActivity)
            recyclerViewTrash.adapter = trashBinAdapter

//            howManyItemInTrash.text = trashBinAdapter.itemCount.toString()
            val items = trashBinAdapter.itemCount.toString()
            howManyItemInTrash.text = "$items Items"
        }
        progressBar.visibility = View.GONE
    }

    private fun loadAllTrashData() {
        3
        (application as AppClass).mainViewModel.tempAllTrashData.observe(this) {
            newTrashList.clear()
            newTrashList.addAll(it)

            val temp = arrayListOf<TrashBinAboveVersion>()
            temp.addAll(newTrashList)
            recyclerIsEmptyOrNot(temp)

            recyclerViewTrash.layoutManager =
                GridLayoutManager(this, count, LinearLayoutManager.VERTICAL, false)
            trashBinAdapter = TrashBinAdapter(this, temp, this@TrashBinActivity)
            recyclerViewTrash.adapter = trashBinAdapter

//            howManyItemInTrash.text = trashBinAdapter.itemCount.toString()

            val items = trashBinAdapter.itemCount.toString()
            howManyItemInTrash.text = "$items Items"

        }
        progressBar.visibility = View.GONE
    }

    private fun setBottomNavigationViewForTrashBin(itemId: Int) {
        when (itemId) {
            R.id.restore_trash -> {

                selectedItemList = trashBinAdapter.checkTrashSelectedList

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                    val arrayList: ArrayList<Uri> = ArrayList()
                    // Create a copy of the list to avoid ConcurrentModificationException
                    val copiedList = ArrayList(selectedItemList)

                    if (copiedList.isNotEmpty()) {
                        copiedList.map { trashBin ->

                            val imageUri = trashBin.uri

                            if (copiedList.isNotEmpty()) {

                                arrayList.add(imageUri)
                                if (arrayList.size == copiedList.size) {

                                    try {
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
                                    } catch (e: Exception) {
                                        Log.e("TAG", "000AAA: $e")
                                    }
                                    ////////////////
                                }
                            } else {
                                showToast(this@TrashBinActivity, "Error: Image not found")
                            }
                            ///////////////
                        }

                        setAllVisibility()
                    } else {
                        showToast(this, "No images/videos Selected to restore!!")
                    }
                }
                //////////
                else {
                    //this code is for below android 10 version
                    if (selectedItemList.isNotEmpty()) {
                        showPopupRestoreMultiple(
                            bottomNavigationView, selectedItemList, this@TrashBinActivity
                        )
                    } else {
                        showToast(this@TrashBinActivity, "Error: Image not found")
                    }
                }
                /////////////////
            }

            R.id.delete_trash -> {

                selectedItemList = trashBinAdapter.checkTrashSelectedList

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                    val arrayList: ArrayList<Uri> = ArrayList()
                    // Create a copy of the list to avoid ConcurrentModificationException
                    val copiedList = ArrayList(selectedItemList)

                    if (copiedList.isNotEmpty()) {
                        copiedList.map { trashBin ->

                            val imageUri = trashBin.uri

                            if (copiedList.isNotEmpty()) {
                                arrayList.add(imageUri)
                                if (arrayList.size == copiedList.size) {

                                    try {
                                        val pendingIntent: PendingIntent =
                                            MediaStore.createDeleteRequest(
                                                contentResolver, arrayList
                                            )
                                        startIntentSenderForResult(
                                            pendingIntent.intentSender,
                                            REQ_CODE_FOR_DELETE_PERMISSION_IN_TRASH_BIN_ACTIVITY,
                                            null,
                                            0,
                                            0,
                                            0,
                                            null
                                        )
                                    } catch (e: Exception) {
                                        Log.e("TAG", "000AAA: $e")
                                    }
                                    ////////////////
                                }
                            } else {
                                showToast(this@TrashBinActivity, "Error: Image not found")
                            }
                            ///////////////
                        }

                        setAllVisibility()
                    } else {
                        showToast(this, "No images/videos Selected to delete!!")
                    }
                    ///////////////////////////////////////
                } else {
                    if (selectedItemList.isNotEmpty()) {
                        showPopupForDeletePermanently(
                            bottomNavigationView, selectedItemList, this@TrashBinActivity
                        )
                    } else {
                        showToast(this, "Error: Image not found")
                    }
                }
                //////////////////
            }
            ///////////////
        }
    }

    private fun recyclerIsEmptyOrNot(trashList: ArrayList<TrashBinAboveVersion>) {
        if (trashList.isEmpty()) {
            noData.visibility = View.VISIBLE
            recyclerViewTrash.visibility = View.GONE
            threeDot.visibility = View.GONE
        } else {
            noData.visibility = View.GONE
            threeDot.visibility = View.VISIBLE
            recyclerViewTrash.visibility = View.VISIBLE
        }
    }

    override fun onLongClick() {
        bottomNavigationView.visibility = View.VISIBLE
        linearLayoutForMainText.visibility = View.GONE
        linearLayoutForSelectText.visibility = View.VISIBLE
    }

    private fun setAllVisibility() {
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

    private fun showPopupSelect(
        anchorView: View
    ) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.popup_select_item, null)

        popupWindow = PopupWindow(
            popupView, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT, true
        )

        popupWindow?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )


        val selectedItem = popupView.findViewById<LinearLayout>(R.id.selectedItem)

        selectedItem.setOnClickListener {
            trashBinAdapter.isSelected = true
            onLongClick()
            trashBinAdapter.notifyDataSetChanged()
            popupWindow?.dismiss()
        }


        val popupItem = popupView.findViewById<RelativeLayout>(R.id.popupItem_select_one)

        popupItem.setOnClickListener {
            popupWindow?.dismiss()
        }
        // Set dismiss listener to nullify the reference
        popupWindow?.setOnDismissListener {
            popupWindow = null
        }
    }


//    fun File.isImageFile(): Boolean {
//        val imageExtensions =
//            setOf("jpg", "jpeg", "png", "gif", "bmp") // Add more extensions as needed
//        return isFile && extension.toLowerCase() in imageExtensions
//    }

}
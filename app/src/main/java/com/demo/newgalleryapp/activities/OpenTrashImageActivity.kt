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
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.ImageSliderAdapter2
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.classes.CustomTypefaceSpan
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.databinding.DialogLoadingBinding
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.models.TrashBinAboveVersion
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_TRASH_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_TRASH_PERMISSION_IN_OPEN_TRASH_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupForDeletePermanentlyForOne
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupRestoreOne
import com.demo.newgalleryapp.utilities.CommonFunctions.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File

class OpenTrashImageActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var backBtn: ImageView
    lateinit var trashOpenTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private var models: ArrayList<MediaModel> = ArrayList()
    private var tempList: ArrayList<TrashBinAboveVersion> = ArrayList()
    private var currentPosition: Int = 0
    private lateinit var dialogBinding: DialogLoadingBinding

    companion object {
        lateinit var imagesSliderAdapterTrash: ImageSliderAdapter2
        var updated: Boolean = false
        var handler: Handler? = null

    }

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
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_FOR_TRASH_PERMISSION_IN_OPEN_TRASH_ACTIVITY && resultCode == Activity.RESULT_OK) {
            doChanges()
            showToast(this, "Restore Successfully.")

        } else if (requestCode == REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_TRASH_ACTIVITY && resultCode == Activity.RESULT_OK) {
            doChanges()
            showToast(this, "Deleted Successfully!!")
        }
    }

    private fun doChanges() {
        updated = true
        imagesSliderAdapterTrash.remove(currentPosition, this@OpenTrashImageActivity)
        tempList.removeAt(currentPosition)
        imagesSliderAdapterTrash.notifyDataSetChanged()
        (application as AppClass).mainViewModel.getMediaFromInternalStorage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_trash_image)

        viewPager = findViewById(R.id.viewPager_slider_trash)
        backBtn = findViewById(R.id.back_btn_trash_open)
        bottomNavigationView = findViewById(R.id.bottomNavigation_trash)
        toolbar = findViewById(R.id.toolBar)
        trashOpenTextView = findViewById(R.id.trash_open_textView)

        // marquee_forever effect
        trashOpenTextView.isSelected = true
        handler = Handler(Looper.getMainLooper())

        backBtn.setOnClickListener {
            if (updated) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
                updated = false
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            (application as AppClass).mainViewModel.tempAllTrashData.observe(this) {
                tempList.clear()
                tempList.addAll(it)

                val trash =
                    it.map {
                        MediaModel(
                            0,
                            it.uri.toString(),
                            it.name,
                            it.uri.toString(),
                            "",
                            0,
                            0,
                            0,
                            it.isVideo
                        )
                    }
                models.clear()
                models.addAll(trash)
                setViewPagerAdapter(models)
            }

        } else {
            ImagesDatabase.getDatabase(this).favoriteImageDao().getAllDeleteImages()
                .observe(this, Observer { it ->
                    tempList.clear()
                    tempList.addAll(it)
                    val trash =
                        it.map {
                            MediaModel(
                                0,
                                it.uri.toString(),
                                it.path,
                                it.uri.toString(),
                                " ",
                                0,
                                0,
                                0,
                                it.isVideo
                            )
                        }

                    models.clear()
                    models.addAll(trash)
                    setViewPagerAdapter(models)
                })
        }

        viewPagerDataSetter()
        bottomNavigationViewItemSetter()

        val menu = bottomNavigationView.menu

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
            updated = false
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onBackPressed()
    }

    private fun viewPagerDataSetter() {

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                if (models.isNotEmpty() && position >= 0 && position < models.size) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val name = File(models[position].displayName)
                        trashOpenTextView.text = name.toString()
                    } else {
                        val name = File(models[position].displayName).name
                        trashOpenTextView.text = name
                    }
                }
            }


            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                Log.d("current", "onPageSelected: $currentPosition")
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                Log.d("current", "onPageSelected: $currentPosition")
            }
        })

    }

    private fun setViewPagerAdapter(models: ArrayList<MediaModel>) {

        if (models.isEmpty()) {
            if (updated) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
                updated = false
            }
            finish()
        }

        val newList = ArrayList<MediaModel>()
        newList.clear()
        newList.addAll(models)

        val pos = intent.getIntExtra("trashBinPos", 0)
        imagesSliderAdapterTrash = ImageSliderAdapter2(this@OpenTrashImageActivity, newList, false)
        viewPager.adapter = imagesSliderAdapterTrash
        viewPager.setCurrentItem(pos, false)
        currentPosition = viewPager.currentItem
    }

    private fun bottomNavigationViewItemSetter() {
        bottomNavigationView.setOnItemSelectedListener { menuItem ->

            when (menuItem.itemId) {
                R.id.restore_trash -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

//                        currentPosition = viewPager.currentItem
                        val imageUri = tempList[currentPosition].uri
                        val arrayList: ArrayList<Uri> = ArrayList()

                        if (tempList.isNotEmpty()) {
                            try {
                                arrayList.clear()
                                arrayList.add(imageUri)
                                val pendingIntent: PendingIntent = MediaStore.createTrashRequest(
                                    contentResolver, arrayList, false
                                )
                                startIntentSenderForResult(
                                    pendingIntent.intentSender,
                                    REQ_CODE_FOR_TRASH_PERMISSION_IN_OPEN_TRASH_ACTIVITY,
                                    null,
                                    0,
                                    0,
                                    0,
                                    null
                                )

                            } catch (e: Exception) {
                                Log.e("TAG", "bottomNavigationViewItemSetter: $e")
                            }
                        } else {
                            showToast(this, "Error: Image not found!!!")
                        }
                    } else {
//                        currentPosition = viewPager.currentItem
                        val trashModel = tempList[currentPosition]

                        if (trashModel.path.isNotEmpty()) {
                            showPopupRestoreOne(
                                bottomNavigationView,
                                trashModel,
                                currentPosition,
                                this@OpenTrashImageActivity
                            )
                            updated = true
                        } else {
                            Toast.makeText(this, "Error: Image not found", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }

                R.id.delete_trash -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

//                        currentPosition = viewPager.currentItem
                        val imageToDelete = tempList[currentPosition].uri
                        val arrayList: ArrayList<Uri> = ArrayList()


                        if (tempList.isNotEmpty()) {
                            try {

                                arrayList.clear()
                                arrayList.add(imageToDelete)

                                val pendingIntent: PendingIntent = MediaStore.createDeleteRequest(
                                    contentResolver, arrayList
                                )
                                startIntentSenderForResult(
                                    pendingIntent.intentSender,
                                    REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_TRASH_ACTIVITY,
                                    null,
                                    0,
                                    0,
                                    0,
                                    null
                                )

                            } catch (e: Exception) {
                                Log.e("TAG", "bottomNavigationViewItemSetter: $e")
                            }
                        } else {
                            showToast(this, "Error: Image not found!!!")
                        }

                    } else {
//                        currentPosition = viewPager.currentItem
                        val trashModel = tempList[currentPosition]

                        if (trashModel.path.isNotEmpty()) {
                            showPopupForDeletePermanentlyForOne(
                                bottomNavigationView,
                                trashModel,
                                currentPosition,
                                this@OpenTrashImageActivity
                            )
                        } else {
                            Toast.makeText(this, "Error: Image not found", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
            true // Return true to indicate that the item selection is handled
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

}
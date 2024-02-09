package com.demo.newgalleryapp.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.demo.newgalleryapp.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.ImageSliderAdapter
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.models.TrashBinAboveVersion
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_TRASH_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_TRASH_PERMISSION_IN_OPEN_TRASH_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupForDeletePermanentlyForOne
import com.demo.newgalleryapp.utilities.CommonFunctions.showPopupRestoreOne
import com.demo.newgalleryapp.utilities.CommonFunctions.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView

class OpenTrashImageActivity : AppCompatActivity() {

    //    private lateinit var recyclerView: RecyclerView
    private lateinit var viewPager: ViewPager
    private lateinit var backBtn: ImageView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private var models: ArrayList<MediaModel> = ArrayList()
    private var tempList: ArrayList<TrashBinAboveVersion> = ArrayList()
    private var updated: Boolean = false
    private var currentPosition: Int = 0

    companion object {
        lateinit var imagesSliderAdapter: ImageSliderAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_FOR_TRASH_PERMISSION_IN_OPEN_TRASH_ACTIVITY && resultCode == Activity.RESULT_OK) {
            imagesSliderAdapter.remove(currentPosition)
            tempList.removeAt(currentPosition)
            updated = true
            showToast(this, "Restore Successfully.")


        } else if (requestCode == REQ_CODE_FOR_DELETE_PERMISSION_IN_OPEN_TRASH_ACTIVITY && resultCode == Activity.RESULT_OK) {
            imagesSliderAdapter.remove(currentPosition)
            tempList.removeAt(currentPosition)
            updated = true
            showToast(this, "Delete Successfully.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_trash_image)

        viewPager = findViewById(R.id.viewPager_slider_trash)
//        recyclerView = findViewById(R.id.recycler_view_trash_new)
//        textView = findViewById(R.id.trash_open_textView)
        backBtn = findViewById(R.id.back_btn_trash_open)
        bottomNavigationView = findViewById(R.id.bottomNavigation_trash)
        toolbar = findViewById(R.id.toolBar)

        backBtn.setOnClickListener {
            if (updated) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            (application as AppClass).mainViewModel.tempAllTrashData.observe(this) {
                tempList.addAll(it)

                val trash =
                    it.map { MediaModel(0, it.path, it.uri.toString(), " ", 0, 0, 0, false) }
                models.addAll(trash)
                setViewPagerAdapter(models)
                viewPagerDataSetter()
            }

//            (application as AppClass).mainViewModel.allTrashData.observe(this, Observer {
//                tempList2.addAll(it)
//                val trash = it.map { MediaModel(0, it.currentPath, it.destinationImagePath, " ", 0, 0, 0, false) }
//                models.addAll(trash)
//                setViewPagerAdapter(models)
//                viewPagerDataSetter()
//            })
        } else {
            ImagesDatabase.getDatabase(this).favoriteImageDao().getAllDeleteImages()
                .observe(this, Observer { it ->
                    tempList.addAll(it)
                    val trash =
                        it.map { MediaModel(0, it.path, it.uri.toString(), " ", 0, 0, 0, false) }
                    models.addAll(trash)
                    setViewPagerAdapter(models)
                    viewPagerDataSetter()
                })
        }
        bottomNavigationViewItemSetter()
    }

    override fun onBackPressed() {
        if (updated) {
            val intent = Intent()
            setResult(Activity.RESULT_OK, intent)
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onBackPressed()
    }

    private fun viewPagerDataSetter() {
        viewPager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {
//                textView.text = models[position].path
            }

            override fun onPageSelected(position: Int) {

            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

    private fun setViewPagerAdapter(models: ArrayList<MediaModel>) {
        val pos = intent.getIntExtra("trashBinPos", 0)
        imagesSliderAdapter = ImageSliderAdapter(this, models)
//        recyclerView.visibility = View.GONE
        viewPager.adapter = imagesSliderAdapter
        viewPager.setCurrentItem(pos, false)

        if (models.size == 0) {
            if (updated) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
            }
            finish()
        }
    }

    private fun bottomNavigationViewItemSetter() {
        bottomNavigationView.setOnItemSelectedListener { menuItem ->

            when (menuItem.itemId) {
                R.id.restore_trash -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                        currentPosition = viewPager.currentItem
                        val imageUri = tempList[currentPosition].uri

                        if (tempList.isNotEmpty()) {
                            try {
                                val pendingIntent: PendingIntent = MediaStore.createTrashRequest(
                                    contentResolver, arrayListOf(imageUri), false
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
                        currentPosition = viewPager.currentItem
                        val trashModel = tempList[currentPosition]

                        if (trashModel.path.isNotEmpty()) {
                            showPopupRestoreOne(bottomNavigationView, trashModel, currentPosition)
                            updated = true
                        } else {
                            Toast.makeText(this, "Error: Image not found", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }

                R.id.delete_trash -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                        currentPosition = viewPager.currentItem
                        val imageToDelete = tempList[currentPosition].uri

                        val pendingIntent: PendingIntent = MediaStore.createDeleteRequest(
                            contentResolver, arrayListOf(imageToDelete)
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
                    } else {
                        currentPosition = viewPager.currentItem
                        val trashModel = tempList[currentPosition]

                        if (trashModel.path.isNotEmpty()) {
                            showPopupForDeletePermanentlyForOne(
                                bottomNavigationView, trashModel, currentPosition
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

}
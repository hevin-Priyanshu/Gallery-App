package com.demo.newgalleryapp.activities

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.FavoriteAdapter
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.interfaces.ImageClickListener
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import com.demo.newgalleryapp.utilities.CommonFunctions.showToast
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File

class FavoriteImagesActivity : AppCompatActivity(), ImageClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var backBtn: ImageView
    private lateinit var howManyItemOn: TextView
    private lateinit var itemSelectedFavoriteTxt: TextView
    private lateinit var selectAll: TextView
    private lateinit var deSelectAll: TextView
    private lateinit var closeBtnTrash: ImageView
    private lateinit var threeDot: ImageView
    private lateinit var noData: LinearLayout
    private var tempFavoriteList: ArrayList<MediaModel> = ArrayList()
    private var selectedItemList: ArrayList<MediaModel> = ArrayList()
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper
    private var popupWindow: PopupWindow? = null

    companion object {
        lateinit var mainLinearLayout: LinearLayout
        lateinit var selectedTextViewLinearLayout: LinearLayout
        lateinit var favoriteBottomNavigationView: BottomNavigationView
        lateinit var favoriteAdapter: FavoriteAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_images)

        backBtn = findViewById(R.id.back_btn)
        recyclerView = findViewById(R.id.recycler_view_fav)
        noData = findViewById(R.id.no_data)
        closeBtnTrash = findViewById(R.id.closeBtn_favorite)
        threeDot = findViewById(R.id.three_dot_item_favorite)
        selectAll = findViewById(R.id.SelectAll_favorite)
        deSelectAll = findViewById(R.id.DeSelectAll_favorite)
        howManyItemOn = findViewById(R.id.howManyItemOn)
        itemSelectedFavoriteTxt = findViewById(R.id.itemSelectedFavoriteTxt)
        mainLinearLayout = findViewById(R.id.main_linear_layout_favorite)
        selectedTextViewLinearLayout = findViewById(R.id.selectText_linear_layout_favorite)
        favoriteBottomNavigationView = findViewById(R.id.bottomNavigationViewFavorite)


        closeBtnTrash.setOnClickListener {
            setAllVisibility()
        }

        threeDot.setOnClickListener {
            showPopupSelect(recyclerView)
        }

        sharedPreferencesHelper = SharedPreferencesHelper(this)
        val gridCount = sharedPreferencesHelper.getGridColumns()
        loadFavoriteItemsFromDataBase(gridCount)

        selectAll.setOnClickListener {
            favoriteAdapter.isSelected = true
            favoriteAdapter.updateSelectionState(true)
            favoriteAdapter.checkSelectedList.clear()
            favoriteAdapter.checkSelectedList.addAll(tempFavoriteList)
            counter(favoriteAdapter.checkSelectedList.size)
            selectAll.visibility = View.GONE
            deSelectAll.visibility = View.VISIBLE
        }

        deSelectAll.setOnClickListener {
            favoriteAdapter.isSelected = false
            favoriteAdapter.updateSelectionState(false)
            favoriteAdapter.checkSelectedList.clear()
            counter(favoriteAdapter.checkSelectedList.size)
            deSelectAll.visibility = View.GONE
            selectAll.visibility = View.VISIBLE
        }

        val menuItemMore = favoriteBottomNavigationView.menu.findItem(R.id.moreItem)
        val menuItemDeleteItem = favoriteBottomNavigationView.menu.findItem(R.id.deleteItem)

        menuItemMore.isVisible = false
        menuItemDeleteItem.isVisible = false

        favoriteBottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.shareItem -> shareSelectedImages()
                R.id.favoriteItem -> handleFavoriteAction()
            }
            true
        }
        // here handling the back press btn
        backBtn.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun handleFavoriteAction() {
        //        val model = models[viewPager.currentItem]
        selectedItemList = favoriteAdapter.checkSelectedList
        selectedItemList.map {
            val paths = it.path

            val favoriteImageDao = ImagesDatabase.getDatabase(this).favoriteImageDao()

            val roomModel = favoriteImageDao.getModelByFile(paths)

            if (roomModel != null) {
                favoriteImageDao.deleteFavorite(roomModel)
            }
        }
//        showToast(this, "Item Remove")
        setAllVisibility()
    }

    private fun shareSelectedImages() {

        selectedItemList = favoriteAdapter.checkSelectedList
        val paths = selectedItemList.map { it.path }

        if (paths.isNotEmpty()) {
            val uris = ArrayList<Uri>()

            // Convert file paths to Uri using FileProvider
            for (file in paths) {
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

    private fun loadFavoriteItemsFromDataBase(gridCount: Int) {
        ImagesDatabase.getDatabase(this).favoriteImageDao().getAllFavorites()
            .observe(this, Observer { favorites ->

                tempFavoriteList.clear()
                tempFavoriteList.addAll(favorites)

                recyclerIsEmptyOrNot(tempFavoriteList)

                recyclerView.layoutManager = GridLayoutManager(this, gridCount, LinearLayoutManager.VERTICAL, false)
                favoriteAdapter = FavoriteAdapter(this@FavoriteImagesActivity, tempFavoriteList, this@FavoriteImagesActivity)
                recyclerView.adapter = favoriteAdapter
//                howManyItemOn.text = favoriteAdapter.itemCount.toString()
                val items = favoriteAdapter.itemCount.toString()
                howManyItemOn.text = "$items Items"
            })
    }

    private fun recyclerIsEmptyOrNot(tempFavoriteList: ArrayList<MediaModel>) {
        if (tempFavoriteList.isEmpty()) {
            noData.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            threeDot.visibility = View.GONE
        } else {
            noData.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            threeDot.visibility = View.VISIBLE
        }

    }

    override fun onLongClick() {
        favoriteBottomNavigationView.visibility = View.VISIBLE
        mainLinearLayout.visibility = View.GONE
        selectedTextViewLinearLayout.visibility = View.VISIBLE
    }

    override fun counter(select: Int) {
        if (select == 0) {
            setAllVisibility()
        }
        itemSelectedFavoriteTxt.text = "Item Selected $select"
    }

    private fun setAllVisibility() {
        favoriteAdapter.updateSelectionState(false)
        favoriteBottomNavigationView.visibility = View.GONE
        mainLinearLayout.visibility = View.VISIBLE
        selectedTextViewLinearLayout.visibility = View.GONE
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
            favoriteAdapter.isSelected = true
            onLongClick()
            favoriteAdapter.notifyDataSetChanged()
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


}
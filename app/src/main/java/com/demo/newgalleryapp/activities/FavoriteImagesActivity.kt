package com.demo.newgalleryapp.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.FavoriteAdapter
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.models.MediaModel

class FavoriteImagesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var backBtn: ImageView
    private lateinit var howManyItemOn: TextView
    private lateinit var favoriteAdapter: FavoriteAdapter
    private lateinit var noData: LinearLayout
    private var tempFavoriteList: ArrayList<MediaModel> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_images)

        backBtn = findViewById(R.id.back_btn)
        recyclerView = findViewById(R.id.recycler_view_fav)
        noData = findViewById(R.id.no_data)
        howManyItemOn = findViewById(R.id.howManyItemOn)


        ImagesDatabase.getDatabase(this).favoriteImageDao().getAllFavorites()
            .observe(this, Observer { favorites ->

                tempFavoriteList.clear()
                tempFavoriteList.addAll(favorites)

                recyclerIsEmptyOrNot(tempFavoriteList)

                recyclerView.layoutManager = GridLayoutManager(this, 4, LinearLayoutManager.VERTICAL, false)
                favoriteAdapter = FavoriteAdapter(this@FavoriteImagesActivity, tempFavoriteList)
                recyclerView.adapter = favoriteAdapter
                howManyItemOn.text = favoriteAdapter.itemCount.toString()
            })


        // here handling the back press btn
        backBtn.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun recyclerIsEmptyOrNot(tempFavoriteList: ArrayList<MediaModel>) {
        if (tempFavoriteList.isEmpty()) {
            noData.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noData.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

    }
}
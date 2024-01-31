package com.demo.newgalleryapp.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.demo.newgalleryapp.activities.OpenImageActivity
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import java.io.File

class FavoriteAdapter(val context: Activity, val list: ArrayList<MediaModel>) :
    RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>() {
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.images_item, parent, false)
        return FavoriteViewHolder(layout)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {

        val screenWidth = context.resources.displayMetrics.widthPixels
        sharedPreferencesHelper = SharedPreferencesHelper(context)

        //get here height of images according to numbers of columns , by dividing them
        val imageViewWidth = screenWidth / sharedPreferencesHelper.getGridColumns()

        val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageViewWidth)
        holder.image.layoutParams = layoutParams

        holder.image.setOnClickListener {

            val intent = Intent(context, OpenImageActivity::class.java)
//            intent.putExtra("favoritesPosition", true)
            intent.putExtra("currentState", 2)
            intent.putExtra("selectedImagePosition", position)
            context.startActivity(intent)
//            context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        val image = list[position].path

        Glide.with(context).load(File(image)).placeholder(R.drawable.placeholder)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(holder.image)
    }

    class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image: ImageView = itemView.findViewById(R.id.image_view)
    }

}
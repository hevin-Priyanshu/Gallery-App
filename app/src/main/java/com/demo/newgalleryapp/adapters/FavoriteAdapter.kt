package com.demo.newgalleryapp.adapters

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.OpenImageActivity
import com.demo.newgalleryapp.interfaces.ImageClickListener
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import java.io.File

class FavoriteAdapter(
    val context: Activity,
    val list: ArrayList<MediaModel>,
    private val listener: ImageClickListener? = null
) : RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>() {

    var isSelected = false
    var checkSelectedList: ArrayList<MediaModel> = ArrayList()
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    fun updateSelectionState(isSelected: Boolean) {
        this.isSelected = isSelected
        if (!isSelected) {
            checkSelectedList.clear()
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.images_item, parent, false)
        return FavoriteViewHolder(layout)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {


//        val screenWidth = context.resources.displayMetrics.widthPixels
//        sharedPreferencesHelper = SharedPreferencesHelper(context)
//
//        //get here height of images according to numbers of columns , by dividing them
//        val imageViewWidth = screenWidth / sharedPreferencesHelper.getGridColumns()
//
//        Log.d("imageViewWidth", "onBindViewHolder: $imageViewWidth")
//
//        val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageViewWidth)
//        holder.image.layoutParams = layoutParams


        val screenWidthFav = context.resources.displayMetrics.widthPixels
        sharedPreferencesHelper = SharedPreferencesHelper(context)

        //get here height of images according to numbers of columns , by dividing them
        val imageViewWidthFav = screenWidthFav / sharedPreferencesHelper.getGridColumns()

        val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageViewWidthFav)
        holder.image.layoutParams = layoutParams

        val imageModel = list[position]

        holder.favoriteLogo.visibility = View.VISIBLE

        if (checkSelectedList.map { it.path }.contains(imageModel.path)) {
            holder.isSelectedCheckbox.setImageResource(R.drawable.right_tick_item)
        } else {
            holder.isSelectedCheckbox.setImageResource(R.drawable.empty_select_item_blur)
        }

        holder.image.setOnClickListener {

            if (isSelected) {
                if (checkSelectedList.map { it.path }.contains(imageModel.path)) {
                    // If image is already selected, unselect it
                    checkSelectedList.remove(imageModel)
//                    holder.isSelectedCheckbox.isChecked = false
                    holder.isSelectedCheckbox.setImageResource(R.drawable.empty_select_item_blur)
                } else {
                    // If image is not selected, select it
                    checkSelectedList.add(imageModel)
//                    holder.isSelectedCheckbox.isChecked = true
                    holder.isSelectedCheckbox.setImageResource(R.drawable.right_tick_item)
                }
                // Notify listener about the change in selection count
                listener?.counter(checkSelectedList.size)
            } else {
                val intent = Intent(context, OpenImageActivity::class.java)
//            intent.putExtra("favoritesPosition", true)
                intent.putExtra("currentState", 2)
                intent.putExtra("selectedImagePosition", position)
                context.startActivity(intent)
//            context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }

        }

        holder.isSelectedCheckbox.visibility = if (isSelected) View.VISIBLE else View.GONE

        holder.image.setOnLongClickListener {
            // Automatically select the image on long-press
            isSelected = true
            if (!checkSelectedList.map { it.path }.contains(imageModel.path)) {

                checkSelectedList.add(imageModel)

                holder.isSelectedCheckbox.visibility = View.VISIBLE
                listener?.onLongClick()
                listener?.counter(checkSelectedList.size)
                notifyDataSetChanged()
            }
            true
        }


        val image = list[position].path

        Glide.with(context).load(File(image)).placeholder(R.drawable.placeholder)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(holder.image)
    }

    class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image: ImageView = itemView.findViewById(R.id.image_view)
        var isSelectedCheckbox: ImageView = itemView.findViewById(R.id.isSelectedCheckbox)
        var favoriteLogo: ImageView = itemView.findViewById(R.id.favorite)
    }

}
package com.demo.newgalleryapp.adapters

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.OpenTrashImageActivity
import com.demo.newgalleryapp.interfaces.ImageClickListener
import com.demo.newgalleryapp.models.TrashBinAboveVersion
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_CHANGES_IN_OPEN_TRASH_ACTIVITY

class TrashBinAdapter(
    private val context: Activity,
    private val list: ArrayList<TrashBinAboveVersion>,
    private val listener: ImageClickListener? = null
) : RecyclerView.Adapter<TrashBinAdapter.TrashBinViewHolder>() {

    var isSelected = false
    var checkTrashSelectedList: ArrayList<TrashBinAboveVersion> = ArrayList()
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashBinViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.images_item, parent, false)
        return TrashBinViewHolder(layout)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun remove(position: Int) {
        if (position >= 0 && position < list.size) {
            list.removeAt(position)
            notifyItemChanged(position)
        }
    }

    fun updateSelectionState(isSelected: Boolean) {
        this.isSelected = isSelected
        if (!isSelected) {
            checkTrashSelectedList.clear()
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: TrashBinViewHolder, position: Int) {

        val screenWidth = context.resources.displayMetrics.widthPixels
        sharedPreferencesHelper = SharedPreferencesHelper(context)

        //get here height of images according to numbers of columns , by dividing them
        val imageViewWidth = screenWidth / sharedPreferencesHelper.getGridColumns()

        val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageViewWidth)
        holder.image.layoutParams = layoutParams


        if (list[position].isVideo) {
            holder.imageVideoThumbnail.visibility = View.VISIBLE
        } else {
            holder.imageVideoThumbnail.visibility = View.GONE
        }

        if (checkTrashSelectedList.map { it.uri }.contains(list[position].uri)) {
            holder.isSelectedCheckbox.setImageResource(R.drawable.right_tick_item)
        } else {
            holder.isSelectedCheckbox.setImageResource(R.drawable.empty_select_item_blur)
        }


//        holder.isSelectedCheckbox.isChecked = checkTrashSelectedList.map { it.currentPath }.contains(list[position].currentPath)

        holder.image.setOnClickListener {

            if (isSelected) {
                if (checkTrashSelectedList.map { it.uri }
                        .contains(list[position].uri)) {
                    // If image is already selected, unselect it
                    checkTrashSelectedList.remove(list[position])
//                    holder.isSelectedCheckbox.isChecked = false
                    holder.isSelectedCheckbox.setImageResource(R.drawable.empty_select_item_blur)
                } else {
                    // If image is not selected, select it
                    checkTrashSelectedList.add(list[position])
//                    holder.isSelectedCheckbox.isChecked = true
                    holder.isSelectedCheckbox.setImageResource(R.drawable.right_tick_item)
                }
                // Notify listener about the change in selection count
                listener?.counter(checkTrashSelectedList.size)
            } else {
                val intentTrash = Intent(context, OpenTrashImageActivity::class.java)
                intentTrash.putExtra("trashBinPos", position)
                context.startActivityForResult(
                    intentTrash, REQ_CODE_FOR_CHANGES_IN_OPEN_TRASH_ACTIVITY
                )
            }
        }

        holder.isSelectedCheckbox.visibility = if (isSelected) View.VISIBLE else View.GONE

        holder.image.setOnLongClickListener {

            isSelected = true
            if (!checkTrashSelectedList.contains(list[position])) {
                checkTrashSelectedList.add(list[position])
//                holder.isSelectedCheckbox.isChecked = true
                holder.isSelectedCheckbox.setImageResource(R.drawable.right_tick_item)
                holder.isSelectedCheckbox.visibility = View.VISIBLE
                listener?.onLongClick()
                listener?.counter(checkTrashSelectedList.size)
                notifyDataSetChanged()
            }
            true
        }

        val imagePath = list[position].uri
//        Picasso.get()
//            .load(fileName)
//            .error(R.drawable.placeholder)
//            .into(holder.image)

//        val imageUri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))

        Glide.with(context).load(imagePath).placeholder(R.drawable.placeholder)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("TAG_E", "onLoadFailed: ${e?.message}")
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("TAG_E", "onResourceReady: ${model.toString()}")
                    return false
                }

            }).diskCacheStrategy(
                DiskCacheStrategy.RESOURCE
            ).into(holder.image)
    }

    class TrashBinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image: ImageView = itemView.findViewById(R.id.image_view)
        var isSelectedCheckbox: ImageView = itemView.findViewById(R.id.isSelectedCheckbox)
        var imageVideoThumbnail: ImageView = itemView.findViewById(R.id.imageView_video_logo)

        init {
            isSelectedCheckbox.visibility = View.GONE // or View.INVISIBLE
        }
    }
}


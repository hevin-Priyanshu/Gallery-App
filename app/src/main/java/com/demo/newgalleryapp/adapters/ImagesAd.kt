package com.demo.newgalleryapp.adapters

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.FavoriteImagesActivity
import com.demo.newgalleryapp.activities.MainScreenActivity
import com.demo.newgalleryapp.activities.OpenImageActivity
import com.demo.newgalleryapp.activities.TrashBinActivity
import com.demo.newgalleryapp.interfaces.ImageClickListener
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_CHANGES_IN_OPEN_IMAGE_ACTIVITY
import java.io.File

class ImagesAd(
    private val context: Activity,
    private var dataList: ArrayList<Any>,
    private val pos: Int,
    private val listener: ImageClickListener? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var isSelected = false
    var checkSelectedList: ArrayList<MediaModel> = ArrayList()

    override fun getItemViewType(position: Int): Int {
        return if (dataList[position] is String) {
            101
        } else {
            100
        }
    }

    fun updateSelectionState(isSelected: Boolean) {
        this.isSelected = isSelected
        if (!isSelected) {
            checkSelectedList.clear()
        }
        notifyDataSetChanged()
    }


    fun remove(position: Int) {
        if (position >= 0 && position < dataList.size) {
            dataList.removeAt(position)
            notifyItemChanged(position)
        }
    }

    fun removeItemsFromAdapter(itemsToRemove: List<String>) {
        val iterator = dataList.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item is MediaModel) {
                val model = item as MediaModel
                if (itemsToRemove.contains(model.path)) {
                    iterator.remove() // Remove the model from the list
                }
            } else if (item is String) {
                val path = item as String
                if (itemsToRemove.contains(path)) {
                    iterator.remove() // Remove the string from the list
                }
            }
            // Handle other types as needed
        }
        notifyDataSetChanged() // Notify the adapter of the data set change
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == 101) {
            MyViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
            )
        } else {
            InnerImageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_inner_image, parent, false)
            )
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }


    fun updateData(filteredData: ArrayList<Any>) {
        this.dataList = filteredData
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is MyViewHolder && dataList[position] is String) {

            holder.dateTextView.text = dataList[position].toString()

        } else if (holder is InnerImageViewHolder) {

            val imageModel = dataList[position] as MediaModel

            if (imageModel.isVideo) {

                holder.imageVideoThumbnail.visibility = View.VISIBLE
                val duration =
                    (context.application as AppClass).mainViewModel.formatDuration(imageModel.duration)
                holder.imageVideoTextView.text = duration

            } else {
                holder.imageVideoThumbnail.visibility = View.GONE
            }

            if (checkSelectedList.map { it.path }.contains(imageModel.path)) {
                holder.isSelectedCheckbox.setImageResource(R.drawable.right_tick_item)
            } else {
                holder.isSelectedCheckbox.setImageResource(R.drawable.empty_select_item_blur)
            }

//            holder.isSelectedCheckbox = checkSelectedList.map { it.path }.contains(list[position].path)
            // Handle click events on inner items
            var state = 0
            holder.imageView.setOnClickListener {

                if (isSelected) {
                    if (checkSelectedList.map { it.path }.contains(imageModel.path)) {
                        // If image is already selected, unselect it
                        checkSelectedList.remove(imageModel)
                        holder.isSelectedCheckbox.setImageResource(R.drawable.empty_select_item_blur)
                    } else {
                        // If image is not selected, select it
                        checkSelectedList.add(imageModel)
                        holder.isSelectedCheckbox.setImageResource(R.drawable.right_tick_item)
                    }
                    // Notify listener about the change in selection count
                    (context as MainScreenActivity).mediaFragment.counter(checkSelectedList.size)
                } else {

                    // If not in selection mode, start OpenImageActivity
                    val intent = Intent(context, OpenImageActivity::class.java)
                    if (pos >= 0) {
                        intent.putExtra("folderPosition", pos)
                    }
                    intent.putExtra("selectedImagePosition", position)
                    intent.putExtra("selectedModel", imageModel)
                    when (context) {
                        is MainScreenActivity -> {

                            state = 1
                        }

                        is FavoriteImagesActivity -> {
                            state = 2
                        }

                        is TrashBinActivity -> {
                            state = 3
                        }
                    }
                    intent.putExtra("currentState", state)
                    context.startActivityForResult(
                        intent, REQ_CODE_FOR_CHANGES_IN_OPEN_IMAGE_ACTIVITY
                    )
                }
            }


            holder.isSelectedCheckbox.visibility = if (isSelected) View.VISIBLE else View.GONE

            holder.imageView.setOnLongClickListener {
                // Automatically select the image on long-press
                isSelected = true
                if (!checkSelectedList.map { it.path }.contains(imageModel.path)) {

                    checkSelectedList.add(imageModel)

                    holder.isSelectedCheckbox.visibility = View.VISIBLE
                    (context as MainScreenActivity).mediaFragment.onLongClick()
                    context.mediaFragment.counter(checkSelectedList.size)

                    notifyDataSetChanged()
                }
                true
            }

            // Load and display the image using Glide or your preferred image-loading library
            Glide.with(context).load(File(imageModel.path))
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("TAG_E", "onLoadFailed: ${e?.message}")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("TAG_E", "onResourceReady: ${model.toString()}")
                        return false
                    }
                }).placeholder(R.drawable.placeholder).diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(holder.imageView)
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var innerRecyclerView: RecyclerView = itemView.findViewById(R.id.innerRecyclerView)
        var dateTextView: TextView = itemView.findViewById(R.id.dateTextView)

        init {
            innerRecyclerView.setHasFixedSize(true)
        }
    }

    class InnerImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.image_view_new)
        var imageVideoTextView: TextView = itemView.findViewById(R.id.videoTime_inner_item)
        var imageVideoThumbnail: LinearLayout =
            itemView.findViewById(R.id.imageView_video_logo_inner_item)
        var isSelectedCheckbox: ImageView = itemView.findViewById(R.id.isSelectedCheckbox)

        init {
            isSelectedCheckbox.visibility = View.GONE // or View.INVISIBLE
        }
    }

}
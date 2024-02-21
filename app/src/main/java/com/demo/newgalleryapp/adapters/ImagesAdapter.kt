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
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.FavoriteImagesActivity
import com.demo.newgalleryapp.activities.FolderImagesActivity.Companion.isUpdatedFolderActivity
import com.demo.newgalleryapp.activities.MainScreenActivity
import com.demo.newgalleryapp.activities.OpenImageActivity
import com.demo.newgalleryapp.activities.TrashBinActivity
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.interfaces.ImageClickListener
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE
import java.io.File

class ImagesAdapter(
    private val context: Activity,
    private var list: ArrayList<MediaModel>,
    private val pos: Int,
    private val listener: ImageClickListener? = null
) : RecyclerView.Adapter<ImagesAdapter.MyViewHolder>() {
    var isSelected = false
    var checkSelectedList: ArrayList<MediaModel> = ArrayList()
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.images_item, parent, false)
        return MyViewHolder(layout)
    }

    override fun getItemCount(): Int {
        return list.size
    }


    fun updateList(newList: List<MediaModel>) {
        this.list = newList.toMutableList() as ArrayList<MediaModel>
        notifyDataSetChanged()
    }

    fun updateSelectionState(isSelected: Boolean) {
        this.isSelected = isSelected
        if (!isSelected) {
            checkSelectedList.clear()
        }
        notifyDataSetChanged()
    }

    fun remove(position: Int) {
        if (position >= 0 && position < list.size) {
            list.removeAt(position)
//            notifyDataSetChanged()
            notifyItemRemoved(position)
        }

        if (list.isEmpty()) {
            isUpdatedFolderActivity = true
            val intent = Intent()
            context.setResult(Activity.RESULT_OK, intent)
            isUpdatedFolderActivity = false
            (context.application as AppClass).mainViewModel.getMediaFromInternalStorage()
            context.finish()
        }
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val listPos = list[position]

        // Get the width of the screen in pixels
        val screenWidth = context.resources.displayMetrics.widthPixels
        sharedPreferencesHelper = SharedPreferencesHelper(context)

        //get here height of images according to numbers of columns , by dividing them
        val imageViewWidth = screenWidth / sharedPreferencesHelper.getGridColumns()

        val layoutParams =
            RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageViewWidth)
        holder.image.layoutParams = layoutParams

        if (listPos.isVideo) {

            if (sharedPreferencesHelper.getGridColumns() == 5) {
                holder.imageVideoThumbnail.visibility = View.GONE
            } else {
                holder.imageVideoThumbnail.visibility = View.VISIBLE
            }

            holder.imageVideo.visibility = View.VISIBLE
            val duration =
                (context.application as AppClass).mainViewModel.formatDuration(listPos.duration)
            holder.imageVideoText.text = duration
        } else {
            holder.imageVideoThumbnail.visibility = View.GONE
            holder.imageVideo.visibility = View.GONE
        }

        if (checkSelectedList.map { it.path }.contains(listPos.path)) {
            holder.isSelectedCheckbox.setImageResource(R.drawable.right_tick_item)
        } else {
            holder.isSelectedCheckbox.setImageResource(R.drawable.empty_select_item_blur)
        }

        var state = 0
        holder.image.setOnClickListener {

            if (isSelected) {
                if (checkSelectedList.map { it.path }.contains(listPos.path)) {
                    // If image is already selected, unselect it
                    checkSelectedList.remove(listPos)
//                    holder.isSelectedCheckbox.isChecked = false
                    holder.isSelectedCheckbox.setImageResource(R.drawable.empty_select_item_blur)
                } else {
                    // If image is not selected, select it
                    checkSelectedList.add(listPos)
//                    holder.isSelectedCheckbox.isChecked = true
                    holder.isSelectedCheckbox.setImageResource(R.drawable.right_tick_item)
                }
                // Notify listener about the change in selection count
                listener?.counter(checkSelectedList.size)
            } else {

                // If not in selection mode, start OpenImageActivity
                val intent = Intent(context, OpenImageActivity::class.java)
                if (pos >= 0) {
                    intent.putExtra("folderPosition", pos)
                }
                intent.putExtra("selectedImagePosition", position)
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
                context.startActivityForResult(intent, REQ_CODE)
            }
        }

        holder.isSelectedCheckbox.visibility = if (isSelected) View.VISIBLE else View.GONE

        holder.image.setOnLongClickListener {
            // Automatically select the image on long-press
            isSelected = true
            if (!checkSelectedList.map { it.path }.contains(listPos.path)) {
                checkSelectedList.add(listPos)
//                holder.isSelectedCheckbox.isChecked = true
                holder.isSelectedCheckbox.visibility = View.VISIBLE
                listener?.onLongClick()
                listener?.counter(checkSelectedList.size)
                notifyDataSetChanged()
            }
            true
        }
        val image = listPos.path

        Glide.with(context).load(File(image).path).placeholder(R.drawable.placeholder)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("IA", "onResourceReady: ${model.toString()}")
                    return false
                }
            }).diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(holder.image)
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image: ImageView = itemView.findViewById(R.id.image_view)
        var imageVideoThumbnail: ImageView = itemView.findViewById(R.id.imageView_video_logo)
        var imageVideo: LinearLayout = itemView.findViewById(R.id.imageView_video_logo_image)
        var isSelectedCheckbox: ImageView = itemView.findViewById(R.id.isSelectedCheckbox)
        var imageVideoText: TextView = itemView.findViewById(R.id.videoTime)

        init {
            isSelectedCheckbox.visibility = View.GONE // or View.INVISIBLE
        }
    }


}
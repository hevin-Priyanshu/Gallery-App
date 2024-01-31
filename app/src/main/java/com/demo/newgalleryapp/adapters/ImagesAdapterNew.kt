package com.demo.newgalleryapp.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.demo.newgalleryapp.MainActivity
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.FavoriteImagesActivity
import com.demo.newgalleryapp.activities.MainScreenActivity
import com.demo.newgalleryapp.activities.OpenImageActivity
import com.demo.newgalleryapp.activities.TrashBinActivity
import com.demo.newgalleryapp.interfaces.ImageClickListener
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import java.io.File

class ImagesAdapterNew(
    private val context: Activity,
    private var dataList: List<Map.Entry<String, List<MediaModel>>>,
    private val listener: ImageClickListener? = null,
    private val spanCount: Int
) : RecyclerView.Adapter<ImagesAdapterNew.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return MyViewHolder(layout)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun updateList(newList: List<Map.Entry<String, List<MediaModel>>>) {
        this.dataList = newList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val entry = dataList[position]
        val date = entry.key
        val images = entry.value

        // Set the date
        holder.dateTextView.text = date

        // Set up a RecyclerView for images within each date
        val imageAdapter = InnerImageAdapter(context, images, listener)
//        holder.innerRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.innerRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
        holder.innerRecyclerView.adapter = imageAdapter
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var innerRecyclerView: RecyclerView = itemView.findViewById(R.id.innerRecyclerView)
        var dateTextView: TextView = itemView.findViewById(R.id.dateTextView)

        init {

            innerRecyclerView.setHasFixedSize(true)
        }
    }
}

class InnerImageAdapter(
    val context: Activity,
    private val imagesList: List<MediaModel>,
    private val listener: ImageClickListener? = null
) : RecyclerView.Adapter<InnerImageAdapter.InnerImageViewHolder>() {

    var isSelected = false
    var checkSelectedList: ArrayList<MediaModel> = ArrayList()
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inner_image, parent, false)
        return InnerImageViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imagesList.size
    }

    override fun onBindViewHolder(holder: InnerImageViewHolder, position: Int) {

        val screenWidth = context.resources.displayMetrics.widthPixels
        sharedPreferencesHelper = SharedPreferencesHelper(context)

        //get here height of images according to numbers of columns , by dividing them
        val imageViewWidth = screenWidth / sharedPreferencesHelper.getGridColumns()

        val layoutParams =
            RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageViewWidth)
        holder.imageView.layoutParams = layoutParams

        val imageModel = imagesList[position]

        // Set up your inner item view as needed

        // Handle click events on inner items
        var state = 0
        holder.imageView.setOnClickListener {

            if (isSelected) {
                if (checkSelectedList.map { it.path }.contains(imagesList[position].path)) {
                    // If image is already selected, unselect it
                    checkSelectedList.remove(imagesList[position])
                    holder.isSelectedCheckbox.isChecked = false
                } else {
                    // If image is not selected, select it
                    checkSelectedList.add(imagesList[position])
                    holder.isSelectedCheckbox.isChecked = true
                }
                // Notify listener about the change in selection count
                listener?.counter(checkSelectedList.size)
            } else {

                // If not in selection mode, start OpenImageActivity
                val intent = Intent(context, OpenImageActivity::class.java)
//                if (pos >= 0) {
//                    intent.putExtra("folderPosition", pos)
//                }
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
                context.startActivityForResult(intent, 11)
            }
            // Pass the imageModel or position to the listener for further actions
//            listener?.onImageClick(imageModel, position)
        }


        holder.isSelectedCheckbox.visibility = if (isSelected) View.VISIBLE else View.GONE


        holder.imageView.setOnLongClickListener {
            // Automatically select the image on long-press
            isSelected = true
            if (!checkSelectedList.map { it.path }.contains(imagesList[position].path)) {
                checkSelectedList.add(imagesList[position])
                holder.isSelectedCheckbox.isChecked = true
                holder.isSelectedCheckbox.visibility = View.VISIBLE
                listener?.onLongClick()
                listener?.counter(checkSelectedList.size)
                notifyDataSetChanged()
            }
            true
        }

        // Load and display the image using Glide or your preferred image-loading library
        Glide.with(holder.itemView.context).load(File(imageModel.path))
            .placeholder(R.drawable.placeholder).diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(holder.imageView)
    }


    class InnerImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.image_view_new)
        var isSelectedCheckbox: CheckBox = itemView.findViewById(R.id.isSelectedCheckbox)

        init {
            isSelectedCheckbox.visibility = View.GONE // or View.INVISIBLE
        }
    }

}

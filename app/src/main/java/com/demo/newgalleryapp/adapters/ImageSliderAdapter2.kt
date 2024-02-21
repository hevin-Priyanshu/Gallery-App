package com.demo.newgalleryapp.adapters

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.OpenImageActivity
import com.demo.newgalleryapp.activities.VideoViewActivity
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.classes.ZoomageView
import com.demo.newgalleryapp.interfaces.SetCropImages
import com.demo.newgalleryapp.models.MediaModel

class ImageSliderAdapter2(
    private val activity: Activity, private var modelList: ArrayList<MediaModel>
) : RecyclerView.Adapter<ImageSliderAdapter2.ImageSliderViewHolder>(), SetCropImages {

    inner class ImageSliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewForSlider: ZoomageView = itemView.findViewById(R.id.imageView)
        val imageVideo: ImageView = itemView.findViewById(R.id.imageViewVideo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageSliderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_open_one_image, parent, false)
        return ImageSliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageSliderViewHolder, position: Int) {
        val model = modelList[position]

        if (model.isVideo) {
            holder.imageVideo.visibility = View.VISIBLE
            holder.imageVideo.setOnClickListener {
                val videoPath = model.path
                val intent = Intent(activity, VideoViewActivity::class.java)
                intent.putExtra("currentVideoPosition", position)
                activity.startActivity(intent)
            }
        }

        Glide.with(activity).load(model.path).into(holder.imageViewForSlider)
    }

    override fun getItemCount(): Int {
        return modelList.size
    }

    fun remove(position: Int) {
        if (position >= 0 && position < modelList.size) {
            modelList.removeAt(position)
//            notifyDataSetChanged()
            notifyItemRemoved(position)
        }

        if (modelList.isEmpty()) {
            OpenImageActivity.anyChanges = true
            val intent = Intent()
            activity.setResult(Activity.RESULT_OK, intent)
            OpenImageActivity.anyChanges = false
            (activity.application as AppClass).mainViewModel.getMediaFromInternalStorage()
            activity.finish()
        }
    }

    fun setList(model: ArrayList<MediaModel>) {
        this.modelList = model
        notifyDataSetChanged()
    }

    override fun setImages(uri: Uri, imageViewForSlider: ZoomageView) {
        Glide.with(activity).load(uri).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean
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
        }).diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(imageViewForSlider)
    }
}

package com.demo.newgalleryapp.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.demo.newgalleryapp.ZoomageView
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.VideoViewActivity
import com.demo.newgalleryapp.interfaces.SetCropImages
import com.demo.newgalleryapp.models.MediaModel

class ImageSliderAdapter(private val context: Activity, private var modelList: ArrayList<MediaModel>) : PagerAdapter(),
    SetCropImages {

    private lateinit var imageViewForSlider: ZoomageView
    private lateinit var imageVideo: ImageView
    private var currentPosition = 0

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        currentPosition = position
        val view = LayoutInflater.from(context).inflate(R.layout.activity_open_one_image, container, false)

        imageViewForSlider = view.findViewById(R.id.imageView)

//       imageViewForSlider.setActivity(OpenImageActivity())
        imageVideo = view.findViewById(R.id.imageViewVideo)

        if (modelList[position].isVideo) {
            imageVideo.visibility = View.VISIBLE
            imageVideo.setOnClickListener {
                val videoPath = modelList[position].path
                val intent = Intent(context, VideoViewActivity::class.java)
                intent.putExtra("path", videoPath)
                context.startActivity(intent)
            }
        }

        Glide.with(context).load(modelList[position].path).into(imageViewForSlider)

        container.addView(view)
        return view
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    fun remove(position: Int) {
        if (position >= 0 && position < modelList.size) {
            modelList.removeAt(position)
            notifyDataSetChanged()
        }
    }

    override fun getCount(): Int {
        return modelList.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun setImages(uri: Uri) {
        Glide.with(context).load(uri).listener(object : RequestListener<Drawable> {
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


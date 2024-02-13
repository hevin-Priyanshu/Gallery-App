package com.demo.newgalleryapp.activities

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.OpenImageActivity.Companion.models
import com.demo.newgalleryapp.adapters.ImageSliderAdapter
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.utilities.CommonFunctions.setNavigationColor

class SlideShowActivity : AppCompatActivity() {

    private lateinit var openImageActivity: OpenImageActivity
    private lateinit var slideShow: ImageView
    lateinit var viewPager: ViewPager
    private val handler = Handler()
    private lateinit var imagesSliderAdapter: ImageSliderAdapter
    private var currentImageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slide_show)

        setNavigationColor(window, Color.BLACK)

        slideShow = findViewById(R.id.slideShowImage)
        viewPager = findViewById(R.id.viewPager_slideShow)
        openImageActivity = OpenImageActivity()

        val slideImagePath = intent.getStringExtra("SlideImagePath")
        currentImageIndex = intent.getIntExtra("SlideImagePosition", 0)
        val isSlideShow = intent.hasExtra("FromSlideShow")

//        val mainScreenActivity = intent.getBooleanExtra("slideShowSelectedMainScreenActivity", false)

        Glide.with(this).load(slideImagePath).placeholder(R.drawable.placeholder)
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
            }).diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(slideShow)

//        if (mainScreenActivity) {
//            val objectList = intent.getSerializableExtra("pathsList") as ArrayList<MediaModel>
//            imagesSliderAdapter = ImageSliderAdapter(this, objectList)
//            viewPager.adapter = imagesSliderAdapter
//            viewPager.setCurrentItem(currentImageIndex, false)
//            startSlideshow(currentImageIndex, objectList)
//        } else {

        if (isSlideShow) {
            val newModels = (application as AppClass).mainViewModel.allMediaList
            imagesSliderAdapter = ImageSliderAdapter(this, newModels)
            viewPager.adapter = imagesSliderAdapter
            viewPager.setCurrentItem(currentImageIndex, false)
            startSlideshow(0, newModels)

        } else {
            imagesSliderAdapter = ImageSliderAdapter(this, models as ArrayList)
            viewPager.adapter = imagesSliderAdapter
            viewPager.setCurrentItem(currentImageIndex, false)
            startSlideshow(currentImageIndex, models)
        }

//        }


    }

    private fun startSlideshow(currentIndex: Int, models: List<MediaModel>) {

        // Define the interval for changing images (2 seconds in this case)
        val slideshowInterval = 2000L

        val runnable = object : Runnable {
            override fun run() {
                // here checking the currentImageIndex is equal to size of all media list then , reset currentImageIndex to 0
                if (currentIndex == models.size) {
                    return
                }
                // Increment the index for the next image
                viewPager.setCurrentItem(this@SlideShowActivity.currentImageIndex++, true)
                // Schedule the next image change after the interval
                handler.postDelayed(this, slideshowInterval)
            }
        }
        // Start the initial image change
        handler.postDelayed(runnable, slideshowInterval)
    }
}
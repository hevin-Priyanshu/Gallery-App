package com.demo.newgalleryapp.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.ImageSliderAdapter2
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.utilities.CommonFunctions.setNavigationColor

class SlideShowActivity : AppCompatActivity() {

    private lateinit var openImageActivity: OpenImageActivity
    private lateinit var slideShow: ImageView
    lateinit var viewPager: ViewPager2
    private val handler = Handler()
    private lateinit var imagesSliderAdapter: ImageSliderAdapter2
    private var currentImageIndex = -1
    private var tempList: ArrayList<MediaModel> = arrayListOf()
    //TODO need to remove below line
    // lateinit var models: List<MediaModel>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slide_show)

        setNavigationColor(window, Color.BLACK)

        slideShow = findViewById(R.id.slideShowImage)
        viewPager = findViewById(R.id.viewPager_slideShow)
        openImageActivity = OpenImageActivity()

        currentImageIndex = intent.getIntExtra("SlideImagePosition", 0)

        tempList = (application as AppClass).mainViewModel.tempPhotoList
        val isSlideShow = intent.hasExtra("FromSlideShow")

//        Glide.with(this).load(slideImagePath).placeholder(R.drawable.placeholder)
//            .listener(object : RequestListener<Drawable> {
//                override fun onLoadFailed(
//                    e: GlideException?,
//                    model: Any?,
//                    target: Target<Drawable>?,
//                    isFirstResource: Boolean
//                ): Boolean {
//                    return false
//                }
//
//                override fun onResourceReady(
//                    resource: Drawable?,
//                    model: Any?,
//                    target: Target<Drawable>?,
//                    dataSource: DataSource?,
//                    isFirstResource: Boolean
//                ): Boolean {
//                    Log.d("IA", "onResourceReady: ${model.toString()}")
//                    return false
//                }
//            }).diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(slideShow)


        if (isSlideShow) {
            imagesSliderAdapter = ImageSliderAdapter2(this, tempList)
            viewPager.adapter = imagesSliderAdapter
            viewPager.setCurrentItem(0, false)
            startSlideshow(0, tempList)

        } else {
            imagesSliderAdapter = ImageSliderAdapter2(this, tempList)
            viewPager.adapter = imagesSliderAdapter
            viewPager.setCurrentItem(currentImageIndex, false)
            startSlideshow(currentImageIndex, tempList)
        }

    }

    private fun startSlideshow(currentIndex: Int, models: ArrayList<MediaModel>) {

        // Define the interval for changing images (2 seconds in this case)
        val slideshowInterval = 2000L
        var currentImageIndex = currentIndex // Initialize the current image index

        val runnable = object : Runnable {
            override fun run() {
                // Check if the current index is within the bounds of the list
                if (currentImageIndex < models.size) {
                    viewPager.setCurrentItem(currentImageIndex, true)
                    currentImageIndex++ // Increment the index after setting the current item
                    handler.postDelayed(this, slideshowInterval)
                }
            }
        }

        // Start the initial image change
        handler.postDelayed(runnable, slideshowInterval)

        // Set up GestureDetector to detect touch events on the ViewPager
//        val gestureDetector =
//            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
//                override fun onDown(e: MotionEvent): Boolean {
//                    // Return true to indicate that the touch event is handled
//                    return true
//                }
//
//                override fun onSingleTapUp(e: MotionEvent): Boolean {
//                    // Stop the slideshow when the user taps on the ViewPager
//                    handler.removeCallbacks(runnable)
//                    return true
//                }
//            })


        slideShow.setOnClickListener {
            handler.removeCallbacks(runnable)
        }
    }

}
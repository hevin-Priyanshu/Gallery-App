package com.demo.newgalleryapp.interfaces

import android.net.Uri
import com.demo.newgalleryapp.classes.ZoomageView

interface SetCropImages {
    fun setImages(uri: Uri, imageViewForSlider: ZoomageView)
}
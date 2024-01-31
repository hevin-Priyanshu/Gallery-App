package com.demo.newgalleryapp.interfaces

import android.widget.CheckBox
import com.demo.newgalleryapp.models.MediaModel

interface ImageClickListener {
//    fun onClick()
    fun onLongClick()
    fun counter(select: Int)
}
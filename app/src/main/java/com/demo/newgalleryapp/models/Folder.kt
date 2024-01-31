package com.demo.newgalleryapp.models

import java.io.Serializable

data class Folder(
    val name: String, val models: ArrayList<MediaModel>
): Serializable
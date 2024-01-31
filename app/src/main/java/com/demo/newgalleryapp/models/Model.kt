package com.demo.newgalleryapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

data class Model(
    val id: Long,
    val path: String,
    val imageName: String
)

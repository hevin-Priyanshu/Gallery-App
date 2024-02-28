package com.demo.newgalleryapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "favorites")
data class MediaModel (
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    var displayName: String,
    val path: String,
    val mimeType: String,
    val duration: Long,
    val size: Long,
    val date: Long,
    val isVideo: Boolean
): Serializable

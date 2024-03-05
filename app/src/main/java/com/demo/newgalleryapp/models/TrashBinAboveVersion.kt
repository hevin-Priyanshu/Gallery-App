package com.demo.newgalleryapp.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "trashBin")
data class TrashBinAboveVersion(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val date: Long,
    val isVideo: Boolean
): Serializable

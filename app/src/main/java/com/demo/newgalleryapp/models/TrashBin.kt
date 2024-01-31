package com.demo.newgalleryapp.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "trashBin")
data class TrashBin(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val currentPath: String,
    val destinationImagePath: String,
    val deletionTimestamp: Long,
): Serializable

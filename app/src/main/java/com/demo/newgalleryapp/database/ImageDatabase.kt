package com.demo.newgalleryapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.models.TrashBin
import com.demo.newgalleryapp.models.TrashBinAboveVersion
import com.demo.newgalleryapp.models.UriConverter

@Database(entities = [MediaModel::class, TrashBinAboveVersion::class], version = 2, exportSchema = false)
@TypeConverters(UriConverter::class)
abstract class ImagesDatabase : RoomDatabase() {
    abstract fun favoriteImageDao(): ImageDao

    companion object {
        private var INSTANCE: ImagesDatabase? = null
        fun getDatabase(context: Context): ImagesDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, ImagesDatabase::class.java, "image_database"
            ).allowMainThreadQueries().fallbackToDestructiveMigration().build()
                .also { INSTANCE = it }
        }
    }
}
package com.demo.newgalleryapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.models.TrashBin

@Database(entities = [MediaModel::class, TrashBin::class], version = 2, exportSchema = false)
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
package com.demo.newgalleryapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.models.TrashBin

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertFavorite(mediaModel: MediaModel)

    @Delete
    fun deleteFavorite(mediaModel: MediaModel)

    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): LiveData<List<MediaModel>>

    @Query("SELECT * FROM favorites WHERE path = :path")
    fun getModelByFile(path: String): MediaModel

    @Query("SELECT COUNT(*) FROM favorites")
    fun getRowCount(): Int


    ///  TRASH BIN WORK
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDeleteImage(trashBin: TrashBin)

    @Query("SELECT * FROM trashBin")
    fun getAllDeleteImages(): LiveData<List<TrashBin>>

    @Delete
    fun deleteImages(trashBin: TrashBin)

    @Query("SELECT * FROM trashBin WHERE deletionTimestamp < :timestamp")
    fun selectImages(timestamp: Long): List<TrashBin>
}
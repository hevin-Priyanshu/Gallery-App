package com.demo.newgalleryapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.models.TrashBin
import com.demo.newgalleryapp.models.TrashBinAboveVersion

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertFavorite(mediaModel: MediaModel)

    @Delete
    fun deleteFavorite(mediaModel: MediaModel)

    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): LiveData<List<MediaModel>>

    @Query("DELETE FROM favorites WHERE path = :path")
    fun deleteFavorites(path: String)

    @Query("SELECT * FROM favorites WHERE path = :path")
    fun getModelByFile(path: String): MediaModel

    @Query("SELECT COUNT(*) FROM favorites")
    fun getRowCount(): Int


    ///  TRASH BIN WORK
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDeleteImage(trashBin: TrashBinAboveVersion)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDeleteMultipleImage(trashBin: ArrayList<TrashBinAboveVersion>)

    @Query("SELECT * FROM trashBin")
    fun getAllDeleteImages(): LiveData<List<TrashBinAboveVersion>>

    @Delete
    fun deleteImages(trashBin: TrashBinAboveVersion)

    @Query("SELECT * FROM trashBin WHERE date < :timestamp")
    fun selectImages(timestamp: Long): List<TrashBinAboveVersion>
}
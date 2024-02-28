package com.demo.newgalleryapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.models.TrashBinAboveVersion

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeleteImage(trashBin: TrashBinAboveVersion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeleteMultipleImage(trashBin: ArrayList<TrashBinAboveVersion>)

    @Query("SELECT * FROM trashBin  ORDER BY date DESC")
    fun getAllDeleteImages(): LiveData<List<TrashBinAboveVersion>>

    @Delete
    fun deleteImages(trashBin: TrashBinAboveVersion)

    @Query("SELECT * FROM trashBin WHERE date < :timestamp")
    fun selectImages(timestamp: Long): List<TrashBinAboveVersion>

    @Query("SELECT * FROM trashBin WHERE date = :timestamp")
    fun timeStamp(timestamp: Long): TrashBinAboveVersion

    @Query("SELECT * FROM trashBin WHERE path = :absolutePath")
    fun getImageByPath(absolutePath: String): TrashBinAboveVersion

    @Query("SELECT * FROM trashBin WHERE path = :path LIMIT 1")
    fun getTrashBinItemByPath(path: String): TrashBinAboveVersion

}
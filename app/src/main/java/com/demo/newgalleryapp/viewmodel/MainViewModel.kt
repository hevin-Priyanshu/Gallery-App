package com.demo.newgalleryapp.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.demo.newgalleryapp.database.ImagesDatabase
import com.demo.newgalleryapp.models.Folder
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.models.TrashBinAboveVersion
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import com.demo.newgalleryapp.utilities.CommonFunctions.ERROR_TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var _allData: MutableLiveData<List<MediaModel>> = MutableLiveData()
    private var _tempAllTrashData: MutableLiveData<List<TrashBinAboveVersion>> = MutableLiveData()
    private var _photosData: MutableLiveData<List<MediaModel>> = MutableLiveData()
    private var _videosData: MutableLiveData<List<MediaModel>> = MutableLiveData()

    val sharedPreferencesHelper: SharedPreferencesHelper = SharedPreferencesHelper(getApplication())

    val allData: LiveData<List<MediaModel>> get() = _allData
    val tempAllTrashData: LiveData<List<TrashBinAboveVersion>> get() = _tempAllTrashData
    val photosData: LiveData<List<MediaModel>> get() = _photosData
    val videosData: LiveData<List<MediaModel>> get() = _videosData

    var allMediaList: ArrayList<MediaModel> = getMediaFromInternalStorage()
    var tempPhotoList: ArrayList<MediaModel> = ArrayList()
    var tempVideoList: ArrayList<MediaModel> = ArrayList()
    val folderList: ArrayList<Folder> = ArrayList()

    init {
        allMediaList = getMediaFromInternalStorage()
    }

    var flag: Boolean = false
    var flagForTrashBinActivity: Boolean = false

    fun getMediaFromInternalStorage(): ArrayList<MediaModel> {

        val mediaList = ArrayList<MediaModel>()

        viewModelScope.launch(Dispatchers.IO) {

            val uriForImages: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uriForVideos: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            // Define the columns you want to retrieve from the MediaStore
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.MIME_TYPE,
                // MediaStore.MediaColumns.DURATION,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED
            )

            val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

            // Query for images
            val imageList = ArrayList<MediaModel>()
            queryMediaStore(uriForImages, projection, null, null, sortOrder, imageList, false)
//        allImagesSize = imageList.size
            _photosData.postValue(imageList)

            // Query for videos
            val videoList = ArrayList<MediaModel>()
            queryMediaStore(uriForVideos, projection, null, null, sortOrder, videoList, true)
//        allVideosSize = videoList.size
            _videosData.postValue(videoList)

            mediaList.addAll(imageList)
            mediaList.addAll(videoList)

            _allData.postValue(mediaList)

        }
        return mediaList
    }

    private fun queryMediaStore(
        contentUri: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
        mediaList: MutableList<MediaModel>,
        isVideo: Boolean,
    ) {

        val context: Context = getApplication()
        // Perform the query using the content resolver
        val cursor = context.contentResolver.query(
            contentUri, projection, selection, selectionArgs, sortOrder
        )

        // Check if the cursor is not null and contains entries
        cursor?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val displayName =
                    c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                val data = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                val mimeType =
                    c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
//                val duration = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION))
                val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                val dateAdded =
                    c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))

                // Create a MediaModel object and add it to the list
                val mediaModel =
                    MediaModel(id, displayName, data, mimeType, 0L, size, dateAdded, isVideo)
                mediaList.add(mediaModel)
            }
        }

    }

    fun moveImageInTrashBin(imagePathToDelete: String, isVideoOrNot: Boolean) {

        val context: Context = getApplication()

        if (imagePathToDelete.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val imagePath = File(imagePathToDelete)
            val trashDirectory = createTrashDirectory()

            if (imagePath.exists()) {

                val destinationImage = File(trashDirectory, imagePath.name)

                if (destinationImage.exists()) {
                    destinationImage.delete()
                }
                imagePath.copyTo(destinationImage)
                val deletionTimestamp = System.currentTimeMillis()// current time in millisec


                val trashBinModel = TrashBinAboveVersion(
                    0,
                    destinationImage.toUri(),
                    imagePath.toString(),
                    imagePath.name,
                    "",
                    deletionTimestamp.toString(), isVideoOrNot
                )

                // Here adding current and destination path in database , for showing item in trash bin
                ImagesDatabase.getDatabase(context).favoriteImageDao()
                    .insertDeleteImage(trashBinModel)

                // Here Deleting favorite items that was present in favorite activity , because we removing files on trash...
//                deleteFavoriteItems(context, imagePathToDelete)
                ImagesDatabase.getDatabase(context).favoriteImageDao()
                    .deleteFavorites(imagePathToDelete)

                imagePath.deleteRecursively()
                scanFile(context, imagePath)

                val updatedPhotoList = _photosData.value?.toMutableList()
                updatedPhotoList?.removeAll { it.path == imagePathToDelete }
                _photosData.postValue(updatedPhotoList)

                // Notify observers about the deletion
                _photosData.value?.let { _photosData.postValue(it) }

                val updatedVideoList = _videosData.value?.toMutableList()
                updatedVideoList?.removeAll { it.path == imagePathToDelete }
                _videosData.postValue(updatedVideoList)

                // Notify observers about the deletion
                _videosData.value?.let { _videosData.postValue(it) }

            } else {
                Log.e(ERROR_TAG, "moveImageInTrashBin: Error deleting file!!")
            }

        }
    }

    fun moveMultipleImagesInTrashBin(imagePathsToDelete: List<String>) {

        if (imagePathsToDelete.isEmpty()) {
            return
        }

        viewModelScope.launch {

            val context: Context = getApplication()

            val trashDirectory = createTrashDirectory()

            val updatedList = _allData.value?.toMutableList()

            for (imagePathToDelete in imagePathsToDelete) {
                val imagePath = File(imagePathToDelete)

                if (imagePath.exists()) {
                    val destinationImage = File(trashDirectory, imagePath.name)

                    if (destinationImage.exists()) {
                        destinationImage.delete()
                    }

                    imagePath.copyTo(destinationImage)

                    val deletionTimestamp = System.currentTimeMillis() // current time in millisec

                    val trashBinModel = TrashBinAboveVersion(
                        0,
                        destinationImage.toUri(),
                        imagePath.toString(),
                        imagePath.name,
                        "",
                        deletionTimestamp.toString(), false
                    )

                    ImagesDatabase.getDatabase(context).favoriteImageDao()
                        .insertDeleteImage(trashBinModel)

                    // Here Deleting favorite items that was present in favorite activity , because we removing files from trash...
                    ImagesDatabase.getDatabase(context).favoriteImageDao()
                        .deleteFavorites(imagePathToDelete)

                    imagePath.deleteRecursively()
                    scanFile(context, imagePath)
                    // Remove the deleted image from the list
                    updatedList?.removeAll { it.path == imagePathToDelete }
//                Toast.makeText(getApplication(), "Move To Trash bin Successfully!!", Toast.LENGTH_SHORT)
//                    .show()
                } else {
                    // Handle the case where the file doesn't exist
                    Log.e(
                        "error",
                        "moveMultipleImagesInTrashBin: Error deleting file: $imagePathToDelete"
                    )
                }
            }
            // Notify observers about the deletions
            _allData.postValue(updatedList)
        }
    }

    //  This function delete images and video permanently
    fun deleteImage(imagePathToDelete: TrashBinAboveVersion) {
        val context: Context = getApplication()
        val deletedImage = File(imagePathToDelete.uri.path!!)
        if (deletedImage.exists()) {
            // Delete the file from the specified folder
            deletedImage.delete()
            // Delete the entry from the database
            ImagesDatabase.getDatabase(context).favoriteImageDao().deleteImages(imagePathToDelete)
            // Notify UI about the deletion
            Toast.makeText(getApplication(), "Delete Successful!!", Toast.LENGTH_SHORT).show()
        } else {
            // Notify user that the file doesn't exist
            Toast.makeText(getApplication(), "File not found!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteMultiple(imagePathToDelete: ArrayList<TrashBinAboveVersion>) {
        val context: Context = getApplication()

        for (delete in imagePathToDelete) {

            val deletedImage = File(delete.uri.path!!)

            if (deletedImage.exists()) {
                // Delete the file from the specified folder
                deletedImage.delete()
                // Delete the entry from the database
                ImagesDatabase.getDatabase(context).favoriteImageDao().deleteImages(delete)
                // Notify UI about the deletion
                Toast.makeText(getApplication(), "Delete Successful!!", Toast.LENGTH_SHORT).show()
            } else {
                // Notify user that the file doesn't exist
                Toast.makeText(getApplication(), "File not found!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun restoreImage(trashBinModel: TrashBinAboveVersion) {
        val context: Context = getApplication()

        viewModelScope.launch {

            // Get the deleted image information from the database
            val deletedImage = File(trashBinModel.uri.path!!)
            val originalImagePath = File(trashBinModel.path)

            if (deletedImage.exists()) {
                deletedImage.copyTo(originalImagePath, overwrite = true)
//          org.apache.commons.io.FileUtils.copyFile(deletedImage, originalImagePath)

                deletedImage.deleteRecursively()

                scanFile(context, originalImagePath)
                // Delete the entry from the database
                ImagesDatabase.getDatabase(context).favoriteImageDao().deleteImages(trashBinModel)

                // Notify observers about the restoration
                val updatedList = _allData.value?.toMutableList()
                updatedList?.add(
                    MediaModel(
                        0, originalImagePath.toString(), deletedImage.toString(), "", 0, 0, 0, false
                    )
                )
                // Add the restored image to the list
                _allData.postValue(updatedList)
            } else {
                Log.e("error", "restoreImage: Error restoring file. File not found in trash!!")
            }

        }
    }

    fun restoreMultipleImagesVideos(trashBinModel: ArrayList<TrashBinAboveVersion>) {
        val context: Context = getApplication()
        viewModelScope.launch {
            // Get the deleted image information from the database
            for (trashBin in trashBinModel) {
                val deletedImagePath = File(trashBin.uri.path!!)
                val originalImagePath = File(trashBin.path)

                if (deletedImagePath.exists()) {
                    deletedImagePath.copyTo(originalImagePath, overwrite = true)

                    //org.apache.commons.io.FileUtils.copyFile(deletedImage, originalImagePath)
                    deletedImagePath.deleteRecursively()

                    scanFile(context, originalImagePath)
                    // Delete the entry from the database
                    ImagesDatabase.getDatabase(context).favoriteImageDao().deleteImages(trashBin)
                    // Notify observers about the restoration
                    val updatedList = _allData.value?.toMutableList()
                    updatedList?.add(
                        MediaModel(
                            0,
                            originalImagePath.toString(),
                            deletedImagePath.toString(),
                            "",
                            0,
                            0,
                            0,
                            false
                        )
                    ) // Add the restored image to the list
                    _allData.postValue(updatedList)

                } else {
                    Log.e(
                        "error",
                        "restoreMultipleImagesVideos: Error restoring file. File not found in trash!!"
                    )
                }
                ////////////
            }
            ///////////
        }
        ////////////
    }

    fun scanFile(context: Context, file: File) {
        MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
    }

    // This function create new folder in directory to store the images
    private fun createTrashDirectory(): File {
        val trashDirectory =
            File(getApplication<Application>().getExternalFilesDir(null), ".TrashBin")
        if (!trashDirectory.exists()) {
            trashDirectory.mkdirs()
        }
        return trashDirectory
    }

    // This function is to share(send) only single images
    fun shareImage(imageUri: Uri, context: Context) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/* video/*"
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(shareIntent, "Share Image or Video"))
    }

    // This function is to share(send) only multiple images or video
    fun shareMultipleImages(uris: ArrayList<Uri>, context: Context) {

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "image/* video/*"// Set the MIME type according to your file type
            // Add the files to the intent
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
        // Grant read permissions to the receiving app
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Start the activity to share the files
        context.startActivity(Intent.createChooser(shareIntent, "Share Image or Video"))
    }

    //  THIS ALL CODE FOR ANDROID 11 AND ABOVE
    @RequiresApi(Build.VERSION_CODES.R)
    fun queryTrashedMediaOnDevice(): List<TrashBinAboveVersion> {

        val trashItemsList = ArrayList<TrashBinAboveVersion>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val bundle = Bundle()
        bundle.putInt("android:query-arg-match-trashed", 1)
        bundle.putString(
            "android:query-arg-sql-selection", "${MediaStore.MediaColumns.IS_TRASHED} = 1"
        )
        bundle.putString(
            "android:query-arg-sql-sort-order", "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )

        val collectionOfImages: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val collectionOfVideos: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }


        val imageList = ArrayList<TrashBinAboveVersion>()
        queryMediaStoreForAPI(collectionOfImages, projection, bundle, imageList, false)

        // Query for videos
        val videoList = ArrayList<TrashBinAboveVersion>()
        queryMediaStoreForAPI(collectionOfVideos, projection, bundle, videoList, true)

//        val query = context.contentResolver.query(collection, projection, bundle, null)

        trashItemsList.addAll(imageList)
        trashItemsList.addAll(videoList)

        _tempAllTrashData.postValue(trashItemsList)

        return trashItemsList
    }

    private fun queryMediaStoreForAPI(
        collectionOfImages: Uri,
        projection: Array<String>,
        bundle: Bundle,
        trashList: ArrayList<TrashBinAboveVersion>,
        isVideo: Boolean
    ) {
        val context: Context = getApplication()

        context.contentResolver.query(collectionOfImages, projection, bundle, null)?.use { cursor ->

            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val path =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH))
                val name =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                val size =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                val date =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))


                val uri = ContentUris.withAppendedId(collectionOfImages, id)

                // Discard invalid images that might exist on the device
                if (size == null) {
                    continue
                }
                trashList.add(
                    TrashBinAboveVersion(id, uri, path, name, size, date, isVideo)
                )
            }
            cursor.close()
        }
    }

}

package com.demo.newgalleryapp.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import com.demo.newgalleryapp.utilities.CommonFunctions
import com.demo.newgalleryapp.utilities.CommonFunctions.ERROR_TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit


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
            val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATA,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.DURATION,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED
                )
            } else {
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATA,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED
                )
            }

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

                var duration = if (isVideo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION))
                } else {
                    0L
                }

                val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                val dateAdded =
                    c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))

                // Discard invalid images that might exist on the device
                if (size == null) {
                    continue
                }

                if (duration == null) {
                    continue
                }
                // Create a MediaModel object and add it to the list
                val mediaModel =
                    MediaModel(id, displayName, data, mimeType, duration, size, dateAdded, isVideo)
                mediaList.add(mediaModel)
            }
        }


        cursor?.close()
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

                val destinationFileName = "${imagePath.name}.trash"
                val destinationImage = File(trashDirectory, destinationFileName)

//                val destinationImage = File(trashDirectory, imagePath.name)

                if (destinationImage.exists()) {
                    destinationImage.deleteRecursively()
                }
                imagePath.copyTo(destinationImage, overwrite = true)

//                val deletionTimestamp = System.currentTimeMillis()// current time in millisec

                val deletionTimestamp = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
//
//                val thirtyDaysInMillis = (30 * 24 * 60 * 60 * 1000) // 30 days in milliseconds
//                val deletionTimestampWithThirtyDays = deletionTimestamp + thirtyDaysInMillis


                // After moving the image to the trash
                val trashBinModel = TrashBinAboveVersion(
                    0, // Assuming the ID is auto-generated
                    destinationImage.toUri(), // URI of the trashed image
                    imagePath.absolutePath, // Path of the trashed image
                    destinationFileName, // Name of the trashed image
                    "", // You might want to add additional information such as description or metadata
                    deletionTimestamp, isVideoOrNot
                )

//                val trashBinModel = TrashBinAboveVersion(
//                    0,
//                    destinationImage.toUri(),
//                    imagePath.toString(),
//                    imagePath.name,
//                    "",
//                    deletionTimestamp.toString(),
//                    isVideoOrNot
//                )

                val existingTrashItem = ImagesDatabase.getDatabase(context).favoriteImageDao()
                    .getTrashBinItemByPath(imagePathToDelete)

                if (existingTrashItem != null) {
                    ImagesDatabase.getDatabase(context).favoriteImageDao()
                        .deleteImages(trashBinModel)
                }

                // Here adding current and destination path in database , for showing item in trash bin
                ImagesDatabase.getDatabase(context).favoriteImageDao()
                    .insertDeleteImage(trashBinModel)

                // Here Deleting favorite items that was present in favorite activity , because we removing files on trash...
                ImagesDatabase.getDatabase(context).favoriteImageDao()
                    .deleteFavorites(imagePathToDelete)

//                imagePath.deleteRecursively()

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                    imagePath.delete()
                    context.sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imagePath)
                        )
                    )
                } else {
                    imagePath.deleteRecursively()
                }
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

    fun moveMultipleImagesInTrashBin(imagePathsToDelete: List<String>, isVideos: List<Boolean>) {

        if (imagePathsToDelete.isEmpty()) {
            return
        }

//        val pairOfLists = listOf(imagePathsToDelete, isVideos)

        viewModelScope.launch {

            val context: Context = getApplication()

            val trashDirectory = createTrashDirectory()

            val updatedList = _allData.value?.toMutableList()


            imagePathsToDelete.zip(isVideos).forEach { pair ->

                val imagePath = File(pair.first)

                if (imagePath.exists()) {

                    val destinationFileName = "${imagePath.name}.trash"
                    val destinationImage = File(trashDirectory, destinationFileName)
//                    val destinationImage = File(trashDirectory, imagePath.name)

                    if (destinationImage.exists()) {
                        destinationImage.deleteRecursively()
                    }

                    imagePath.copyTo(destinationImage)

//                    val deletionTimestamp = System.currentTimeMillis() // current time in millisec

                    val deletionTimestamp = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)

//                    val thirtyDaysInMillis = (30 * 24 * 60 * 60 * 1000) // 30 days in milliseconds
//                    val deletionTimestampWithThirtyDays = deletionTimestamp + thirtyDaysInMillis


                    val trashBinModel = TrashBinAboveVersion(
                        0, // Assuming the ID is auto-generated
                        destinationImage.toUri(), // URI of the trashed image
                        imagePath.absolutePath, // Path of the trashed image
                        destinationFileName, // Name of the trashed image
                        "", // You might want to add additional information such as description or metadata
                        deletionTimestamp, pair.second
                    )

                    ImagesDatabase.getDatabase(context).favoriteImageDao()
                        .deleteImages(trashBinModel)

                    ImagesDatabase.getDatabase(context).favoriteImageDao()
                        .insertDeleteImage(trashBinModel)

                    // Here Deleting favorite items that was present in favorite activity , because we removing files from trash...
                    ImagesDatabase.getDatabase(context).favoriteImageDao()
                        .deleteFavorites(pair.first)


                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                        imagePath.delete()
                        context.sendBroadcast(
                            Intent(
                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imagePath)
                            )
                        )
                    } else {
                        imagePath.deleteRecursively()
                    }
                    scanFile(context, imagePath)
                    // Remove the deleted image from the list
                    updatedList?.removeAll { it.path == pair.first }
//                Toast.makeText(getApplication(), "Move To Trash bin Successfully!!", Toast.LENGTH_SHORT)
//                    .show()
                } else {
                    // Handle the case where the file doesn't exist
                    Log.e(
                        "error", "moveMultipleImagesInTrashBin: Error deleting file: ${pair.first}"
                    )
                }
            }


//            for (imagePathToDelete in imagePathsToDelete) {
//                val imagePath = File(imagePathToDelete)
//
//                if (imagePath.exists()) {
//
//                    val destinationFileName = "${imagePath.name}.trash"
//                    val destinationImage = File(trashDirectory, destinationFileName)
////                    val destinationImage = File(trashDirectory, imagePath.name)
//
//                    if (destinationImage.exists()) {
//                        destinationImage.deleteRecursively()
//                    }
//
//                    imagePath.copyTo(destinationImage)
//
////                    val deletionTimestamp = System.currentTimeMillis() // current time in millisec
//
//                    val deletionTimestamp = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
//
////                    val thirtyDaysInMillis = (30 * 24 * 60 * 60 * 1000) // 30 days in milliseconds
////                    val deletionTimestampWithThirtyDays = deletionTimestamp + thirtyDaysInMillis
//
//
//                    val trashBinModel = TrashBinAboveVersion(
//                        0, // Assuming the ID is auto-generated
//                        destinationImage.toUri(), // URI of the trashed image
//                        imagePath.absolutePath, // Path of the trashed image
//                        destinationFileName, // Name of the trashed image
//                        "", // You might want to add additional information such as description or metadata
//                        deletionTimestamp, false
//                    )
//
//                    ImagesDatabase.getDatabase(context).favoriteImageDao()
//                        .deleteImages(trashBinModel)
//
//                    ImagesDatabase.getDatabase(context).favoriteImageDao()
//                        .insertDeleteImage(trashBinModel)
//
//                    // Here Deleting favorite items that was present in favorite activity , because we removing files from trash...
//                    ImagesDatabase.getDatabase(context).favoriteImageDao()
//                        .deleteFavorites(imagePathToDelete)
//
//
//                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
//                        imagePath.delete()
//                        context.sendBroadcast(
//                            Intent(
//                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imagePath)
//                            )
//                        )
//                    } else {
//                        imagePath.deleteRecursively()
//                    }
//                    scanFile(context, imagePath)
//                    // Remove the deleted image from the list
//                    updatedList?.removeAll { it.path == imagePathToDelete }
////                Toast.makeText(getApplication(), "Move To Trash bin Successfully!!", Toast.LENGTH_SHORT)
////                    .show()
//                } else {
//                    // Handle the case where the file doesn't exist
//                    Log.e(
//                        "error",
//                        "moveMultipleImagesInTrashBin: Error deleting file: $imagePathToDelete"
//                    )
//                }
//            }
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
            } else {
                // Notify user that the file doesn't exist
                Toast.makeText(getApplication(), "File not found!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun restoreImage(trashBinModel: TrashBinAboveVersion) {
        val context: Context = getApplication()

        viewModelScope.launch {

            try {
                // Get the deleted image information from the database

                val deletedImage = File(trashBinModel.uri.path ?: "")
                val originalImagePath = File(trashBinModel.path)


//                val deletedImage = File(trashBinModel.uri.path!!)
//                val originalImagePath = File(trashBinModel.path)

                if (deletedImage.exists()) {
                    deletedImage.copyTo(originalImagePath, overwrite = true)

                    deletedImage.deleteRecursively()
                    scanFile(context, originalImagePath)
                    // Delete the entry from the database
                    ImagesDatabase.getDatabase(context).favoriteImageDao()
                        .deleteImages(trashBinModel)

                    // Notify observers about the restoration
                    val updatedList = _allData.value?.toMutableList()
                    updatedList?.add(
                        MediaModel(
                            0,
                            originalImagePath.toString(),
                            deletedImage.toString(),
                            "",
                            0,
                            0,
                            0,
                            false
                        )
                    )
                    // Add the restored image to the list
                    _allData.postValue(updatedList)
                } else {
                    Log.e("error", "restoreImage: Error restoring file. File not found in trash!!")
                }

            } catch (e: Exception) {
                Log.e("RestoreImage", "Error restoring file: ${e.message}")
            }
            //////////////
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

    fun formatDuration(durationInMillis: Long): String {
        val seconds = durationInMillis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        val hours = minutes / 60

        return if (hours > 0) {
            val remainingMinutes = minutes % 60
            String.format("%02d:%02d:%02d", hours, remainingMinutes, remainingSeconds)
        } else {
            String.format("%02d:%02d", minutes, remainingSeconds)
        }
    }


    fun copyImage(sourcePath: File, destinationPath: File) {
        val context: Context = getApplication()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (destinationPath.exists()) {
                    withContext(Dispatchers.Main) {
                        CommonFunctions.showToast(context, "Item already exists!!")
                        return@withContext
                    }
                } else {
                    sourcePath.copyTo(destinationPath)

                    scanFile(context, destinationPath)
                    scanFile(context, sourcePath)

                    getMediaFromInternalStorage()

                    withContext(Dispatchers.Main) {
//                        horizontalProgress.visibility = View.GONE
//                        recyclerViewCopyOrMove.visibility = View.VISIBLE
//                        CommonFunctions.showToast(context, "Item copied successfully!!")
                    }
                }
            } catch (e: IOException) {
                Log.e("CopyImage", "Error copying image: ${e.message}", e)
            }
        }
        ////////////
    }

    fun moveFile(sourcePath: File, destinationPath: File) {
        val context: Context = getApplication()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (destinationPath.exists()) {
                    withContext(Dispatchers.Main) {
                        CommonFunctions.showToast(context, "Item already exists!!")
                        return@withContext
                    }
                } else {
                    try {
                        Files.move(
                            sourcePath.toPath(),
                            destinationPath.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )

                        scanFile(context, destinationPath)
                        scanFile(context, sourcePath)

                        getMediaFromInternalStorage()

                        withContext(Dispatchers.Main) {
//                            horizontalProgress.visibility = View.GONE
//                            recyclerViewCopyOrMove.visibility = View.VISIBLE
//                            CommonFunctions.showToast(context, "Item moved successfully!!")
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e("tagDelete", e.message!!)
                    }
                }
            } catch (e: java.lang.Exception) {
                Log.e("tagDelete", e.message!!)
            }
        }
        ////////////////////
    }

    fun scanFile(context: Context, file: File) {
        MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
    }

    // This function create new folder in directory to store the images
    private fun createTrashDirectory(): File {
//        val trashDirectory = File(getApplication<Application>().getExternalFilesDir(null), ".TrashBin")
        val trashDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), ".trashBin"
        )
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
            MediaStore.MediaColumns.DATE_EXPIRES
        )

        val bundle = Bundle()
        bundle.putInt("android:query-arg-match-trashed", 1)
        bundle.putString(
            "android:query-arg-sql-selection", "${MediaStore.MediaColumns.IS_TRASHED} = 1"
        )
        bundle.putString(
            "android:query-arg-sql-sort-order", "${MediaStore.MediaColumns.DATE_EXPIRES} DESC"
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

    @RequiresApi(Build.VERSION_CODES.R)
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
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_EXPIRES))

                val uri = ContentUris.withAppendedId(collectionOfImages, id)

                // Discard invalid images that might exist on the device
                if (size == null) {
                    continue
                }

                // Convert the date string to a timestamp in milliseconds
                val timestampInMillis = date * 1000

                // Calculate deletion timestamp (current time + 30 days)
//                val deletionTimestamp = timestampInMillis + TimeUnit.DAYS.toMillis(30)

                val trash =
                    TrashBinAboveVersion(id, uri, path, name, size, timestampInMillis, isVideo)
                trashList.add(trash)
            }
            cursor.close()
        }
    }
}

package com.demo.newgalleryapp.activities

import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.FilterAdapter
import com.demo.newgalleryapp.databinding.DialogLoadingBinding
import com.demo.newgalleryapp.utilities.CommonFunctions.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.alhazmy13.imagefilter.ImageFilter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class EditActivity : AppCompatActivity() {

    private lateinit var filterRecyclerView: RecyclerView
    private lateinit var filterAdapter: FilterAdapter // Create a custom RecyclerView adapter for filters
    private lateinit var originalBitmap: Bitmap
    private lateinit var cropView: ImageView
    private lateinit var originalImageSave: TextView
    private lateinit var saveEditedImage: TextView
    private lateinit var backBtn: ImageView
    private lateinit var editProgressBar: ProgressBar
    lateinit var toolbar: Toolbar
    private lateinit var filteredBitmap: Bitmap
    private var uriString: String? = null
    private var anyChanges: Boolean = false
    private var popupWindow: PopupWindow? = null
    private var scopeJob: Job? = null
    private var handler: Handler? = null

    private lateinit var dialogBinding: DialogLoadingBinding

    private val progressDialogFragment by lazy {
        val dialog = Dialog(this)

        dialog.setOnShowListener {
            try {
                val displayMetrics = resources.displayMetrics
                val width = (displayMetrics.widthPixels * 0.8).toInt()
                val height = (displayMetrics.heightPixels * 0.6).toInt()

                val windowLayoutParams = dialog.window?.attributes
                windowLayoutParams?.width = width
                windowLayoutParams?.height = height
                dialog.window?.attributes = windowLayoutParams
            } catch (e: Exception) {
                Log.e("ProgressDialog", "Error setting dialog size", e)
            }

            // Set rounded corners (using previous method)
            dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.rounded_border_shape))
        }

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialogBinding = DialogLoadingBinding.inflate(dialog.layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        cropView = findViewById(R.id.cropView)
        filterRecyclerView = findViewById(R.id.filterRecyclerView)
        originalImageSave = findViewById(R.id.image_original_save)
        backBtn = findViewById(R.id.filter_back_btn)
        saveEditedImage = findViewById(R.id.image_filter_save_btn)
        editProgressBar = findViewById(R.id.edit_progressBar)
        toolbar = findViewById(R.id.toolBar_edit)

        handler = Handler(Looper.getMainLooper())

        saveEditedImage.setOnClickListener {
            showThreeDotPopup(toolbar, "filterImage")
        }

        uriString = intent.getStringExtra("Uri")

        backBtn.setOnClickListener {
            if (anyChanges) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
                anyChanges = false
            }
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            scopeJob?.cancel()
            finish()
        }


        if (uriString != null) {
            // Load the image into the ImageView using Glide
            Glide.with(this).load(uriString).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("IA", "onResourceReady: ${model.toString()}")
                    return false
                }
            }).diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(cropView)

            // this line of code save the original crop image
            originalImageSave.setOnClickListener {
                showThreeDotPopup(toolbar, "original")
            }

            runOnUiThread {
                // Initialize the originalBitmap with the loaded image
                Glide.with(this).asBitmap().load(uriString)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap, transition: Transition<in Bitmap>?
                        ) {
                            originalBitmap = resource
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // Do nothing here
                        }
                    })

                filterAdapter = FilterAdapter(this@EditActivity,
                    createFilterList(),
                    object : FilterAdapter.OnItemClickListener {
                        override fun onItemClick(filter: ImageFilter.Filter, position: Int) {
                            scopeJob?.cancel()
                            scopeJob = lifecycleScope.launch(Dispatchers.IO) {

                                withContext(Dispatchers.Main) {
                                    originalImageSave.visibility = View.GONE
                                    saveEditedImage.visibility = View.VISIBLE
                                }
                                applyFilter(filter)
                            }
                        }
                    })
                filterRecyclerView.layoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                filterRecyclerView.adapter = filterAdapter
            }
        } else {
            Log.e("EditActivity", "No URI provided")
            finish()
        }
    }

    override fun onBackPressed() {
        if (anyChanges) {
            val intent = Intent()
            setResult(Activity.RESULT_OK, intent)
            anyChanges = false
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        scopeJob?.cancel()
        super.onBackPressed()
    }

    private fun showThreeDotPopup(anchorView: View, onclickText: String) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.save_popup_menu, null)

        popupWindow = PopupWindow(
            popupView, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT, true
        )

        popupWindow?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val popupTextSave = popupView.findViewById<TextView>(R.id.save_column)
        val popupTextCancel = popupView.findViewById<TextView>(R.id.cancel_column)

        popupTextSave.setOnClickListener {

            if (onclickText == "original") {
                val noneImageFilter = uriStringToBitmap(uriString!!)

                progressDialogFragment.show()
                saveBitmapToInternalStorage(noneImageFilter!!)
                anyChanges = true
                handler?.postDelayed({
                    showToast(this, "Image Saved Successfully!!")
                    progressDialogFragment.cancel()
                    onBackPressed()
                }, 1000)

            } else if (onclickText == "filterImage") {

                progressDialogFragment.show()
                saveBitmapToInternalStorage(filteredBitmap)
                anyChanges = true
                handler?.postDelayed({
                    showToast(this, "Image Saved Successfully!!")
                    progressDialogFragment.cancel()
                    onBackPressed()
                }, 1000)
            }
            popupWindow?.dismiss()
        }

        popupTextCancel.setOnClickListener {
            popupWindow?.dismiss()
        }

        val popupItem = popupView.findViewById<LinearLayout>(R.id.popupItem_column_items)

        popupItem.setOnClickListener {
            popupWindow?.dismiss()
        }
        // Set dismiss listener to nullify the reference
        popupWindow?.setOnDismissListener {
            popupWindow = null
        }
    }

    private fun createFilterList(): List<ImageFilter.Filter> {
        // Return a list of available filters
        return ImageFilter.Filter.values().toList()
    }

    private suspend fun applyFilter(filter: ImageFilter.Filter) {
        withContext(Dispatchers.Main) {
            editProgressBar.visibility = View.VISIBLE
        }

        // Apply the selected filter to the main image
        filteredBitmap = ImageFilter.applyFilter(originalBitmap, filter)

        withContext(Dispatchers.Main) {
            cropView.setImageBitmap(filteredBitmap)
            saveEditedImage.visibility = View.VISIBLE
            editProgressBar.visibility = View.GONE
        }
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use MediaStore API for Android 11 and above
            val contentValues = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "edited_image_${System.currentTimeMillis()}.jpg"
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }

            val resolver = contentResolver
            val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            imageUri?.let {
                try {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        Log.d("ImageSave", "Image saved successfully: $it")
                    }
                } catch (e: IOException) {
                    Log.e("ImageSave", "Error saving image: $e")
                }
            }
        } else {

            val root = Environment.getExternalStorageDirectory().absolutePath
            val myDir = File("$root/saved_images")
            myDir.mkdirs()

            val filename = "edited_image_${System.currentTimeMillis()}.jpg"
            val file = File(myDir, filename)

            // Convert the bitmap to a JPEG and save it to the file
            try {
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.flush()
                stream.close()

                // Notify the system that a new file has been created
                MediaScannerConnection.scanFile(
                    applicationContext, arrayOf(file.absolutePath), null
                ) { path, uri ->
                    Log.e("fatal", "$uri   onActivityResult:  $path")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            /////////////////////////
        }
    }

    private fun uriStringToBitmap(uriString: String): Bitmap? {
        try {
            // Convert the URI string to a URI object
            val uri = Uri.parse(uriString)

            // Use the URI to load the image as a Bitmap
            val inputStream = contentResolver.openInputStream(uri)
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onDestroy() {
        handler?.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onStop() {
        // Remove the callbacks to stop the slideshow when the activity is not visible
        handler?.removeCallbacksAndMessages(null)
        super.onStop()
    }

    /////////////////////////
}
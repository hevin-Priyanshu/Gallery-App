package com.demo.newgalleryapp.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.newgalleryapp.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.photosFragment
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.videosFragment
import com.demo.newgalleryapp.adapters.FolderAdapter
import com.demo.newgalleryapp.interfaces.FolderClickListener
import com.demo.newgalleryapp.models.Folder
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_WRITE_PERMISSION_IN_COPY_MOVE_ACTIVITY
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.Executors


class CopyOrMoveActivity : AppCompatActivity(), FolderClickListener {

    private lateinit var recyclerViewCopyOrMove: RecyclerView
    private lateinit var copyText: TextView
    private lateinit var moveText: TextView
    private lateinit var closeBtn: ImageView
    private lateinit var noDataImage: LinearLayout
    private lateinit var folderAdapter: FolderAdapter
    private var updated: Boolean = false
    private lateinit var sourceFile: File
    private lateinit var destinationFile: File
    private lateinit var folderPath: String
    private lateinit var horizontalProgress: RelativeLayout

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_FOR_WRITE_PERMISSION_IN_COPY_MOVE_ACTIVITY && resultCode == Activity.RESULT_OK) {
            try {
                // check its from copy image path or click
                if (intent.hasExtra("copyImagePath")) {
//                    if (destinationFile.exists()) {
//                        // If it exists, delete it before copying
//                        destinationFile.delete()
//                    }
//                    sourceFile.copyTo(destinationFile)
                    recyclerViewCopyOrMove.visibility = View.GONE
                    horizontalProgress.visibility = View.VISIBLE
                    copyImage(sourceFile, destinationFile)

                } else {
//                    if (destinationFile.exists()) {
//                        // If it exists, delete it before copying
//                        destinationFile.delete()
//                    }
//                    sourceFile.copyTo(destinationFile)
//                    sourceFile.delete()
//                    Toast.makeText(this, "Image Move Successfully.", Toast.LENGTH_SHORT).show()
//                    finish()
                    recyclerViewCopyOrMove.visibility = View.GONE
                    horizontalProgress.visibility = View.VISIBLE
                    moveFile(sourceFile, destinationFile)

                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle the exception, e.g., show an error message
                Toast.makeText(this, "Failed to copy image.", Toast.LENGTH_SHORT).show()
                Log.e("error12", "onActivityResult: ${e.message}")
            }

            (application as AppClass).mainViewModel.getMediaFromInternalStorage()
            photosFragment.imagesAdapter?.notifyDataSetChanged()
            videosFragment.imagesAdapter?.notifyDataSetChanged()
 
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_copy_or_move)

        recyclerViewCopyOrMove = findViewById(R.id.copyOrMove_recycler_view)
        copyText = findViewById(R.id.text_copy)
        moveText = findViewById(R.id.text_move)
        closeBtn = findViewById(R.id.close_btn_copyOrMove)
        noDataImage = findViewById(R.id.no_data_image)
        horizontalProgress = findViewById(R.id.horizontalProgress)

        observeAllData()

        if (intent.hasExtra("copyImagePath")) {
            copyText.visibility = View.VISIBLE
            moveText.visibility = View.GONE
        } else {
            copyText.visibility = View.GONE
            moveText.visibility = View.VISIBLE
        }


        closeBtn.setOnClickListener {
            if (updated) {
                val intent = Intent()
                setResult(Activity.RESULT_OK, intent)
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

    }

    private fun observeAllData() {

        (application as AppClass).mainViewModel.allData.observe(this@CopyOrMoveActivity) {
            val folders: List<Folder> = it.groupBy { File(it.path).parent }.map { (path, models) ->
                Folder(path!!, models as ArrayList<MediaModel>)
            }
            (application as AppClass).mainViewModel.folderList.addAll(folders)
            recyclerViewCopyOrMove.layoutManager =
                GridLayoutManager(this@CopyOrMoveActivity, 3, LinearLayoutManager.VERTICAL, false)
            folderAdapter = FolderAdapter(
                this@CopyOrMoveActivity,
                folders as ArrayList<Folder>,
                this@CopyOrMoveActivity,
                "move"
            )
            recyclerViewCopyOrMove.adapter = folderAdapter
        }
    }


    override fun onClick(folderPath: String) {
        this.folderPath = folderPath

        val originalCopyPath = intent.getStringExtra("copyImagePath")
        val originalMovePath = intent.getStringExtra("moveImagePath")

        sourceFile = if (intent.hasExtra("copyImagePath")) {
            File(originalCopyPath!!)
        } else {
            File(originalMovePath!!)
        }

        val name = sourceFile.name
        destinationFile = File(folderPath, name)

        // for android 11 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val arrayList: ArrayList<Uri> = ArrayList()
            MediaScannerConnection.scanFile(this, arrayOf(sourceFile.path), null) { file, uri ->
                try {
                    if (uri != null) {
                        arrayList.add(uri)
                        val pendingIntent: PendingIntent = MediaStore.createWriteRequest(contentResolver, arrayList)
                        startIntentSenderForResult(
                            pendingIntent.intentSender,
                            REQ_CODE_FOR_WRITE_PERMISSION_IN_COPY_MOVE_ACTIVITY,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } else {
                        Log.e(
                            "CopyOrMoveActivity",
                            "MediaScannerConnection scanFile callback returned null Uri $uri"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("CopyOrMoveActivity", "Error creating write request", e)
                }
            }
        }
        // for below android 10 versions
        else {
            if (intent.hasExtra("copyImagePath")) {

                AlertDialog.Builder(this).setTitle("Copy Item?")
                    .setMessage("Are you sure to copy these image on $destinationFile?")
                    .setPositiveButton("Copy") { _, _ ->
                        copyImage(sourceFile, destinationFile)
                    }.setNegativeButton("No") { dialog, _ ->
                        // User clicked "No", do nothing
                        dialog.dismiss()
                    }.show()

            } else {

                AlertDialog.Builder(this).setTitle("Move Item?")
                    .setMessage("Are you sure to move these image on $destinationFile?")
                    .setPositiveButton("Move") { _, _ ->
                        moveFile(sourceFile, destinationFile)
                    }.setNegativeButton("No") { dialog, _ ->
                        // User clicked "No", do nothing
                        dialog.dismiss()
                    }.show()

            }
        }

    }


    private fun copyImage(sourcePath: File, destinationPath: File) {

        Executors.newSingleThreadExecutor().execute {
            try {
                if (destinationPath.exists()) {
                    runOnUiThread {
                        Toast.makeText(this, "Image already exists!!", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                    finish()
                } else {
                    sourcePath.copyTo(destinationPath)
                    (application as AppClass).mainViewModel.scanFile(this, destinationPath)
                    (application as AppClass).mainViewModel.scanFile(this, sourcePath)

                    runOnUiThread {
                        horizontalProgress.visibility = View.GONE
                        recyclerViewCopyOrMove.visibility = View.VISIBLE
                        Toast.makeText(this, "Image Copy successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: IOException) {
                Log.e("CopyImage", "Error copying image: ${e.message}", e)
            }
        }
    }

    private fun moveFile(file: File, dir: File) {

        Executors.newSingleThreadExecutor().execute {
            try {
                if (dir.exists()) {
                    runOnUiThread {
                        Toast.makeText(this, "Image already exists!!", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                    finish()
                } else {
                    try {
                        Files.move(file.toPath(), dir.toPath())

                        (application as AppClass).mainViewModel.scanFile(this, dir)
                        (application as AppClass).mainViewModel.scanFile(this, file)

                        runOnUiThread {
                            horizontalProgress.visibility = View.GONE
                            recyclerViewCopyOrMove.visibility = View.VISIBLE
                            Toast.makeText(this, "Image move successfully", Toast.LENGTH_SHORT)
                                .show()
                            finish()
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
}
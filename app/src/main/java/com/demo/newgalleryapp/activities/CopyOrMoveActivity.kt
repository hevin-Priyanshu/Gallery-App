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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
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
import com.demo.newgalleryapp.utilities.CommonFunctions.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files

class CopyOrMoveActivity : AppCompatActivity(), FolderClickListener {

    private lateinit var recyclerViewCopyOrMove: RecyclerView
    private lateinit var horizontalProgress: RelativeLayout
    private lateinit var copyText: TextView
    private lateinit var moveText: TextView
    private lateinit var closeBtn: ImageView
    private lateinit var noDataImage: LinearLayout
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var sourceFile: File
    private lateinit var destinationFile: File
    private lateinit var folderPath: String
    private var anyupdated: Boolean = false
    private var popupWindow: PopupWindow? = null

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
            if (anyupdated) {
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
                        val pendingIntent: PendingIntent =
                            MediaStore.createWriteRequest(contentResolver, arrayList)
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

                showCopyOrMovePopupmenu(recyclerViewCopyOrMove, "Copy")

//                AlertDialog.Builder(this).setTitle("Copy Item?")
//                    .setMessage("Are you sure to copy these image on $destinationFile?")
//                    .setPositiveButton("Copy") { _, _ ->
//                        copyImage(sourceFile, destinationFile)
//                    }.setNegativeButton("No") { dialog, _ ->
//                        // User clicked "No", do nothing
//                        dialog.dismiss()
//                    }.show()

            } else {
                showCopyOrMovePopupmenu(recyclerViewCopyOrMove, "Move")
            }
        }

    }

    override fun onBackPressed() {
        if (anyupdated) {
            val intent = Intent()
            setResult(Activity.RESULT_OK, intent)
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onBackPressed()
    }

    private fun copyImage(sourcePath: File, destinationPath: File) {

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (destinationPath.exists()) {
                    withContext(Dispatchers.Main) {
                        showToast(this@CopyOrMoveActivity, "Item already exists!!")
                        return@withContext
                    }
                    finish()
                } else {
                    sourcePath.copyTo(destinationPath)
                    (application as AppClass).mainViewModel.scanFile(
                        this@CopyOrMoveActivity, destinationPath
                    )
                    (application as AppClass).mainViewModel.scanFile(
                        this@CopyOrMoveActivity, sourcePath
                    )

                    withContext(Dispatchers.Main) {
                        horizontalProgress.visibility = View.GONE
                        recyclerViewCopyOrMove.visibility = View.VISIBLE
                        showToast(this@CopyOrMoveActivity, "Item copy successfully!!")
                    }
                }
            } catch (e: IOException) {
                Log.e("CopyImage", "Error copying image: ${e.message}", e)
            }
        }
        ////////////
    }

    private fun moveFile(sourcePath: File, destinationPath: File) {

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (destinationPath.exists()) {
                    withContext(Dispatchers.Main) {
                        showToast(this@CopyOrMoveActivity, "Item already exists!!")
                        return@withContext
                    }
                    finish()
                } else {
                    try {
                        Files.move(sourcePath.toPath(), destinationPath.toPath())

                        (application as AppClass).mainViewModel.scanFile(
                            this@CopyOrMoveActivity, destinationPath
                        )
                        (application as AppClass).mainViewModel.scanFile(
                            this@CopyOrMoveActivity, sourcePath
                        )

                        withContext(Dispatchers.Main) {
                            horizontalProgress.visibility = View.GONE
                            recyclerViewCopyOrMove.visibility = View.VISIBLE
                            showToast(this@CopyOrMoveActivity, "Item move successfully!!")
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

    private fun showCopyOrMovePopupmenu(anchorView: View, setTitle: String) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupWindowRestoreOne: View = inflater.inflate(R.layout.copy_or_move_popup_menu, null)

        popupWindow = PopupWindow(
            popupWindowRestoreOne,
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.MATCH_PARENT,
            true
        )

        popupWindow?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val copyMoveSaveBtn = popupWindowRestoreOne.findViewById<TextView>(R.id.copy_move_save_btn)
        val cancelBtn = popupWindowRestoreOne.findViewById<TextView>(R.id.copy_move_cancel_btn)

        val mainText = popupWindowRestoreOne.findViewById<TextView>(R.id.copy_move_main_text)
        val howManyItems = popupWindowRestoreOne.findViewById<TextView>(R.id.copy_move_text)

        mainText.text = "$setTitle Item ?"
        howManyItems.text =
            "Are you sure to $setTitle these image on ${destinationFile.parentFile} ?"

        copyMoveSaveBtn.text = setTitle

        copyMoveSaveBtn.setOnClickListener {
            if (setTitle == "Copy") {
                copyImage(sourceFile, destinationFile)
                anyupdated = true
            } else if (setTitle == "Move") {
                moveFile(sourceFile, destinationFile)
                anyupdated = true
            }

            popupWindow?.dismiss()
        }

        cancelBtn.setOnClickListener {
            popupWindow?.dismiss()
        }
    }
}
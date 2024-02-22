package com.demo.newgalleryapp.activities

import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.FolderAdapter
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.databinding.DialogLoadingBinding
import com.demo.newgalleryapp.interfaces.FolderClickListener
import com.demo.newgalleryapp.models.Folder
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_WRITE_PERMISSION_IN_COPY_MOVE_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class CopyOrMoveActivity : AppCompatActivity(), FolderClickListener {

    private lateinit var recyclerViewCopyOrMove: RecyclerView
    private lateinit var copyText: TextView
    private lateinit var moveText: TextView
    private lateinit var closeBtn: ImageView
    private lateinit var noDataImage: LinearLayout
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var sourceFile: File
    private lateinit var destinationFile: File
    private lateinit var folderPath: String
    private lateinit var dialogBinding: DialogLoadingBinding
    private var anyupdated: Boolean = false
    private var popupWindow: PopupWindow? = null
    private var tempList: ArrayList<Folder> = ArrayList()
    private var handler: Handler? = null

    private val progressDialogFragment by lazy {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialogBinding = DialogLoadingBinding.inflate(dialog.layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_FOR_WRITE_PERMISSION_IN_COPY_MOVE_ACTIVITY && resultCode == Activity.RESULT_OK) {
            try {

                val mainScreenActivity = intent.getBooleanExtra("mainScreenActivity", false)

                if (mainScreenActivity) {

                    val objectList = intent.getSerializableExtra("pathsList") as ArrayList<String>
                    if (intent.hasExtra("copySelectedMainScreenActivity")) {
                        progressDialogFragment.show()
                        copyOrMoveImages(objectList, folderPath, false)
                        anyupdated = true

                        Handler().postDelayed(Runnable {
                            progressDialogFragment.cancel()
                            onBackPressed()
                        }, 1000)
                    } else {

                        progressDialogFragment.show()
                        copyOrMoveImages(objectList, folderPath, true)
                        anyupdated = true
                        Handler().postDelayed(Runnable {
                            progressDialogFragment.cancel()
                            onBackPressed()
                        }, 1000)
                    }
                } else {
                    // check its from copy image path or click
                    if (intent.hasExtra("copyImagePath")) {

                        progressDialogFragment.show()
                        (application as AppClass).mainViewModel.copyImage(
                            sourceFile, destinationFile
                        )
                        anyupdated = true
                        Handler().postDelayed(Runnable {
                            progressDialogFragment.cancel()
                            showToast(this, "Item copied successfully!!")
                            onBackPressed()
                        }, 1000)

//                        copyImage(sourceFile, destinationFile)
                    } else {

                        progressDialogFragment.show()
                        (application as AppClass).mainViewModel.moveFile(
                            sourceFile, destinationFile
                        )
                        anyupdated = true
                        Handler().postDelayed(Runnable {
                            progressDialogFragment.cancel()
                            showToast(this, "Item moved successfully!!")
                            onBackPressed()
                        }, 1000)
//                        moveFile(sourceFile, destinationFile)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Handle the exception, e.g., show an error message
                Toast.makeText(this, "Failed to copy image.", Toast.LENGTH_SHORT).show()
                Log.e("error12", "onActivityResult: ${e.message}")
            }
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

        handler = Handler(Looper.getMainLooper())

        observeAllData()

        val mainScreenActivity = intent.getBooleanExtra("mainScreenActivity", false)

        if (mainScreenActivity) {
            if (intent.hasExtra("copySelectedMainScreenActivity")) {
                copyText.visibility = View.VISIBLE
                moveText.visibility = View.GONE
            } else {
                copyText.visibility = View.GONE
                moveText.visibility = View.VISIBLE
            }
        } else {
            if (intent.hasExtra("copyImagePath")) {
                copyText.visibility = View.VISIBLE
                moveText.visibility = View.GONE
            } else {
                copyText.visibility = View.GONE
                moveText.visibility = View.VISIBLE
            }
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

            (application as AppClass).mainViewModel.folderList.clear()
            (application as AppClass).mainViewModel.folderList.addAll(folders)

            tempList.addAll((application as AppClass).mainViewModel.folderList)

            recyclerViewCopyOrMove.layoutManager =
                GridLayoutManager(this@CopyOrMoveActivity, 3, LinearLayoutManager.VERTICAL, false)

            val newFolder = Folder("New Album", ArrayList())
            tempList.add(0, newFolder)

            folderAdapter = FolderAdapter(
                this@CopyOrMoveActivity, tempList, this@CopyOrMoveActivity, "FromCopyMove"
            )
            recyclerViewCopyOrMove.adapter = folderAdapter
        }
    }

    override fun onClick(folderPath: String) {
        this.folderPath = folderPath

        val mainScreenActivity = intent.getBooleanExtra("mainScreenActivity", false)

        if (mainScreenActivity) {

            val objectList = intent.getSerializableExtra("pathsList") as ArrayList<String>

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                if (objectList.isNotEmpty()) {
                    val arrayList: ArrayList<Uri> = ArrayList()
                    MediaScannerConnection.scanFile(
                        this, objectList.toTypedArray(), null
                    ) { _, uri ->
                        arrayList.add(uri)
                        try {
                            if (arrayList.size == objectList.size) {
                                val pendingIntent: PendingIntent =
                                    MediaStore.createWriteRequest(contentResolver, arrayList)
                                startIntentSenderForResult(
                                    pendingIntent.intentSender,
                                    REQ_CODE_FOR_WRITE_PERMISSION_IN_COPY_MOVE_ACTIVITY,
                                    null,
                                    0,
                                    0,
                                    0
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("TAG", "000AAA: $e")
                        }
                    }
                } else {
                    showToast(this, "No Item Selected to Modify")
                }

            } else {
                if (intent.hasExtra("copySelectedMainScreenActivity")) {
                    showCopyOrMove(recyclerViewCopyOrMove, objectList, folderPath, false, "Copy")
                } else {
                    showCopyOrMove(recyclerViewCopyOrMove, objectList, folderPath, true, "Move")
                }
            }
            ////////////////////
        } else {

            val originalCopyPath = intent.getStringExtra("copyImagePath")
            val originalMovePath = intent.getStringExtra("moveImagePath")

            sourceFile = if (intent.hasExtra("copyImagePath")) {
                File(originalCopyPath!!)
            } else {
                File(originalMovePath!!)
            }

            val name = sourceFile.name
            var copyNumber = 1
            val extension = name.substringAfterLast('.')
            val fileNameWithoutExtension = name.substringBeforeLast('.')

            if (fileNameWithoutExtension.startsWith("copy_")) {
                val originalCopyNumber =
                    fileNameWithoutExtension.substringAfterLast('(').substringBeforeLast(')')
                        .toIntOrNull()
                if (originalCopyNumber != null) {
                    copyNumber = originalCopyNumber + 1
                    val newName =
                        fileNameWithoutExtension.substringBeforeLast('(') + "(${copyNumber})"
                    destinationFile = File(folderPath, "$newName.$extension")
                } else {
                    destinationFile =
                        File(folderPath, "${fileNameWithoutExtension}(${copyNumber}).$extension")
                }
            } else {
                destinationFile = File(folderPath, "copy_${fileNameWithoutExtension}.$extension")
            }

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

                    showCopyOrMovePopupmenu(
                        recyclerViewCopyOrMove, "Copy", sourceFile, destinationFile
                    )

                } else {
                    showCopyOrMovePopupmenu(
                        recyclerViewCopyOrMove, "Move", sourceFile, destinationFile
                    )
                }
            }
            /////////////////
        }
        //////////////
    }


    private fun copyOrMoveImages(
        imagePaths: ArrayList<String>, destinationFolder: String, move: Boolean
    ) {

        lifecycleScope.launch(Dispatchers.IO) {

            for (imagePath in imagePaths) {
                val sourcePath = File(imagePath)
                val imageName = sourcePath.name
                val destinationFile: File

                var copyNumber = 1
                val extension = imageName.substringAfterLast('.')
                val fileNameWithoutExtension = imageName.substringBeforeLast('.')

                if (fileNameWithoutExtension.startsWith("copy_")) {
                    val originalCopyNumber =
                        fileNameWithoutExtension.substringAfterLast('(').substringBeforeLast(')')
                            .toIntOrNull()
                    if (originalCopyNumber != null) {
                        copyNumber = originalCopyNumber + 1
                        val newName =
                            fileNameWithoutExtension.substringBeforeLast('(') + "(${copyNumber})"
                        destinationFile = File(destinationFolder, "$newName.$extension")
                    } else {
                        destinationFile = File(
                            destinationFolder,
                            "${fileNameWithoutExtension}(${copyNumber}).$extension"
                        )
                    }
                } else {
                    destinationFile =
                        File(destinationFolder, "copy_${fileNameWithoutExtension}.$extension")
                }

                if (move) {
                    // Move the file
                    (application as AppClass).mainViewModel.moveFile(sourcePath, destinationFile)

                } else {
                    // Copy the file
                    (application as AppClass).mainViewModel.copyImage(sourcePath, destinationFile)
                }
            }

        }
        if (move) {
            showToast(this, "Item moved successfully!!")
        } else {
            showToast(this, "Item copied successfully!!")
        }

        ////////////
    }

    override fun onBackPressed() {
        if (anyupdated) {
            val intent = Intent()
            setResult(Activity.RESULT_OK, intent)
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onBackPressed()
    }


    private fun showCopyOrMovePopupmenu(
        anchorView: View, setTitle: String, srcFile: File, desFile: File
    ) {
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
        howManyItems.text = "Are you sure to $setTitle these image on ${desFile.parentFile} ?"

        copyMoveSaveBtn.text = setTitle

        copyMoveSaveBtn.setOnClickListener {
            if (setTitle == "Copy") {

                progressDialogFragment.show()
                (application as AppClass).mainViewModel.copyImage(srcFile, desFile)
                anyupdated = true

                handler?.postDelayed({
                    progressDialogFragment.cancel()
                    onBackPressed()
                    showToast(this, "Item copied successfully!!")
                }, 1000)

            } else if (setTitle == "Move") {

                progressDialogFragment.show()
                (application as AppClass).mainViewModel.moveFile(srcFile, desFile)
                anyupdated = true
                handler?.postDelayed({
                    progressDialogFragment.cancel()
                    onBackPressed()
                    showToast(this, "Item moved successfully!!")
                }, 1000)
            }

            popupWindow?.dismiss()
        }

        cancelBtn.setOnClickListener {
            popupWindow?.dismiss()
        }
    }

    private fun showCopyOrMove(
        anchorView: View,
        imagePaths: ArrayList<String>,
        folderPath: String,
        isMove: Boolean,
        setTitle: String,
    ) {
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
        howManyItems.text = "Are you sure to $setTitle these image on ${folderPath} ?"

        copyMoveSaveBtn.text = setTitle

        copyMoveSaveBtn.setOnClickListener {
            progressDialogFragment.show()
            copyOrMoveImages(imagePaths, folderPath, isMove)
            anyupdated = true

            handler?.postDelayed({
                progressDialogFragment.cancel()
                onBackPressed()
            }, 1000)

            popupWindow?.dismiss()
        }

        cancelBtn.setOnClickListener {
            popupWindow?.dismiss()
        }
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
}
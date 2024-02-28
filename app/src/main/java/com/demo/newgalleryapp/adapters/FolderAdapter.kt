package com.demo.newgalleryapp.adapters

import android.app.Activity
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.FolderImagesActivity
import com.demo.newgalleryapp.interfaces.FolderClickListener
import com.demo.newgalleryapp.models.Folder
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_CHANGES_IN_FOLDER_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.showToast
import java.io.File

class FolderAdapter(
    private val context: Activity,
    private var list: ArrayList<Folder>,
    private val listener: FolderClickListener? = null,
    private val from: String
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    private var popupWindow: PopupWindow? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.folder_items, parent, false)
        return FolderViewHolder(layout)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun clearData() {
        list.clear()
        notifyDataSetChanged()
    }

    fun updateData(filteredData: ArrayList<Folder>) {
        this.list = filteredData
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {

        holder.textView.isSelected = true


        when (from) {

            "FromCopyMove" -> {
                if (position == 0) {

                    holder.textView.text = list[0].name
                    holder.noItem.visibility = View.GONE
                    holder.image.setImageResource(R.drawable.empty_image)
                    holder.image.background = context.getDrawable(R.drawable.placeholder)

                    holder.image.setOnClickListener {
                        showCreateNewAlbumPopup(holder.image)
                    }

                } else {

                    holder.textView.text = File(list[position].name).name
                    holder.noItem.text = list[position].models.size.toString()

                    holder.image.setOnClickListener {
                        listener?.onClick(File(list[position].name).path)
                    }

                    if (list[position].models.isNotEmpty()) {
                        val folder = list[position].models[0].path
                        Glide.with(context).load(folder).placeholder(R.drawable.placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE).fitCenter()
                            .into(holder.image)
                    } else {
                        // Handle the case when the folder is empty
                        holder.image.setImageResource(R.drawable.placeholder)
                    }
                }

            }

            "FromFolder" -> {

                holder.textView.text = File(list[position].name).name
                holder.noItem.text = list[position].models.size.toString()


                holder.image.setOnClickListener {
                    val intent = Intent(context, FolderImagesActivity::class.java)
                    intent.putExtra("folderPosition", position)
                    intent.putExtra("folderName", File(list[position].name).name)
                    context.startActivityForResult(intent, REQ_CODE_FOR_CHANGES_IN_FOLDER_ACTIVITY)
                    context.overridePendingTransition(
                        android.R.anim.fade_in, android.R.anim.fade_out
                    )
                }

                if (list[position].models.isNotEmpty()) {
                    val folder = list[position].models[0].path
                    Glide.with(context).load(folder).placeholder(R.drawable.placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE).fitCenter()
                        .into(holder.image)
                } else {
                    // Handle the case when the folder is empty
                    holder.image.setImageResource(R.drawable.placeholder)
                }
                /////////////////////
            }
            //////////////////////////
        }
        /////////////////////
    }

    private fun showCreateNewAlbumPopup(
        anchorView: View
    ) {
        val popupView =
            LayoutInflater.from(context).inflate(R.layout.popup_menu_create_folder, null)
//        val inflater = getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//        val popupView: View = inflater.inflate(R.layout.popup_menu_create_folder, null)

        popupWindow = PopupWindow(
            popupView, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT, true
        )

        popupWindow?.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

        popupWindow?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val saveBtn = popupView.findViewById<TextView>(R.id.create_folder_save)
        val cancelBtn = popupView.findViewById<TextView>(R.id.create_folder_chancel)
        val searchEditText = popupView.findViewById<EditText>(R.id.create_folder_searchEditText)


        saveBtn.setOnClickListener {
            val newName = searchEditText.text.toString().trim()

            // Check if the new name is empty
            if (newName.isEmpty()) {
                showToast(context, "Please enter a valid name")
            } else {
//                val createNewAlbum = File(context.application.getExternalFilesDir(null), newName)
                val createNewAlbum = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    newName
                )

                // Check if a directory with the same name already exists
                if (createNewAlbum.exists()) {
                    showToast(context, "Album with this name already exists")
                } else {
                    val create = createNewAlbum.mkdirs()

                    if (create) {
                        MediaScannerConnection.scanFile(
                            context.applicationContext, arrayOf(createNewAlbum.absolutePath), null
                        ) { path, uri ->
                            Log.e("fatal", "$uri   onActivityResult:  $path")
                        }
                        // Notify the listener about the new directory name
                        listener?.onClick(createNewAlbum.path)
                        // Dismiss the popup window
                        popupWindow?.dismiss()
                    } else {
                        // Handle the case where directory creation fails
                        showToast(context, "Failed to create album")
                    }
                }
            }
            ///////////////////
        }


        cancelBtn.setOnClickListener {
            popupWindow?.dismiss()
        }

//        val popupItem = popupView.findViewById<LinearLayout>(R.id.popupItem_create)
//
//        popupItem.setOnClickListener {
//            popupWindow?.dismiss()
//        }
//        // Set dismiss listener to nullify the reference
//        popupWindow?.setOnDismissListener {
//            popupWindow = null
//        }
    }


    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image: ImageView = itemView.findViewById(R.id.image_view_folder)
        var textView: TextView = itemView.findViewById(R.id.textView_folder)
        var noItem: TextView = itemView.findViewById(R.id.noItem_folder)
    }
}
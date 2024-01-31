package com.demo.newgalleryapp.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.FolderImagesActivity
import com.demo.newgalleryapp.interfaces.FolderClickListener
import com.demo.newgalleryapp.models.Folder
import java.io.File

class FolderAdapter(
    private val context: Activity,
    private var list: ArrayList<Folder>,
    private val listener: FolderClickListener? = null,
    private val from: String
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {


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
        holder.textView.text = File(list[position].name).name

        holder.noItem.text = list[position].models.size.toString()

        holder.image.setOnClickListener {

            when (from) {
                "move" -> {
                    listener?.onClick(File(list[position].name).path)
                }

                "normal" -> {
                    val intent = Intent(context, FolderImagesActivity::class.java)
                    intent.putExtra("folderPosition", position)
                    intent.putExtra("folderName", File(list[position].name).name)
                    context.startActivityForResult(intent, 333)
                    context.overridePendingTransition(
                        android.R.anim.fade_in, android.R.anim.fade_out
                    )
                }
            }
            ///////////////////
        }

        if (list[position].models.isNotEmpty()) {
            val folder = list[position].models[0].path
            Glide.with(context).load(folder).placeholder(R.drawable.placeholder)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE).fitCenter().into(holder.image)
        } else {
            // Handle the case when the folder is empty
            holder.image.setImageResource(R.drawable.placeholder)
        }
    }


    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image: ImageView = itemView.findViewById(R.id.image_view_folder)
        var textView: TextView = itemView.findViewById(R.id.textView_folder)
        var noItem: TextView = itemView.findViewById(R.id.noItem_folder)
    }
}
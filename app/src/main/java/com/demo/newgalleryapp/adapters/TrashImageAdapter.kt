package com.demo.newgalleryapp.adapters

import android.database.Cursor
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.demo.newgalleryapp.R

class TrashImageAdapter(private val cursor: Cursor) : RecyclerView.Adapter<TrashImageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.images_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (cursor.moveToPosition(position)) {
            val imageUri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
            // Use Glide to load the image into holder.imageView
            Glide.with(holder.itemView).load(imageUri).into(holder.imageView)

            // Set restore/delete button listeners (if applicable)
        }
    }

    override fun getItemCount(): Int = cursor.count

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_view)
        // References to other UI elements (e.g., buttons)
    }
}
package com.demo.newgalleryapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.demo.newgalleryapp.R
import net.alhazmy13.imagefilter.ImageFilter

class FilterAdapter(
    private val filterList: List<ImageFilter.Filter>, private val listener: OnItemClickListener
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    private val filterNames = arrayOf(
        "Gray",
        "Relief",
        "Average Blur",
        "Oil",
        "Neon",
        "Pixelate",
        "TV",
        "Invert",
        "Block",
        "Old",
        "Sharpen",
        "Light",
        "Lomo",
        "HDR",
        "Gaussian Blur",
        "Soft Glow",
        "Sketch",
        "Motion Blur",
        "Gotham"
    )
    var selectedPosition = RecyclerView.NO_POSITION

    interface OnItemClickListener {
        fun onItemClick(filter: ImageFilter.Filter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.image_filters, parent, false)
        return FilterViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filterList[position]
        holder.bind(filter)
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    inner class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val filterImageView: ImageView = itemView.findViewById(R.id.imageCrop_filter_preview)
        private val filterNameTextView: TextView = itemView.findViewById(R.id.filters_text)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (position != selectedPosition) {
                        // Update the selected filter position
                        val previousSelected = selectedPosition
                        selectedPosition = position
                        // Notify item changes to redraw the UI
                        notifyItemChanged(previousSelected)
                        notifyItemChanged(selectedPosition)

                        val filter = filterList[position]
                        listener.onItemClick(filter)
                    }
                }
            }
        }

        fun bind(filter: ImageFilter.Filter) {
            // Load filtered image preview using Glide
            val requestOptions = RequestOptions().fitCenter()

            Glide.with(itemView).load(getFilterPreviewResourceId(filter)) // Replace with your image resource ID
                .apply(requestOptions).into(filterImageView)

            filterNameTextView.text = getFilterName(filter)

            if (adapterPosition == selectedPosition) {
                filterNameTextView.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context, R.color.selected_filter_color
                    )
                )
                filterNameTextView.setTextColor(
                    ContextCompat.getColor(
                        itemView.context, R.color.white
                    )
                )
            } else {
                filterNameTextView.setBackgroundColor(Color.TRANSPARENT)
                filterNameTextView.setTextColor(
                    ContextCompat.getColor(
                        itemView.context, R.color.black
                    )
                )
            }
        }
    }


    private fun getFilterName(filter: ImageFilter.Filter): String {
        return filterNames.getOrNull(filter.ordinal) ?: "Unknown"
    }

    private fun getFilterPreviewResourceId(filter: ImageFilter.Filter): Int {
        // Return the resource ID corresponding to the filter
        return when (filter) {

            ImageFilter.Filter.GRAY -> R.drawable.gray_preview_image
            ImageFilter.Filter.RELIEF -> R.drawable.relief_preview_image
            ImageFilter.Filter.AVERAGE_BLUR -> R.drawable.average_preview_image
            ImageFilter.Filter.OIL -> R.drawable.oil_preview_image
            ImageFilter.Filter.NEON -> R.drawable.neon_preview_image
            ImageFilter.Filter.PIXELATE -> R.drawable.pixelate_preview_image
            ImageFilter.Filter.TV -> R.drawable.tv_preview_image
            ImageFilter.Filter.INVERT -> R.drawable.invert_preview_image
            ImageFilter.Filter.BLOCK -> R.drawable.block_preview_image
            ImageFilter.Filter.OLD -> R.drawable.old_preview_image
            ImageFilter.Filter.SHARPEN -> R.drawable.sharpen_preview_image
            ImageFilter.Filter.LIGHT -> R.drawable.light_preview_image
            ImageFilter.Filter.LOMO -> R.drawable.lomo_preview_image
            ImageFilter.Filter.HDR -> R.drawable.hdr_preview_image
            ImageFilter.Filter.GAUSSIAN_BLUR -> R.drawable.gaussian_preview_image
            ImageFilter.Filter.SOFT_GLOW -> R.drawable.soft_preview_image
            ImageFilter.Filter.SKETCH -> R.drawable.sketch_preview_image
            ImageFilter.Filter.MOTION_BLUR -> R.drawable.motion_preview_image
            ImageFilter.Filter.GOTHAM -> R.drawable.gotham_preview_image

            else -> {
                return 0
            }
        }
    }
}

package com.demo.newgalleryapp.adapters

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.demo.newgalleryapp.R
import net.alhazmy13.imagefilter.ImageFilter

class FilterAdapter(
    val context: Activity,
    private val filterList: List<ImageFilter.Filter>,
    private val listener: OnItemClickListener
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

    private var selectedPosition = RecyclerView.NO_POSITION

    interface OnItemClickListener {
        fun onItemClick(filter: ImageFilter.Filter, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.image_filters, parent, false)
        return FilterViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filterList[position]

        val requestOptions = RequestOptions().fitCenter()



        Glide.with(context).load(getFilterPreviewResourceId(filter)).apply(requestOptions).into(holder.filterImageView)

        holder.filterNameTextView.text = getFilterName(filter)

        val isSelected = selectedPosition == position

        // Set background color accordingly
        if (isSelected) {
            holder.filterCardView.background = ContextCompat.getDrawable(context, R.drawable.cardview_selector)
        } else {
            holder.filterCardView.background = null
        }

        holder.filterCardView.setOnClickListener {
            // Update the selected position
            selectedPosition = holder.adapterPosition
            // Notify adapter about the change
            notifyDataSetChanged()
            listener.onItemClick(filter, position)
        }
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val filterImageView: ImageView = itemView.findViewById(R.id.imageCrop_filter_preview)
        val filterNameTextView: TextView = itemView.findViewById(R.id.filters_text)
        val filterCardView: LinearLayout = itemView.findViewById(R.id.card_view_filter)
    }


    private fun getFilterName(filter: ImageFilter.Filter): String {
        return filterNames.getOrNull(filter.ordinal) ?: "Unknown"
    }

    private fun getFilterPreviewResourceId(filter: ImageFilter.Filter): Int {
        // Return the resource ID corresponding to the filter
        return when (filter) {

//            ImageFilter.Filter.GRAY -> R.drawable.cardview_selector
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

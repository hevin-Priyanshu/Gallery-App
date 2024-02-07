package com.demo.newgalleryapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.newgalleryapp.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.ImagesAd
import com.demo.newgalleryapp.fragments.MediaFragment.Companion.mediaProgressBar
import com.demo.newgalleryapp.models.MediaModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class PhotosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    var imagesAdapter: ImagesAd? = null
    private val commonList: ArrayList<Any> = arrayListOf()
    private var position: Int = 0
    private var count: Int = 0

    companion object {
        private const val ARG_POSITION = "position"
        fun newInstance(position: Int): PhotosFragment {
            val fragment = PhotosFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.fragment_photos, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_photos)

        arguments?.let {
            position = it.getInt(ARG_POSITION, 0)
            if (position == 0) {
                count =
                    (requireActivity().application as AppClass).mainViewModel.sharedPreferencesHelper.getGridColumns()
                lifecycleScope.launch {
                    observeAllData(count)
                }
            }
        }

        return view
    }

    fun observeAllData(spanCount: Int) {

        (requireActivity().application as AppClass).mainViewModel.photosData.observe(
            viewLifecycleOwner
        ) { photosList ->

            commonList.clear()
            (requireActivity().application as AppClass).mainViewModel.tempPhotoList.clear()
            // Sort the list of images based on date
            val sortedPhotos = photosList.sortedByDescending { it.date }
            (requireActivity().application as AppClass).mainViewModel.tempPhotoList.addAll(
                sortedPhotos
            )

            val groupedPhotos: List<Map.Entry<String, List<MediaModel>>> =
                sortedPhotos.groupBy { getFormattedDate(it.date) }.entries.toList()

//            for (entry in groupedPhotos) {
//                val date = entry.key
//                commonList.add(date)
//                for (image in entry.value) {
//                    commonList.add(image)
//                }
//            }

            groupedPhotos.forEach { (date, images) ->
                commonList.add(date)
                commonList.addAll(images)
            }
            loadLayout(spanCount)

//            imagesAdapter = ImagesAd(requireActivity(), commonList, -1)
//
//            val gl = GridLayoutManager(requireActivity(), spanCount, LinearLayoutManager.VERTICAL, false)
////            val gl = GridLayoutManager(requireActivity(), spanCount)
//
//            gl.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
//                override fun getSpanSize(position: Int): Int {
//                    // Assuming your adapter has a method getItemViewType(position) to get the item type
//                    // Set span count based on item type
//                    return when (imagesAdapter!!.getItemViewType(position)) {
//                        101 -> spanCount
//                        100 -> 1
//                        // Add more cases as needed
//                        else -> 1
//                    }
//                }
//            }
//
//            recyclerView.layoutManager = gl
//            recyclerView.adapter = imagesAdapter
        }
    }

    private fun loadLayout(spanCount: Int) {
        imagesAdapter = ImagesAd(requireActivity(), commonList, -1)

        val gl =
            GridLayoutManager(requireActivity(), spanCount, LinearLayoutManager.VERTICAL, false)
//            val gl = GridLayoutManager(requireActivity(), spanCount)

        gl.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // Assuming your adapter has a method getItemViewType(position) to get the item type
                // Set span count based on item type
                return when (imagesAdapter!!.getItemViewType(position)) {
                    101 -> spanCount
                    100 -> 1
                    // Add more cases as needed
                    else -> 1
                }
            }
        }

        recyclerView.layoutManager = gl
        recyclerView.adapter = imagesAdapter

        mediaProgressBar.visibility = View.GONE
    }


    fun notifyAdapter(filteredData: ArrayList<Any>) {
        if (imagesAdapter != null) {
            imagesAdapter?.updateData(filteredData)
        }
    }

    private fun getFormattedDate(dateAdded: Long): String {
////        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
//        return formatter.format(Date(date * 1000))
//
//        val dateAddedInSeconds = dateAdded ?: 0L
//        val dateAddedInMillis = dateAddedInSeconds * 1000
//        val date = Date(dateAddedInMillis)
//        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
//        return dateFormat.format(date)

//        val instant = Instant.ofEpochMilli(dateAddedInMillis)
//        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault())
//        return formatter.format(instant)


        val dateAddedInSeconds = dateAdded ?: 0L
        val dateAddedInMillis = dateAddedInSeconds * 1000

        val localDate = Instant.ofEpochMilli(dateAddedInMillis).atZone(ZoneId.systemDefault()).toLocalDate()

        return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(localDate)
    }

}
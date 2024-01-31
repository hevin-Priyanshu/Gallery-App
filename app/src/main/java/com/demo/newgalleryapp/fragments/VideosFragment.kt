package com.demo.newgalleryapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.newgalleryapp.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.ImagesAd
import com.demo.newgalleryapp.models.MediaModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    var imagesAdapter: ImagesAd? = null
    private var position: Int = 0
    private val commonList: ArrayList<Any> = arrayListOf()


    // Use this method to create new instances of the GalleryFragment with arguments
    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int): VideosFragment {
            val fragment = VideosFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.fragment_videos, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_videos)

        arguments?.let {
            position = it.getInt(ARG_POSITION, 0)

            if (position == 1) {
                val count =
                    (requireActivity().application as AppClass).mainViewModel.sharedPreferencesHelper.getGridColumns()
                observeAllData(count)
            }
        }
        return view
    }


    fun observeAllData(spanCount: Int) {

        (requireActivity().application as AppClass).mainViewModel.videosData.observe(
            viewLifecycleOwner
        ) { videoList ->

            commonList.clear()
            (requireActivity().application as AppClass).mainViewModel.tempVideoList.clear()

            // Sort the list of images based on date
            val sortedPhotos = videoList.sortedByDescending { it.date }
            (requireActivity().application as AppClass).mainViewModel.tempVideoList.addAll(
                sortedPhotos
            )

            val groupedPhotos: List<Map.Entry<String, List<MediaModel>>> =
                sortedPhotos.groupBy { getFormattedDate(it.date) }.entries.toList()

            for (entry in groupedPhotos) {
                val date = entry.key
                commonList.add(date)
                for (image in entry.value) {
                    commonList.add(image)
                }
            }

            loadLayout(spanCount)
        }
    }


    private fun loadLayout(spanCount: Int) {
        imagesAdapter = ImagesAd(requireActivity(), commonList, -1)
        val gl =
            GridLayoutManager(requireActivity(), spanCount, LinearLayoutManager.VERTICAL, false)

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
    }

    fun notifyAdapter(filteredData: ArrayList<Any>) {
        imagesAdapter?.updateData(filteredData)
    }

    private fun getFormattedDate(date: Long): String {
//        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return formatter.format(Date(date * 1000))
    }

}
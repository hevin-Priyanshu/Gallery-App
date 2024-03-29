package com.demo.newgalleryapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.adapters.ImagesAd
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.models.MediaModel
import com.demo.newgalleryapp.viewmodel.MainViewModel
import com.demo.newgalleryapp.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
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
                    Log.e("from", "onCreateView: PhotosFragment")
                    observeAllData(count, "Photo")
                }
            }
        }

        return view
    }

    fun observeAllData(spanCount: Int, from: String) {

        Log.e("from", "observeAllData12222:----------------- $from --- ")

        (requireActivity().application as AppClass).mainViewModel.photosData.observe(
            viewLifecycleOwner
        ) { photosList ->

            Log.e("from", "observeAllData:----------------- $from --- ")
            commonList.clear()
            (requireActivity().application as AppClass).mainViewModel.tempPhotoList.clear()

            // Sort the list of images based on date
            val sortedPhotos = photosList.sortedByDescending { it.date }
            (requireActivity().application as AppClass).mainViewModel.tempPhotoList.addAll(
                sortedPhotos
            )

            val groupedPhotos: List<Map.Entry<String, List<MediaModel>>> =
                sortedPhotos.groupBy { getFormattedDate(it.date) }.entries.toList()


            groupedPhotos.forEach { (date, images) ->
                commonList.add(date)
                commonList.addAll(images)
            }
            loadLayout(spanCount)
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

        MediaFragment.mediaProgressBar.visibility = View.GONE

        if (commonList.isNotEmpty()) {
            MediaFragment.threeDotItem.visibility = View.VISIBLE
            MediaFragment.openFavoriteActivity.visibility = View.VISIBLE
        }

    }


    private fun getFormattedDate(dateAdded: Long): String {
        val dateAddedInSeconds = dateAdded ?: 0L
        val dateAddedInMillis = dateAddedInSeconds * 1000

        val addedDate =
            Instant.ofEpochMilli(dateAddedInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val currentDate = LocalDate.now()

        return when (addedDate) {
            currentDate -> "Today"
//            currentDate.plusDays(1) -> "Tomorrow"
            currentDate.minusDays(1) -> "Yesterday"
            else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(addedDate)
        }
    }

}
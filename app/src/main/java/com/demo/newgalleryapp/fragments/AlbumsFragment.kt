package com.demo.newgalleryapp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.newgalleryapp.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.MainScreenActivity
import com.demo.newgalleryapp.adapters.FolderAdapter
import com.demo.newgalleryapp.models.Folder
import com.demo.newgalleryapp.models.MediaModel
import java.io.File

class AlbumsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var searchEditTextAlbum: EditText
    private lateinit var search_close_btn: ImageView
    private var tempFolderList: ArrayList<Folder> = ArrayList()

    companion object {
        fun newInstance(): AlbumsFragment {
            val fragment = AlbumsFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        tempFolderList = (requireActivity().application as AppClass).mainViewModel.folderList
        val view: View = inflater.inflate(R.layout.fragment_albums, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_album)
        searchEditTextAlbum = view.findViewById(R.id.searchEditText_album)
        search_close_btn = view.findViewById(R.id.search_close_btn_album)



        search_close_btn.setOnClickListener {
            searchEditTextAlbum.text.clear()
            observeAllData()
        }


        searchEditTextAlbum.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank()) {
                    search_close_btn.visibility = View.GONE
                    observeAllData()

                } else {
                    search_close_btn.visibility = View.VISIBLE
                }
                filterData(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    search_close_btn.visibility = View.GONE
                    observeAllData()
                }
            }
        })
        observeAllData()

        return view
    }


    private fun filterData(query: String) {
        val filteredList = if (query.isNotEmpty()) {
            // Filter the original data list based on the search query
            tempFolderList.filter { item ->
                item.name.contains(query, ignoreCase = true)
            } as ArrayList<Folder>
        } else {
            // If the query is empty, show all folders
            tempFolderList
        }

        // Update the adapter with the filtered list
        folderAdapter.updateData(filteredList)
    }

    private fun observeAllData() {
        (requireActivity().application as AppClass).mainViewModel.allData.observe(viewLifecycleOwner) {
            val folders: List<Folder> = it.groupBy { File(it.path).parent }.map { (path, models) ->
                Folder(path!!, models as ArrayList<MediaModel>)
            }
            (requireActivity().application as AppClass).mainViewModel.folderList.clear()
            (requireActivity().application as AppClass).mainViewModel.folderList.addAll(folders)
            recyclerView.layoutManager =
                GridLayoutManager(requireContext(), 3, LinearLayoutManager.VERTICAL, false)
            folderAdapter = FolderAdapter(
                requireActivity(),
                folders as ArrayList<Folder>,
                requireActivity() as MainScreenActivity,
                "normal"
            )
            recyclerView.adapter = folderAdapter
        }
    }

}
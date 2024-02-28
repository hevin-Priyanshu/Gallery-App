package com.demo.newgalleryapp.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.FavoriteImagesActivity
import com.demo.newgalleryapp.activities.MainScreenActivity
import com.demo.newgalleryapp.adapters.FolderAdapter
import com.demo.newgalleryapp.classes.AppClass
import com.demo.newgalleryapp.models.Folder
import com.demo.newgalleryapp.viewmodel.MainViewModel
import com.demo.newgalleryapp.viewmodel.MainViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class AlbumsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    var folderAdapter: FolderAdapter? = null
    private lateinit var searchEditTextAlbum: EditText
    private lateinit var searchCloseBtn: ImageView
    private var tempFolderList: ArrayList<Folder> = ArrayList()
    private lateinit var openFavoriteActivity: ImageView

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
        val view: View = inflater.inflate(R.layout.fragment_albums, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_album)
        searchEditTextAlbum = view.findViewById(R.id.searchEditText_album)
        searchCloseBtn = view.findViewById(R.id.search_close_btn_album)
        openFavoriteActivity = view.findViewById(R.id.favorites_album)

        observeAllData()

        openFavoriteActivity.setOnClickListener {
            val intent = Intent(requireContext(), FavoriteImagesActivity::class.java)
            startActivity(intent)
        }

        searchCloseBtn.setOnClickListener {
            searchEditTextAlbum.text.clear()
            hideKeyboard()
            observeAllData()
        }

        searchEditTextAlbum.isCursorVisible = false
        searchEditTextAlbum.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide the keyboard
                hideKeyboard()
                // Perform your search logic here
                // You can access the text entered in the EditText using searchEvent.text.toString()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        searchEditTextAlbum.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank()) {

                    searchCloseBtn.visibility = View.GONE
                    observeAllData()
                } else {
                    searchCloseBtn.visibility = View.VISIBLE
                }
                searchEditTextAlbum.isCursorVisible = true
                filterData(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    searchCloseBtn.visibility = View.GONE
                    searchEditTextAlbum.isCursorVisible = false
                    hideKeyboard()
                    observeAllData()
                }
            }
        })

        return view
    }


    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditTextAlbum.windowToken, 0)
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
        if (folderAdapter != null) {
            folderAdapter?.updateData(filteredList)
        }
    }

//    private fun observeAllData() {
//        (requireActivity().application as AppClass).mainViewModel.allData.observe(viewLifecycleOwner) {
//            val folders: List<Folder> = it.groupBy { File(it.path).parent }.map { (path, models) ->
//                Folder(path!!, models as ArrayList<MediaModel>)
//            }
//
//            lifecycleScope.launch(Dispatchers.IO) {
//                (requireActivity().application as AppClass).mainViewModel.folderList.clear()
//                (requireActivity().application as AppClass).mainViewModel.folderList.addAll(folders)
//            }
//
//            val folderList: ArrayList<Folder> =
//                (requireActivity().application as AppClass).mainViewModel.folderList
//
//            recyclerView.layoutManager = GridLayoutManager(requireContext(), 3, LinearLayoutManager.VERTICAL, false)
//            folderAdapter = FolderAdapter(requireActivity(), folderList, requireActivity() as MainScreenActivity, "FromFolder")
//            recyclerView.adapter = folderAdapter
//        }
//    }

    private fun observeAllData() {
        val mainViewModel = (requireActivity().application as AppClass).mainViewModel

        mainViewModel.allData.observe(viewLifecycleOwner) { mediaList ->
            val folders: List<Folder> =
                mediaList.groupBy { File(it.path).parent }.map { (path, models) ->
                    Folder(path ?: "", ArrayList(models))
                }

            lifecycleScope.launch(Dispatchers.IO) {
                mainViewModel.folderList.clear()
                mainViewModel.folderList.addAll(folders)
            }

            setupRecyclerView(mainViewModel.folderList)
        }
    }

    private fun setupRecyclerView(folderList: ArrayList<Folder>) {
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        folderAdapter = FolderAdapter(
            requireActivity(), folderList, requireActivity() as MainScreenActivity, "FromFolder"
        )
        recyclerView.adapter = folderAdapter
    }

}
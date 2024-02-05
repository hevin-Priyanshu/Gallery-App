package com.demo.newgalleryapp.fragments

import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.demo.newgalleryapp.AppClass
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.MainScreenActivity
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.bottomNavigationView
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.bottomNavigationViewForLongSelect
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.photosFragment
import com.demo.newgalleryapp.activities.MainScreenActivity.Companion.videosFragment
import com.demo.newgalleryapp.activities.SlideShowActivity
import com.demo.newgalleryapp.adapters.ViewPagerAdapter
import com.demo.newgalleryapp.interfaces.ImageClickListener
import com.demo.newgalleryapp.models.MediaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaFragment : Fragment(), ImageClickListener {

    private lateinit var textPhoto: TextView
    private lateinit var textVideo: TextView
    private lateinit var selectItem: TextView
    private lateinit var searchEvent: EditText
    private lateinit var threeDotItem: ImageView
    private lateinit var closeBtnMedia: ImageView
    private lateinit var searchCloseBtn: ImageView
    private lateinit var toolbar: Toolbar
    lateinit var viewPager: ViewPager
    private var allMediaList: ArrayList<MediaModel> = ArrayList()
    private var popupWindow: PopupWindow? = null
    private var popupWindow2: PopupWindow? = null
    private var selectedColumns = 0

    companion object {

        lateinit var textViewSelectAllMedia: TextView
        lateinit var textViewDeSelectAllMedia: TextView
        lateinit var linearLayoutForMainText: LinearLayout
        lateinit var linearLayoutForSelectText: LinearLayout
        lateinit var mediaProgressBar: ProgressBar

        fun newInstance(): MediaFragment {
            val fragment = MediaFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.fragment_media, container, false)

        initializeViews(view)
        setupViewPager()

        closeBtnMedia.setOnClickListener {
            setAllVisibility()
        }

        mediaProgressBar.visibility = View.VISIBLE

        searchCloseBtn.setOnClickListener {
            searchEvent.text.clear()
            lifecycleScope.launch {
                photosFragment.observeAllData(selectedColumns)
                videosFragment.observeAllData(selectedColumns)
            }
        }

        textViewSelectAllMedia.setOnClickListener {

            if (viewPager.currentItem == 0) {
                val photoList =
                    (requireActivity().application as AppClass).mainViewModel.tempPhotoList

                photosFragment.imagesAdapter?.isSelected = true
                photosFragment.imagesAdapter?.updateSelectionState(true)
                photosFragment.imagesAdapter?.checkSelectedList?.clear()
                photosFragment.imagesAdapter?.checkSelectedList?.addAll(photoList)
                (context as MainScreenActivity).mediaFragment.counter(photosFragment.imagesAdapter?.checkSelectedList?.size!!)
                textViewSelectAllMedia.visibility = View.GONE
                textViewDeSelectAllMedia.visibility = View.VISIBLE

            } else {
                val videoList =
                    (requireActivity().application as AppClass).mainViewModel.tempVideoList
                videosFragment.imagesAdapter?.isSelected = true
                videosFragment.imagesAdapter?.updateSelectionState(true)
                photosFragment.imagesAdapter?.checkSelectedList?.clear()
                videosFragment.imagesAdapter?.checkSelectedList?.addAll(videoList)
                (context as MainScreenActivity).mediaFragment.counter(videosFragment.imagesAdapter?.checkSelectedList?.size!!)
                textViewSelectAllMedia.visibility = View.GONE
                textViewDeSelectAllMedia.visibility = View.VISIBLE
            }
        }

        textViewDeSelectAllMedia.setOnClickListener {

            if (viewPager.currentItem == 0) {

                val photoList =
                    (requireActivity().application as AppClass).mainViewModel.tempPhotoList
                photosFragment.imagesAdapter?.isSelected = false
                photosFragment.imagesAdapter?.updateSelectionState(false)
                photosFragment.imagesAdapter?.checkSelectedList?.removeAll(photoList.toSet())
                (context as MainScreenActivity).mediaFragment.counter(photosFragment.imagesAdapter?.checkSelectedList?.size!!)
                textViewDeSelectAllMedia.visibility = View.GONE
                textViewSelectAllMedia.visibility = View.VISIBLE
            } else {

                val videoList =
                    (requireActivity().application as AppClass).mainViewModel.tempVideoList
                videosFragment.imagesAdapter?.isSelected = false
                videosFragment.imagesAdapter?.updateSelectionState(false)
                videosFragment.imagesAdapter?.checkSelectedList?.removeAll(videoList.toSet())
                (context as MainScreenActivity).mediaFragment.counter(videosFragment.imagesAdapter?.checkSelectedList?.size!!)
                textViewDeSelectAllMedia.visibility = View.GONE
                textViewSelectAllMedia.visibility = View.VISIBLE
            }

        }


        textPhoto.setOnClickListener {
            viewPager.currentItem = 0
        }

        textVideo.setOnClickListener {
            viewPager.currentItem = 1
        }

        searchEvent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewPager.visibility = View.GONE
                searchCloseBtn.visibility = View.VISIBLE
                filterData(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    searchCloseBtn.visibility = View.GONE
                }
            }
        })

        val whiteTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val blueTextColor = ContextCompat.getColor(requireContext(), R.color.color_main)

        threeDotItem.setOnClickListener {
            showThreeDotPopup(toolbar)
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {

                photosFragment.imagesAdapter?.isSelected = false
                videosFragment.imagesAdapter?.isSelected = false
                photosFragment.imagesAdapter?.updateSelectionState(false)
                videosFragment.imagesAdapter?.updateSelectionState(false)
                setAllVisibility()

                if (position == 0) {
                    textPhoto.setTextColor(whiteTextColor)
                    textVideo.setTextColor(blueTextColor)
                    textPhoto.background =
                        requireContext().getDrawable(R.drawable.text_photo_background_view)
                    textVideo.background = null
                } else {
                    textPhoto.setTextColor(blueTextColor)
                    textVideo.setTextColor(whiteTextColor)
                    textVideo.background =
                        requireContext().getDrawable(R.drawable.text_video_background_view)
                    textPhoto.background = null

                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        return view
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(childFragmentManager)
        adapter.addFragment(photosFragment)
        adapter.addFragment(videosFragment)
        viewPager.adapter = adapter
    }

    private fun initializeViews(view: View) {
        allMediaList = (requireActivity().application as AppClass).mainViewModel.allMediaList

        viewPager = view.findViewById(R.id.view_pager_main)
        textPhoto = view.findViewById(R.id.text_photo)
        textVideo = view.findViewById(R.id.text_video)
        closeBtnMedia = view.findViewById(R.id.closeBtn_media)
        searchCloseBtn = view.findViewById(R.id.search_close_btn)
        mediaProgressBar = view.findViewById(R.id.media_progress_bar)

        textViewSelectAllMedia = view.findViewById(R.id.textView_selectAll_media)
        textViewDeSelectAllMedia = view.findViewById(R.id.textView_removeAll_media)

        linearLayoutForMainText = view.findViewById(R.id.linearLayoutForMainText_media)
        linearLayoutForSelectText = view.findViewById(R.id.linearLayoutForSelectText_media)

        selectItem = view.findViewById(R.id.select_item_media)
        threeDotItem = view.findViewById(R.id.three_dot_item)
        searchEvent = view.findViewById(R.id.searchEditText_media)
        toolbar = view.findViewById(R.id.toolBar_media)

        selectedColumns =
            (requireActivity().application as AppClass).mainViewModel.sharedPreferencesHelper.getGridColumns()
    }

    private fun filterData(query: String) {

        mediaProgressBar.visibility = View.VISIBLE

        val commonList: ArrayList<Any> = ArrayList()
        lifecycleScope.launch(Dispatchers.IO) {
            // Filter your original data list based on the search query
            val filteredList = allMediaList.filter { item ->
                // Check if the display name contains the query string
                val nameMatch = item.displayName.contains(query, ignoreCase = true)

                // Check if the date matches the query string (assuming your date is stored as a string)
                val dateString = item.date.toString()
                val dateMatch = dateString.contains(query, ignoreCase = true)

                // Return true if either the name or the date matches the query
                nameMatch || dateMatch
            }

            val groupedPhotos: List<Map.Entry<String, List<MediaModel>>> = filteredList.groupBy {
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(
                    Date(it.date * 1000)
                )
            }.entries.toList()

//            for (entry in groupedPhotos) {
//                val date = entry.key
//                commonList.add(date)
//                for (image in entry.value) {
//                    commonList.add(image)
//                }
//            }

            for ((date, itemList) in groupedPhotos) {
                commonList.add(date)
                commonList.addAll(itemList)
            }
            withContext(Dispatchers.Main) {
                photosFragment.notifyAdapter(commonList)
                videosFragment.notifyAdapter(commonList)

                viewPager.visibility = View.VISIBLE
                mediaProgressBar.visibility = View.GONE
            }
        }
    }


//    fun filterDataNew2(query: String) {
//
//        lifecycleScope.launch(Dispatchers.IO) {
//            val commonList: ArrayList<Any> = ArrayList()
//            allMediaList.sortWith(compareBy({ it.displayName }, { it.date }))
//
//            val groupedPhotos: Map<String, List<MediaModel>> = allMediaList.groupBy {
//                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(it.date * 1000))
//            }
//
//            for ((date, itemList) in groupedPhotos) {
//                commonList.add(date)
//                commonList.addAll(itemList)
//            }
//
//            // Notify your adapters with the sorted and formatted list
//            withContext(Dispatchers.Main) {
//                photosFragment.notifyAdapter(commonList)
//                videosFragment.notifyAdapter(commonList)
//            }
//
//        }
//    }

    private fun showThreeDotPopup(anchorView: View) {

        val inflater = requireActivity().layoutInflater
        val popupView: View = inflater.inflate(R.layout.popup_three_item2, null)

        popupWindow = PopupWindow(
            popupView, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT, true
        )

        popupWindow?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val popupTextSelectCheckBox = popupView.findViewById<TextView>(R.id.selectedCheckBoxItem)
        val popupTextColumn = popupView.findViewById<TextView>(R.id.column)
        val popupTextSlideShow = popupView.findViewById<TextView>(R.id.slideShow)

        // Set click listener for popup item (customize as needed)

        popupTextSelectCheckBox.setOnClickListener {
            selectItem.text = "Item Selected 0"
            photosFragment.imagesAdapter?.isSelected = true
            onLongClick()
            photosFragment.imagesAdapter?.notifyDataSetChanged()
            popupWindow?.dismiss()
        }

        popupTextColumn.setOnClickListener {
            openColumnMenu(anchorView)
            popupWindow?.dismiss()
        }

        popupTextSlideShow.setOnClickListener {
            val intent = Intent(requireContext(), SlideShowActivity::class.java)
            intent.putExtra("SlideImagePath", 0)
            intent.putExtra("FromSlideShow", true)
            intent.putExtra("SlideImagePosition", viewPager.currentItem)
            startActivity(intent)
            popupWindow?.dismiss()
        }

        val popupItem = popupView.findViewById<RelativeLayout>(R.id.popupItem_two)

        popupItem.setOnClickListener {
            popupWindow?.dismiss()
        }
        // Set dismiss listener to nullify the reference
        popupWindow?.setOnDismissListener {
            popupWindow = null
        }
    }

    private fun setAllVisibility() {
        photosFragment.imagesAdapter?.updateSelectionState(false)
        videosFragment.imagesAdapter?.updateSelectionState(false)
        bottomNavigationViewForLongSelect.visibility = View.GONE
        linearLayoutForSelectText.visibility = View.GONE
        textViewDeSelectAllMedia.visibility = View.GONE
        linearLayoutForMainText.visibility = View.VISIBLE
        bottomNavigationView.visibility = View.VISIBLE
        textViewSelectAllMedia.visibility = View.VISIBLE
    }

    private fun openColumnMenu(anchorView: View) {

        val inflater = requireContext().getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView2: View = inflater.inflate(R.layout.pop_menu_for_select_column, null)

        popupWindow2 = PopupWindow(
            popupView2,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            true
        )
        val selectedColumns =
            (requireActivity().application as AppClass).mainViewModel.sharedPreferencesHelper.getGridColumns()
        var tempColumn = selectedColumns

        popupWindow2?.showAtLocation(
            anchorView, Gravity.FILL_VERTICAL or Gravity.FILL_HORIZONTAL, 0, 0
        )

        val linearLayout2 = popupView2.findViewById<LinearLayout>(R.id.linearLayout2)
        val linearLayout3 = popupView2.findViewById<LinearLayout>(R.id.linearLayout3)
        val linearLayout4 = popupView2.findViewById<LinearLayout>(R.id.linearLayout4)
        val linearLayout5 = popupView2.findViewById<LinearLayout>(R.id.linearLayout5)


        val checkBox2 = popupView2.findViewById<ImageView>(R.id.Checkbox_2_column)
        val checkBox3 = popupView2.findViewById<ImageView>(R.id.Checkbox_3_column)
        val checkBox4 = popupView2.findViewById<ImageView>(R.id.Checkbox_4_column)
        val checkBox5 = popupView2.findViewById<ImageView>(R.id.Checkbox_5_column)

        val btnOk = popupView2.findViewById<TextView>(R.id.save_column)
        val btnCancel = popupView2.findViewById<TextView>(R.id.cancel_column)


        checkBox2.setImageResource(R.drawable.empty_select_item)
        checkBox3.setImageResource(R.drawable.empty_select_item)
        checkBox4.setImageResource(R.drawable.empty_select_item)
        checkBox5.setImageResource(R.drawable.empty_select_item)


        when (selectedColumns) {
            2 -> {
                checkBox2.setImageResource(R.drawable.right_tick_icon)
            }

            3 -> {
                checkBox3.setImageResource(R.drawable.right_tick_icon)
            }

            4 -> {
                checkBox4.setImageResource(R.drawable.right_tick_icon)
            }

            5 -> {
                checkBox5.setImageResource(R.drawable.right_tick_icon)
            }
        }

        linearLayout2.setOnClickListener {
            checkBox2.setImageResource(R.drawable.empty_select_item)
            checkBox3.setImageResource(R.drawable.empty_select_item)
            checkBox4.setImageResource(R.drawable.empty_select_item)
            checkBox5.setImageResource(R.drawable.empty_select_item)
            checkBox2.setImageResource(R.drawable.right_tick_icon)
            tempColumn = 2
        }
        linearLayout3.setOnClickListener {
            checkBox2.setImageResource(R.drawable.empty_select_item)
            checkBox3.setImageResource(R.drawable.empty_select_item)
            checkBox4.setImageResource(R.drawable.empty_select_item)
            checkBox5.setImageResource(R.drawable.empty_select_item)
            checkBox3.setImageResource(R.drawable.right_tick_icon)
            tempColumn = 3
        }

        linearLayout4.setOnClickListener {
            checkBox2.setImageResource(R.drawable.empty_select_item)
            checkBox3.setImageResource(R.drawable.empty_select_item)
            checkBox4.setImageResource(R.drawable.empty_select_item)
            checkBox5.setImageResource(R.drawable.empty_select_item)
            checkBox4.setImageResource(R.drawable.right_tick_icon)
            tempColumn = 4
        }

        linearLayout5.setOnClickListener {
            checkBox2.setImageResource(R.drawable.empty_select_item)
            checkBox3.setImageResource(R.drawable.empty_select_item)
            checkBox4.setImageResource(R.drawable.empty_select_item)
            checkBox5.setImageResource(R.drawable.empty_select_item)
            checkBox5.setImageResource(R.drawable.right_tick_icon)
            tempColumn = 5
        }


        btnOk.setOnClickListener {
            updateLayoutWithSelectedColumns(tempColumn)
            popupWindow2?.dismiss()
        }

        btnCancel.setOnClickListener {
            popupWindow2?.dismiss()
        }

    }

    private fun updateLayoutWithSelectedColumns(tempColumn: Int) {
        (requireActivity().application as AppClass).mainViewModel.sharedPreferencesHelper.saveGridColumns(
            tempColumn
        )
        photosFragment.observeAllData(tempColumn)
        videosFragment.observeAllData(tempColumn)
        photosFragment.imagesAdapter?.notifyDataSetChanged()
        videosFragment.imagesAdapter?.notifyDataSetChanged()
    }

    override fun counter(select: Int) {
        if (select == 0) {
            setAllVisibility()
        }
        selectItem.text = "Item Selected $select"
    }

    override fun onLongClick() {
        linearLayoutForMainText.visibility = View.GONE
        bottomNavigationView.visibility = View.GONE
        linearLayoutForSelectText.visibility = View.VISIBLE
        bottomNavigationViewForLongSelect.visibility = View.VISIBLE
    }
}
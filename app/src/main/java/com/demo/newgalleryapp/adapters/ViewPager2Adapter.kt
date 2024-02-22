package com.demo.newgalleryapp.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.demo.newgalleryapp.models.MediaModel

class ViewPager2Adapter(private val fragments: List<Fragment>, fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

//    private var images: List<MediaModel> = emptyList()

    override fun getItemCount(): Int {
        return fragments.size
    }

//    fun setData(list: List<MediaModel>) {
//        this.images = list
//        notifyDataSetChanged()
//    }


    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}
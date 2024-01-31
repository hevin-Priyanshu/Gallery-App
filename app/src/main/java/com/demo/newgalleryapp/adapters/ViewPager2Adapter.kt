package com.demo.newgalleryapp.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.demo.newgalleryapp.models.MediaModel

class ViewPager2Adapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    private val fragments: MutableList<Fragment> = mutableListOf()
    private var images: List<MediaModel> = emptyList()

    override fun getItemCount(): Int {
        return fragments.size
    }

    fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
        notifyDataSetChanged()
    }

    fun setData(list: List<MediaModel>) {
        this.images = list
        notifyDataSetChanged()
    }


    fun clearFragments() {
        fragments.clear()
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}
package com.demo.newgalleryapp.adapters

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.demo.newgalleryapp.models.MediaModel


class ViewPagerAdapter(fm: FragmentManager) :
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments: MutableList<Fragment> = ArrayList()
    private var images: List<MediaModel> = arrayListOf()

    override fun getCount(): Int {
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

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        try {
            super.restoreState(state, loader)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
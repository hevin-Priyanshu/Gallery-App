package com.demo.newgalleryapp.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.FavoriteImagesActivity
import com.demo.newgalleryapp.activities.TrashBinActivity

class SettingFragment : Fragment() {

    private lateinit var favorites: LinearLayout
    private lateinit var trash: LinearLayout
    private lateinit var language: LinearLayout
    private lateinit var privacyPolicy: LinearLayout
    private lateinit var rateUs: LinearLayout
    private lateinit var shareApp: LinearLayout

    companion object{
        fun newInstance(): SettingFragment {
            val fragment = SettingFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_setting, container, false)

        favorites = view.findViewById(R.id.favorite_click_setting)
        trash = view.findViewById(R.id.trash_click_setting)
        language = view.findViewById(R.id.language_click_setting)
        privacyPolicy = view.findViewById(R.id.privacy_click_setting)
        rateUs = view.findViewById(R.id.rateUs_click_setting)
        shareApp = view.findViewById(R.id.share_click_setting)


        favorites.setOnClickListener {
            val intent = Intent(requireContext(), FavoriteImagesActivity::class.java)
            startActivity(intent)
        }

        trash.setOnClickListener {
            val intent = Intent(requireContext(), TrashBinActivity::class.java)
            startActivity(intent)
        }

        language.setOnClickListener {
            Toast.makeText(requireContext(), "Coming Soon....", Toast.LENGTH_SHORT).show()
        }


        privacyPolicy.setOnClickListener {

        }

        rateUs.setOnClickListener {

        }

        shareApp.setOnClickListener {
            val appPackageName = requireContext().packageName
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out this amazing app: https://play.google.com/store/apps/$appPackageName"
                )
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }


        return view
    }

}
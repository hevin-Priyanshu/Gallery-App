package com.demo.newgalleryapp.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.demo.newgalleryapp.R
import com.demo.newgalleryapp.activities.FavoriteImagesActivity
import com.demo.newgalleryapp.activities.TrashBinActivity
import com.demo.newgalleryapp.sharePreference.SharedPreferencesHelper
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_CHANGES_IN_TRASH_ACTIVITY
import com.demo.newgalleryapp.utilities.CommonFunctions.REQ_CODE_FOR_OPEN_DOCUMENT_TREE_REQUEST_CODE

class SettingFragment : Fragment() {

    private lateinit var favorites: LinearLayout
    private lateinit var trash: LinearLayout
    private lateinit var language: LinearLayout
    private lateinit var privacyPolicy: LinearLayout
    private lateinit var rateUs: LinearLayout
    private lateinit var shareApp: LinearLayout
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    companion object {
        fun newInstance(): SettingFragment {
            val fragment = SettingFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE_FOR_OPEN_DOCUMENT_TREE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->

//                val contentResolver = requireContext().contentResolver
//                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
////                // Check for the freshest data.
//                contentResolver.takePersistableUriPermission(uri, takeFlags)
//
//                sharedPreferencesHelper.saveUri(uri) // Save for later use
                openTrashBinActivity(uri)
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_setting, container, false)

        favorites = view.findViewById(R.id.favorite_click_setting)
        trash = view.findViewById(R.id.trash_click_setting)
        language = view.findViewById(R.id.language_click_setting)
        privacyPolicy = view.findViewById(R.id.privacy_click_setting)
        rateUs = view.findViewById(R.id.rateUs_click_setting)
        shareApp = view.findViewById(R.id.share_click_setting)
        sharedPreferencesHelper = SharedPreferencesHelper(requireContext())


        favorites.setOnClickListener {
            val intent = Intent(requireContext(), FavoriteImagesActivity::class.java)
            startActivity(intent)
        }

        trash.setOnClickListener {

            val intent = Intent(requireContext(), TrashBinActivity::class.java)
            startActivityForResult(intent, REQ_CODE_FOR_CHANGES_IN_TRASH_ACTIVITY )
//
//            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
//            startActivityForResult(intent, OPEN_DOCUMENT_TREE_REQUEST_CODE)

//            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
//              putExtra("android.content.extra.INITIAL_URI", Uri.parse("/storage/emulated/0/.trashed"))
////                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("/com.google.android.apps.nbu.files.trash.ui.TrashActivity"))
//            }
//            startActivityForResult(intent, OPEN_DOCUMENT_TREE_REQUEST_CODE)
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

    private fun requestDirectoryAccess() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQ_CODE_FOR_OPEN_DOCUMENT_TREE_REQUEST_CODE)
    }


    private fun openTrashBinActivity(storedUri: Uri) {
        val intent = Intent(requireContext(), TrashBinActivity::class.java)
        intent.data = storedUri
        startActivity(intent)
    }
}
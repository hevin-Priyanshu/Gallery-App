package com.demo.newgalleryapp.sharePreference

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

class SharedPreferencesHelper(context: Context) {

    private val PREFS_NAME = "GridPrefs"
    private val KEY_GRID_COLUMNS = "GRID_COLUMN"
    private val KEY_CONTROLLER = "CONTROLLER"
    private val key = "uri"

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveGridColumns(columns: Int) {
        sharedPreferences.edit().putInt(KEY_GRID_COLUMNS, columns).apply()
    }

    fun saveUri(uri: Uri) {
        sharedPreferences.edit().putString(key, uri.toString()).apply()
    }

    fun getGridColumns(): Int {
        return sharedPreferences.getInt(KEY_GRID_COLUMNS, DEFAULT_GRID_COLUMNS)
    }

    fun saveDefaultControlLer(columns: String) {
        sharedPreferences.edit().putString(KEY_CONTROLLER, columns).apply()
    }

    fun getDefaultControlLer(): String? {
        return sharedPreferences.getString(KEY_CONTROLLER, DEFAULT_CONTROLLER)
    }

    fun retrieveTreeUri(): String? {
        return sharedPreferences.getString(key, null)
    }

    companion object {
        const val DEFAULT_GRID_COLUMNS = 4
        const val DEFAULT_CONTROLLER = "normal"
    }
}
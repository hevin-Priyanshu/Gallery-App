package com.demo.newgalleryapp.sharePreference

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesHelper(context: Context) {

    private val PREFS_NAME = "GridPrefs"
    private val KEY_GRID_COLUMNS = "GRID_COLUMN"

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveGridColumns(columns: Int) {
        sharedPreferences.edit().putInt(KEY_GRID_COLUMNS, columns).apply()
    }

    fun getGridColumns(): Int {
        return sharedPreferences.getInt(KEY_GRID_COLUMNS, DEFAULT_GRID_COLUMNS)
    }

    companion object {
        const val DEFAULT_GRID_COLUMNS = 4
    }
}
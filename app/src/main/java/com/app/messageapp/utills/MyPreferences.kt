package com.app.fitnessgym.utils

import android.content.Context

class MyPreferences {

    companion object {
        fun saveStringInPreference(context: Context, key: String, value: String) {
            val preferences = context.getSharedPreferences("Fitness_gym", Context.MODE_PRIVATE)
            preferences.edit().putString(key, value).apply()
        }

        fun getFromPreferences(context: Context, key: String): String? {
            return context.getSharedPreferences("Fitness_gym", Context.MODE_PRIVATE).getString(key, "")
        }
        
        fun clearData(context: Context) {
            val preferences = context.getSharedPreferences("Fitness_gym", Context.MODE_PRIVATE)
            preferences.edit().clear().apply()
        }
    }
}
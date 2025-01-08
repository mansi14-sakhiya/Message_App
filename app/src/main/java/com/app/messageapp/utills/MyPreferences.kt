package com.app.fitnessgym.utils

import android.content.Context
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson

class MyPreferences {

    companion object {
        private const val PREF_NAME = "MessageApp"
        private const val ARCHIVED_CHATS_KEY = "ArchivedUsers"

        fun saveStringInPreference(context: Context, key: String, value: String) {
            val preferences = context.getSharedPreferences("MessageApp", Context.MODE_PRIVATE)
            preferences.edit().putString(key, value).apply()
        }

        fun getFromPreferences(context: Context, key: String): String? {
            return context.getSharedPreferences("MessageApp", Context.MODE_PRIVATE).getString(key, "")
        }
        
        fun clearData(context: Context) {
            val preferences = context.getSharedPreferences("MessageApp", Context.MODE_PRIVATE)
            preferences.edit().clear().apply()
        }

        fun saveArchivedUsers(context: Context, archivedIds: Set<String>) {
            val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val json = Gson().toJson(archivedIds)
            preferences.edit().putString(ARCHIVED_CHATS_KEY, json).apply()
        }

        fun getArchivedUsers(context: Context): Set<String> {
            val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val json = preferences.getString(ARCHIVED_CHATS_KEY, null)
            return if (json != null) {
                val type = object : TypeToken<Set<String>>() {}.type
                Gson().fromJson(json, type)
            } else {
                emptySet()
            }
        }
    }
}
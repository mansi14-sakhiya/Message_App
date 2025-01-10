package com.app.fitnessgym.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson

class MyPreferences {

    companion object {
        private const val PREF_NAME = "MessageApp"
        private const val ARCHIVED_CHATS_KEY = "ArchivedUsers"

        fun saveStringInPreference(context: Context, key: String, value: String) {
            val preferences = context.getSharedPreferences("MessageApp", MODE_PRIVATE)
            preferences.edit().putString(key, value).apply()
        }

        fun getFromPreferences(context: Context, key: String): String? {
            return context.getSharedPreferences("MessageApp", MODE_PRIVATE).getString(key, "")
        }
        
        fun clearData(context: Context) {
            val preferences = context.getSharedPreferences("MessageApp", MODE_PRIVATE)
            preferences.edit().clear().apply()
        }

         fun addBlockedUsers(context: Context, phoneNumbers: List<String>) {
            val sharedPreferences = context.getSharedPreferences("BlockedUsers", MODE_PRIVATE)
            val blockedUsers = sharedPreferences.getStringSet("BLOCKED_NUMBERS", HashSet()) ?: HashSet()
            blockedUsers.addAll(phoneNumbers)
            sharedPreferences.edit().putStringSet("BLOCKED_NUMBERS", blockedUsers).apply()
        }

         fun getBlockedUsers(context: Context): Set<String> {
            val sharedPreferences = context.getSharedPreferences("BlockedUsers", MODE_PRIVATE)
            return sharedPreferences.getStringSet("BLOCKED_NUMBERS", HashSet()) ?: emptySet()
        }

         fun removeBlockedUsers(context: Context, phoneNumbers: List<String>) {
            val sharedPreferences = context.getSharedPreferences("BlockedUsers", MODE_PRIVATE)
            val blockedUsers = sharedPreferences.getStringSet("BLOCKED_NUMBERS", HashSet()) ?: HashSet()
            blockedUsers.removeAll(phoneNumbers)
            sharedPreferences.edit().putStringSet("BLOCKED_NUMBERS", blockedUsers).apply()
        }

         fun addArchivedChats(context: Context, threadIds: List<String>) {
            val sharedPreferences = context.getSharedPreferences("ArchivedChats", MODE_PRIVATE)
            val archivedChats = sharedPreferences.getStringSet("ARCHIVED_THREADS", HashSet()) ?: HashSet()
            archivedChats.addAll(threadIds)
            sharedPreferences.edit().putStringSet("ARCHIVED_THREADS", archivedChats).apply()
        }

         fun getArchivedChats(context: Context): Set<String> {
            val sharedPreferences = context.getSharedPreferences("ArchivedChats", MODE_PRIVATE)
            return sharedPreferences.getStringSet("ARCHIVED_THREADS", HashSet()) ?: emptySet()
        }

         fun removeArchivedChats(context: Context, threadIds: List<String>) {
            val sharedPreferences = context.getSharedPreferences("ArchivedChats", MODE_PRIVATE)
            val archivedChats = sharedPreferences.getStringSet("ARCHIVED_THREADS", HashSet()) ?: HashSet()
            archivedChats.removeAll(threadIds)
            sharedPreferences.edit().putStringSet("ARCHIVED_THREADS", archivedChats).apply()
        }

    }
}
package com.app.messageapp.utills

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

@Suppress("DEPRECATION")
class LocalizationApp {
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        // Update application context if applicable
        if (context.applicationContext != null) {
            context.applicationContext.resources.updateConfiguration(configuration, resources.displayMetrics)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            context.createConfigurationContext(configuration)
        } else {
            resources.updateConfiguration(configuration, resources.displayMetrics)
            context
        }
    }
}
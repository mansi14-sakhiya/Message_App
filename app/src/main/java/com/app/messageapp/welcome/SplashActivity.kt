package com.app.messageapp.welcome

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.app.fitnessgym.utils.MyPreferences
import com.app.messageapp.chat.view.MessageListActivity
import com.app.messageapp.databinding.ActivitySplashBinding
import com.app.messageapp.language.view.LanguageActivity
import com.app.messageapp.utills.LocalizationApp
import com.app.myapplication.utils.Constant

@Suppress("DEPRECATION")
class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
    }

    private fun initData() {
        val userLanguage = MyPreferences.getFromPreferences(this, Constant.userLanguage).toString()
        Log.e("getLoged", "-0-----${userLanguage}")
        val localizationApp = LocalizationApp()
        localizationApp.setLocale(this, userLanguage)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        Handler(Looper.getMainLooper()).postDelayed({
            if (MyPreferences.getFromPreferences(this, Constant.isLanguageSelected).toString() == "selected") {
                 startActivity(
                    Intent(this, MessageListActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            } else {
                startActivity(
                    Intent(this, LanguageActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }

        }, 3000)
    }
}
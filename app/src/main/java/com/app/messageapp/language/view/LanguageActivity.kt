package com.app.messageapp.language.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.fitnessgym.utils.MyPreferences
import com.app.messageapp.R
import com.app.messageapp.chat.view.MessageListActivity
import com.app.messageapp.databinding.ActivityLanguageBinding
import com.app.messageapp.language.adapter.LanguageAdapter
import com.app.messageapp.language.dataModel.LanguageDataModel
import com.app.messageapp.language.dataModel.LanguageSelectionCallback
import com.app.messageapp.utills.LocalizationApp
import com.app.myapplication.utils.Constant

class LanguageActivity : AppCompatActivity(), LanguageSelectionCallback {
    private lateinit var binding: ActivityLanguageBinding

    private lateinit var languageAdapter: LanguageAdapter
    private val languages = ArrayList<LanguageDataModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        setOnClickViews()
    }

    private fun initData() {
        languages.add(LanguageDataModel(R.drawable.ic_english, "English", "(English)", true))
        languages.add(LanguageDataModel(R.drawable.ic_hindi, "Hindi", "(हिंदी)", false))
        languages.add(LanguageDataModel(R.drawable.ic_spanish, "Spanish", "(Española)", false))
        languages.add(LanguageDataModel(R.drawable.ic_arabic, "Arabic", "(العربية)", false))
        languages.add(LanguageDataModel(R.drawable.ic_german, "German", "(Deutsch)", false))
        languages.add(LanguageDataModel(R.drawable.ic_france, "French", "(Français)", false))
        languages.add(LanguageDataModel(R.drawable.ic_portugues, "Portuguese", "(Português)", false))

        languageAdapter = LanguageAdapter(languages, this)
        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        binding.rvLanguages.adapter = languageAdapter
    }

    private fun setOnClickViews() {
        binding.btnDone.setOnClickListener {
//            MyPreferences.saveStringInPreference(this, Constant.isLanguageSelected, "selected")
            startActivity(
                Intent(this, MessageListActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }
    }

    override fun onLanguageSelected(position: Int) {
        val localizationApp = LocalizationApp()
        Log.e("getLoged", "---${languages[position].name}")
        when(languages[position].name) {
            "English" -> {
                localizationApp.setLocale(this, "en")
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "en")
            }
            "Hindi" -> {
                localizationApp.setLocale(this, "hi")
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "hi")
            }
            "Spanish" -> {
                localizationApp.setLocale(this, "es")
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "es")
            }
            "Arabic" -> {
                localizationApp.setLocale(this, "ae")
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "ae")
            }
            "German" -> {
                localizationApp.setLocale(this, "de")
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "de")
            }
            "French" -> {
                localizationApp.setLocale(this, "fr")
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "fr")
            }
            "Portuguese" -> {
                localizationApp.setLocale(this, "pt")
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "pt")
            }
        }
    }
}
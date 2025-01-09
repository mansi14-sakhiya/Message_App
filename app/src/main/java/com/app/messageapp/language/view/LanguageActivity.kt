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
        var isEnglish = false
        var isHindi = false
        var isSpanish = false
        var isArabic = false
        var isGerman = false
        var isFrench = false
        var isPortuguese = false

            when(MyPreferences.getFromPreferences(this, Constant.userLanguage).toString()) {
                "en" -> isEnglish = true
                "hi" -> isHindi = true
                "es" -> isSpanish = true
                "ae" -> isArabic = true
                "de" -> isGerman = true
                "fr" -> isFrench = true
                "pt" -> isPortuguese = true
            }

        languages.add(LanguageDataModel(R.drawable.ic_english, "English", "(English)", isEnglish))
        languages.add(LanguageDataModel(R.drawable.ic_hindi, "Hindi", "(हिंदी)", isHindi))
        languages.add(LanguageDataModel(R.drawable.ic_spanish, "Spanish", "(Española)", isSpanish))
        languages.add(LanguageDataModel(R.drawable.ic_arabic, "Arabic", "(العربية)", isArabic))
        languages.add(LanguageDataModel(R.drawable.ic_german, "German", "(Deutsch)", isGerman))
        languages.add(LanguageDataModel(R.drawable.ic_france, "French", "(Français)", isFrench))
        languages.add(LanguageDataModel(R.drawable.ic_portugues, "Portuguese", "(Português)", isPortuguese))

        languageAdapter = LanguageAdapter(languages, this)
        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        binding.rvLanguages.adapter = languageAdapter
    }

    private fun setOnClickViews() {
        binding.btnDone.setOnClickListener {
            val userLanguage = MyPreferences.getFromPreferences(this, Constant.userLanguage).toString()
            val localizationApp = LocalizationApp()
            localizationApp.setLocale(this, userLanguage)
            val intent = Intent(this, MessageListActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    override fun onLanguageSelected(position: Int) {
        Log.e("getLoged", "---${languages[position].name}")
        when(languages[position].name) {
            "English" -> {
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "en")
            }
            "Hindi" -> {
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "hi")
            }
            "Spanish" -> {
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "es")
            }
            "Arabic" -> {
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "ae")
            }
            "German" -> {
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "de")
            }
            "French" -> {
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "fr")
            }
            "Portuguese" -> {
                MyPreferences.saveStringInPreference(this, Constant.userLanguage, "pt")
            }
        }
    }
}
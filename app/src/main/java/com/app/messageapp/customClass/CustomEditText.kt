package com.app.messageapp.customClass

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class CustomEditText  : AppCompatEditText {

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        applyCustomFont(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
            context,
            attrs,
            defStyle
    ) {

        applyCustomFont(context, attrs)
    }

    private fun applyCustomFont(context: Context, attrs: AttributeSet) {
        val textStyle = attrs.getAttributeIntValue(ANDROID_SCHEMA, "textStyle", Typeface.NORMAL)
        typeface = selectTypeface(context, textStyle)
    }

    private fun selectTypeface(context: Context, textStyle: Int): Typeface {
      return when (textStyle) {
        Typeface.BOLD -> Typeface.createFromAsset(context.assets, "axiforma_bold.otf")
        Typeface.NORMAL -> Typeface.createFromAsset(context.assets, "axiforma_regular.otf")// regular
        else -> Typeface.createFromAsset(context.assets, "axiforma_medium.otf")// regular
      }
    }

    companion object {
        const val ANDROID_SCHEMA = "http://schemas.android.com/apk/res/android"
    }
}
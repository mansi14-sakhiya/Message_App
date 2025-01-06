package com.app.messageapp.language.dataModel

data class LanguageDataModel(
    val flagResId: Int,
    val name: String,
    val nativeName: String,
    var isSelected: Boolean
)
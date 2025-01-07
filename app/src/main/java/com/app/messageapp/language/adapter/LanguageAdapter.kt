package com.app.messageapp.language.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.messageapp.R
import com.app.messageapp.databinding.RawLanguageBinding
import com.app.messageapp.language.dataModel.LanguageDataModel
import com.app.messageapp.language.dataModel.LanguageSelectionCallback

class LanguageAdapter(
    private val languages: List<LanguageDataModel>,
    private val callback: LanguageSelectionCallback
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    inner class LanguageViewHolder(val binding: RawLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) { }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = RawLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val language = languages[position]
        holder.binding.ivLanguageName.setImageResource(language.flagResId)
        holder.binding.tvLanguageName.text = language.name
        holder.binding.tvLanguageType.text = language.nativeName
        holder.binding.languageCheckBox.isChecked = language.isSelected

        if (language.isSelected) {
            holder.binding.main.setBackgroundResource(R.drawable.bg_white_border_gray)
            callback.onLanguageSelected(position)
        }

        holder.binding.root.setOnClickListener {
            languages.forEach { it.isSelected = false }
            language.isSelected = true
            notifyDataSetChanged()
            callback.onLanguageSelected(position)
        }

        holder.binding.languageCheckBox.setOnClickListener {
            languages.forEach { it.isSelected = false }
            language.isSelected = true
            notifyDataSetChanged()
            callback.onLanguageSelected(position)
        }
    }

    override fun getItemCount(): Int = languages.size
}
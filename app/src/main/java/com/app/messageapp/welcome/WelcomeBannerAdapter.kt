package com.app.messageapp.welcome

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.messageapp.databinding.RawBannerViewBinding

class WelcomeBannerAdapter(private val bannerList: ArrayList<WelcomeBannerDataModel>) :
    RecyclerView
    .Adapter<WelcomeBannerAdapter
    .ViewHolder>() {

    class ViewHolder(val binding: RawBannerViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RawBannerViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.ivBanner.setImageResource(bannerList[position].image)
        holder.binding.tvBannerTitle.text = bannerList[position].title
        holder.binding.tvBannerDescription.text = bannerList[position].description

    }

    override fun getItemCount() = bannerList.size
}
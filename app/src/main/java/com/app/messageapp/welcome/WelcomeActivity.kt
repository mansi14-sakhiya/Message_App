package com.app.messageapp.welcome

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.app.messageapp.R
import com.app.messageapp.databinding.ActivityWelcomeBinding
import com.google.android.material.tabs.TabLayoutMediator

class WelcomeActivity : AppCompatActivity() {
    lateinit var binding: ActivityWelcomeBinding

    /** variables are used for adapter **/
    private lateinit var welcomeBannerAdapter: WelcomeBannerAdapter

    private var bannerList = ArrayList<WelcomeBannerDataModel>()

    private var bannerPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        setOnClickViews()
    }

    private fun initData() {
        bannerList.add(WelcomeBannerDataModel(R.drawable.ic_welcome_one,getString(R.string.read_message), getString(R.string.mention_any_additional_features_like_read_receipts_typing_indicators_or_message)))
        bannerList.add(WelcomeBannerDataModel(R.drawable.ic_welcome_two,
            getString(R.string.free_voice_calls),
            getString(R.string.explain_the_capability_to_send_voice_messages_without_any_charges)))
        bannerList.add(WelcomeBannerDataModel(R.drawable.ic_welcome_three,
            getString(R.string.groups),
            getString(R.string.highlight_the_ability_to_create_and_join_groups_for_different_purposes)))

       welcomeBannerAdapter = WelcomeBannerAdapter(bannerList)
        binding.vpWelcome.adapter = welcomeBannerAdapter
        binding.vpWelcome.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.vpWelcome.offscreenPageLimit = 1
        viewpagerAdapter()
        binding.vpWelcome.isSaveEnabled = false

        binding.vpWelcome.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.vpWelcome.isUserInputEnabled = false
                val recyclerView = binding.vpWelcome.getChildAt(0) as? RecyclerView
                val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position) as? WelcomeBannerAdapter.ViewHolder
                if (viewHolder != null) {
                    bannerPosition = position
                    when (position) {
                        0 -> binding.btnNext.text = getString(R.string.next)

                        1 -> binding.btnNext.text = getString(R.string.next)

                        2 -> binding.btnNext.text = getString(R.string.let_s_start)
                    }
                }
            }
        })
    }

    private fun setOnClickViews() {
        binding.btnNext.setOnClickListener {
            if (bannerPosition != welcomeBannerAdapter.itemCount) {
                val newPosition = bannerPosition + 1
                if (newPosition in 0 until welcomeBannerAdapter.itemCount) {
                    binding.vpWelcome.currentItem = newPosition
                }
            } else {
                // start activity
            }
        }
    }

    /** this function product image viewPager **/
    private fun viewpagerAdapter() {
        val nextItemVisiblePx = 0
        val currentItemHorizontalMarginPx = 0
        val pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx
        val pageTransformer = ViewPager2.PageTransformer { page: View, position: Float ->
            page.translationX = -pageTranslationX * position
            page.scaleY = 1 - (0.00f * kotlin.math.abs(position))
        }
        binding.vpWelcome.setPageTransformer(pageTransformer)
//        val tabHomeBanner = TabLayoutMediator(binding.tlIndicator , binding.vpWelcome, true) { _, _ -> }
//        tabHomeBanner.attach()

    }
}
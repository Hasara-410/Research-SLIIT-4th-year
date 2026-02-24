package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabDots: TabLayout
    private lateinit var btnNext: MaterialButton
    private lateinit var tvSkip: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        tabDots = findViewById(R.id.tabDots)
        btnNext = findViewById(R.id.btnNext)
        tvSkip = findViewById(R.id.tvSkip)

        // ✅ For now: 2 pages (we can add more later if needed)
        val items = listOf(
            OnboardingItem(
                imageRes = R.drawable.onb1,  // you will add this image in Step 2.4
                title = "Step into Ancient Lanka",
                desc = "Experience Sri Lanka’s rich heritage in full 3D, anywhere, anytime with LankaLens."
            ),
            OnboardingItem(
                imageRes = R.drawable.onb2,
                title = "Explore & Learn",
                desc = "Navigate heritage zones, unlock places, and discover stories through interactive features."
            )
        )

        viewPager.adapter = OnboardingAdapter(items)

        TabLayoutMediator(tabDots, viewPager) { _, _ -> }.attach()

        tvSkip.setOnClickListener { goNextScreen() }

        btnNext.setOnClickListener {
            val last = items.size - 1
            if (viewPager.currentItem < last) viewPager.currentItem += 1
            else goNextScreen()
        }
    }

    private fun goNextScreen() {
        // After onboarding, go to Sign In (we will build it next step)
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }
}
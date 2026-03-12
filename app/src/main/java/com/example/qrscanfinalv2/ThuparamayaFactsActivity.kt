package com.example.qrscanfinalv2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class ThuparamayaFactsActivity : AppCompatActivity() {

    private lateinit var viewPager   : ViewPager2
    private lateinit var btnNext     : Button
    private lateinit var btnPostQuiz : Button
    private lateinit var layoutDots  : LinearLayout

    private val facts = listOf(
        FactItem(
            emoji   = "🏛️",
            title   = "Sri Lanka's Oldest Stupa",
            fact    = "Thuparamaya is the oldest Buddhist stupa in Sri Lanka, built by King Devanampiya Tissa in the 3rd century BC after receiving the collar bone relic of the Buddha.",
            color   = "#1B3A2D"
        ),
        FactItem(
            emoji   = "🪷",
            title   = "The Sacred Relic",
            fact    = "The stupa enshrines the right collar bone of Lord Buddha, brought to Sri Lanka by Mahinda Thero. It is considered one of the most sacred Buddhist sites in the world.",
            color   = "#1A2A3A"
        ),
        FactItem(
            emoji   = "🏗️",
            title   = "Unique Architecture",
            fact    = "Thuparamaya features a unique 'Gharbaya' bell-shaped dome supported by stone pillars. It has been restored multiple times over 2,300 years and still stands today in Anuradhapura.",
            color   = "#2A1A2A"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thuparamaya_facts)

        viewPager   = findViewById(R.id.viewPager)
        btnNext     = findViewById(R.id.btnNext)
        layoutDots  = findViewById(R.id.layoutDots)
        btnPostQuiz = findViewById(R.id.btnPostQuiz)

        // Setup ViewPager
        viewPager.adapter = FactsAdapter(facts)

        // Setup dots
        setupDots(0)

        // Page change listener
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(position)
                if (position == facts.size - 1) {
                    // Last page — show Post Quiz button
                    btnNext.visibility    = View.GONE
                    btnPostQuiz.visibility = View.VISIBLE
                } else {
                    btnNext.visibility    = View.VISIBLE
                    btnPostQuiz.visibility = View.GONE
                }
            }
        })

        // Next button
        btnNext.setOnClickListener {
            val next = viewPager.currentItem + 1
            if (next < facts.size) viewPager.currentItem = next
        }

        // Post Quiz button — last card
        btnPostQuiz.setOnClickListener {
            startActivity(Intent(this, PostQuizActivity::class.java))
            finish()
        }
    }

    private fun setupDots(activeIndex: Int) {
        layoutDots.removeAllViews()
        facts.forEachIndexed { index, _ ->
            val dot = View(this).apply {
                val size   = if (index == activeIndex) 12 else 8
                val params = LinearLayout.LayoutParams(
                    (size * resources.displayMetrics.density).toInt(),
                    (size * resources.displayMetrics.density).toInt()
                ).also { it.setMargins(6, 0, 6, 0) }
                layoutParams    = params
                setBackgroundColor(
                    if (index == activeIndex) Color.parseColor("#4CAF50")
                    else Color.parseColor("#444444")
                )
                // Round the dot
                background = if (index == activeIndex) {
                    resources.getDrawable(R.drawable.circle_green, null)
                } else {
                    resources.getDrawable(R.drawable.circle_grey, null)
                }
            }
            layoutDots.addView(dot)
        }
    }
}

// ── Data class ────────────────────────────────────────────
data class FactItem(
    val emoji : String,
    val title : String,
    val fact  : String,
    val color : String
)

// ── Facts Adapter ─────────────────────────────────────────
class FactsAdapter(private val facts: List<FactItem>) :
    RecyclerView.Adapter<FactsAdapter.FactViewHolder>() {

    inner class FactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji : TextView = view.findViewById(R.id.tvFactEmoji)
        val tvTitle : TextView = view.findViewById(R.id.tvFactTitle)
        val tvFact  : TextView = view.findViewById(R.id.tvFactContent)
        val card    : androidx.cardview.widget.CardView = view.findViewById(R.id.cardFact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fact_card, parent, false)
        return FactViewHolder(view)
    }

    override fun onBindViewHolder(holder: FactViewHolder, position: Int) {
        val item = facts[position]
        holder.tvEmoji.text = item.emoji
        holder.tvTitle.text = item.title
        holder.tvFact.text  = item.fact
        holder.card.setCardBackgroundColor(Color.parseColor(item.color))
    }

    override fun getItemCount() = facts.size
}
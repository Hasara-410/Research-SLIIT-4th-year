package com.example.qrscanfinalv2

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.apply
import kotlin.collections.count
import kotlin.text.isNullOrBlank
import kotlin.to

class ProgressActivity : AppCompatActivity() {

    private lateinit var tvProgressTitle: TextView
    private lateinit var tvProgressText: TextView
    private lateinit var progressCircle: ProgressBar
    private lateinit var tvPercent: TextView
    private lateinit var listContainer: LinearLayout
    private lateinit var tvCongrats: TextView

    // Local storage (same as MainActivity)
    private val prefs by lazy { getSharedPreferences("offline_visited", MODE_PRIVATE) }
    private fun zoneKey(zoneId: String) = "ZONE:$zoneId"
    private fun placeKey(zoneId: String, placeId: String) = "PLACE:$zoneId|$placeId"

    private fun isZoneVisitedLocal(zoneId: String): Boolean =
        prefs.getBoolean(zoneKey(zoneId), false)

    private fun isPlaceVisitedLocal(zoneId: String, placeId: String): Boolean =
        prefs.getBoolean(placeKey(zoneId, placeId), false)

    // ✅ Same zone/place names as MainActivity
    private val zonePlaceMap = mapOf(
        "SLIIT Malabe" to listOf(
            Place("SLIIT Main Building", 6.914678, 79.973208, 50.0, false),
            Place("SLIIT New Building", 6.9155, 79.9739, 50.0, false)
        ),
        "Sigiriya" to listOf(
            Place("Sigiriya Museum", 7.956970, 80.751570, 80.0, false),
            Place("Water Gardens", 7.956819, 80.755592, 80.0, false),
            Place("Boulder Gardens", 7.958560, 80.759230, 80.0, false),
            Place("Cobra Hood Cave", 7.955961, 80.758440, 40.0, false),
            Place("Mirror Wall", 7.956706, 80.759566, 40.0, false),
            Place("Lion Gate", 7.957565, 80.759900, 40.0, false),
            Place("Summit", 7.957201, 80.759068, 40.0, false)
        ),
        "My Home" to listOf(
            Place("Main Gate", 7.5772998, 80.4511324, 15.0, false),
            Place("Gate2", 7.5771083, 80.4510794, 15.0, false)
        )
    )

    private var currentZone: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        tvProgressTitle = findViewById(R.id.tvProgressTitle)
        tvProgressText = findViewById(R.id.tvProgressText)
        progressCircle = findViewById(R.id.progressCircle)
        tvPercent = findViewById(R.id.tvPercent)
        listContainer = findViewById(R.id.listContainer)
        tvCongrats = findViewById(R.id.tvCongrats)

        // ✅ get zone from MainActivity
        currentZone = intent.getStringExtra("zone")

        // fallback: last known zone stored by MainActivity
        if (currentZone.isNullOrBlank()) {
            currentZone = getSharedPreferences("progress_zone", MODE_PRIVATE)
                .getString("last_zone", null)
        }

        renderProgress()
    }

    override fun onResume() {
        super.onResume()
        renderProgress()
    }

    private fun tintProgressBar(color: Int) {
        // ✅ Safe tint (won’t crash if drawable is null)
        progressCircle.progressDrawable?.setTint(color)
        progressCircle.indeterminateDrawable?.setTint(color)
    }

    private fun renderProgress() {
        listContainer.removeAllViews()

        val zoneName = currentZone

        // If zone is null (user not inside zone), show message
        if (zoneName.isNullOrBlank()) {
            tvProgressTitle.text = "Progress"
            tvProgressText.text = "Go inside a zone to view progress"
            progressCircle.progress = 0
            tvPercent.text = "0%"
            tvCongrats.visibility = View.GONE
            tintProgressBar(Color.parseColor("#2196F3"))

            val hint = TextView(this).apply {
                text = "Tip: Move into SLIIT / Sigiriya / My Home zone, then open Progress."
                textSize = 15f
                setPadding(0, 18, 0, 0)
            }
            listContainer.addView(hint)
            return
        }

        val places = zonePlaceMap[zoneName] ?: emptyList()

        val totalPlaces = places.size
        val visitedPlaces = places.count { isPlaceVisitedLocal(zoneName, it.placeName) }
        val percent = if (totalPlaces == 0) 0 else ((visitedPlaces * 100) / totalPlaces)

        tvProgressTitle.text = "Your Progress"
        tvProgressText.text = "$visitedPlaces / $totalPlaces places completed"
        progressCircle.max = 100
        progressCircle.progress = percent
        tvPercent.text = "$percent%"

        // Zone heading
        val zoneTitle = TextView(this).apply {
            text = "You are in $zoneName Zone" + if (isZoneVisitedLocal(zoneName)) " ✔" else ""
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 10, 0, 12)
        }
        listContainer.addView(zoneTitle)

        // Place list
        for (p in places) {
            val visited = isPlaceVisitedLocal(zoneName, p.placeName)
            val row = TextView(this).apply {
                text = "${p.placeName}  " + if (visited) "✔" else "❌"
                textSize = 16f
                setPadding(0, 6, 0, 6)
            }
            listContainer.addView(row)
        }

        // Congrats
        if (totalPlaces > 0 && visitedPlaces == totalPlaces) {

            tintProgressBar(Color.parseColor("#22B14C")) // green

            val congratsText = TextView(this).apply {
                text = "\nCongratulations! You completed this zone!"
                textSize = 17f
                text = "\nYou completed this zone!"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#22B14C"))
                setPadding(0, 20, 0, 20)
            }

            listContainer.addView(congratsText)

        } else {
            tintProgressBar(Color.parseColor("#B2BEB5")) // blue
        }

    }
}

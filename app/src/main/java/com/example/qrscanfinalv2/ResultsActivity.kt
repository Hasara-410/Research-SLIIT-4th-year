package com.example.qrscanfinalv2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ResultsActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    private lateinit var layoutLoading    : LinearLayout
    private lateinit var layoutResults    : LinearLayout
    private lateinit var tvUserName       : TextView
    private lateinit var tvPreQuiz        : TextView
    private lateinit var tvGame1          : TextView
    private lateinit var tvGame2          : TextView
    private lateinit var tvPostQuiz       : TextView
    private lateinit var tvImprovement    : TextView
    private lateinit var tvImprovementLabel: TextView
    private lateinit var layoutLeaderboard: LinearLayout
    private lateinit var btnHome          : Button

    // Current user's UID — used to highlight their row in leaderboard
    private var currentUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        layoutLoading     = findViewById(R.id.layoutLoading)
        layoutResults     = findViewById(R.id.layoutResults)
        tvUserName        = findViewById(R.id.tvUserName)
        tvPreQuiz         = findViewById(R.id.tvPreQuiz)
        tvGame1           = findViewById(R.id.tvGame1)
        tvGame2           = findViewById(R.id.tvGame2)
        tvPostQuiz        = findViewById(R.id.tvPostQuiz)
        tvImprovement     = findViewById(R.id.tvImprovement)
        tvImprovementLabel= findViewById(R.id.tvImprovementLabel)
        layoutLeaderboard = findViewById(R.id.layoutLeaderboard)
        btnHome           = findViewById(R.id.btnHome)

        currentUid = auth.currentUser?.uid

        showLoading(true)
        loadResults()

        btnHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    // ── Load user scores ────────────────────────────────
    // READ from users collection ONLY for name + quiz scores
    // This is existing app data — we never write here
    private fun loadResults() {
        val uid = currentUid ?: run {
            showLoading(false)
            return
        }

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val name      = doc.getString("name").orEmpty().ifBlank { "Explorer" }
                val preScore  = doc.getLong("preQuizScore")?.toInt()  ?: 0
                val postScore = doc.getLong("postQuizScore")?.toInt() ?: 0

                tvUserName.text = "Well done, $name!"

                tvPreQuiz.text  = "$preScore / 5"
                tvPostQuiz.text = "$postScore / 5"

                // Game scores — read from leaderboard collection (our data)
                loadGameScores(uid) { game1Time, game2Time ->
                    tvGame1.text = if (game1Time != null)
                        formatTime(game1Time) else "Pending"
                    tvGame2.text = if (game2Time != null)
                        formatTime(game2Time) else "Pending"

                    // Learning improvement
                    if (preScore > 0) {
                        val pct = ((postScore - preScore).toDouble() / preScore * 100).toInt()
                        when {
                            pct > 0 -> {
                                tvImprovement.text = "+$pct%"
                                tvImprovement.setTextColor(Color.parseColor("#4CAF50"))
                                tvImprovementLabel.text = "Learning Improvement"
                            }
                            pct < 0 -> {
                                tvImprovement.text = "$pct%"
                                tvImprovement.setTextColor(Color.parseColor("#F44336"))
                                tvImprovementLabel.text = "Score Change"
                            }
                            else -> {
                                tvImprovement.text = "="
                                tvImprovement.setTextColor(Color.parseColor("#888888"))
                                tvImprovementLabel.text = "Same Score"
                            }
                        }
                    } else {
                        tvImprovement.text = "$postScore / 5"
                        tvImprovementLabel.text = "Post Quiz Score"
                    }

                    showLoading(false)
                    loadLeaderboard()
                }
            }
            .addOnFailureListener {
                showLoading(false)
                loadLeaderboard()
            }
    }

    // ── Read game times from our leaderboard collection ──
    // NEVER reads from users collection
    private fun loadGameScores(uid: String, callback: (Double?, Double?) -> Unit) {
        db.collection("leaderboard")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { docs ->
                var game1Time: Double? = null
                var game2Time: Double? = null
                for (doc in docs) {
                    val gNum = doc.getLong("gameNumber")?.toInt() ?: 1
                    val t    = doc.getDouble("totalTime")
                    if (gNum == 1) game1Time = t
                    if (gNum == 2) game2Time = t
                }
                callback(game1Time, game2Time)
            }
            .addOnFailureListener { callback(null, null) }
    }

    // ── Beautiful leaderboard ───────────────────────────
    // Reads ONLY from leaderboard collection (our data)
    private fun loadLeaderboard() {
        db.collection("leaderboard")
            .whereEqualTo("gameNumber", 1)
            .orderBy("totalTime", Query.Direction.ASCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                layoutLeaderboard.removeAllViews()

                if (result.isEmpty) {
                    addEmptyState()
                    return@addOnSuccessListener
                }

                result.forEachIndexed { index, doc ->
                    val entryUid  = doc.getString("uid")      ?: ""
                    val username  = doc.getString("username") ?: "Player"
                    val totalTime = doc.getDouble("totalTime") ?: 0.0
                    val score     = doc.getLong("gameScore")?.toInt() ?: 0
                    val wrong     = doc.getLong("wrongAttempts")?.toInt() ?: 0
                    val isMe      = (entryUid == currentUid)

                    val row = buildLeaderboardRow(
                        rank      = index + 1,
                        username  = username,
                        totalTime = totalTime,
                        score     = score,
                        wrong     = wrong,
                        isMe      = isMe
                    )
                    layoutLeaderboard.addView(row)
                }
            }
            .addOnFailureListener {
                addEmptyState()
            }
    }

    // ── Build one leaderboard row ────────────────────────
    private fun buildLeaderboardRow(
        rank: Int, username: String, totalTime: Double,
        score: Int, wrong: Int, isMe: Boolean
    ): CardView {

        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(8)) }
            radius      = dpToPx(12).toFloat()
            cardElevation = if (isMe) dpToPx(6).toFloat() else dpToPx(2).toFloat()
            setCardBackgroundColor(Color.parseColor(
                when {
                    isMe       -> "#1A3A2A"   // Green tint — current user
                    rank == 1  -> "#2A2200"   // Gold tint — 1st place
                    rank == 2  -> "#1A1E22"   // Silver tint
                    rank == 3  -> "#1E1510"   // Bronze tint
                    else       -> "#1A1A1A"   // Default dark
                }
            ))
        }

        // Border for current user
        if (isMe) {
            card.foreground = null
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            gravity = Gravity.CENTER_VERTICAL
        }

        // ── Rank badge ───────────────────────────────────
        val tvRank = TextView(this).apply {
            text = when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "#$rank"
            }
            textSize  = if (rank <= 3) 22f else 16f
            setTextColor(Color.parseColor(
                when (rank) {
                    1 -> "#FFD700"
                    2 -> "#C0C0C0"
                    3 -> "#CD7F32"
                    else -> "#888888"
                }
            ))
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), ViewGroup.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER
        }

        // ── Player name ──────────────────────────────────
        val nameLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(dpToPx(8), 0, 0, 0)
        }

        val tvName = TextView(this).apply {
            text     = if (isMe) "$username  ← YOU" else username
            textSize = 15f
            setTextColor(if (isMe) Color.parseColor("#4CAF50") else Color.WHITE)
            if (isMe || rank == 1) setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val tvDetails = TextView(this).apply {
            text     = "Score: $score/6  •  Wrong: $wrong"
            textSize = 11f
            setTextColor(Color.parseColor("#888888"))
        }

        nameLayout.addView(tvName)
        nameLayout.addView(tvDetails)

        // ── Time ─────────────────────────────────────────
        val tvTime = TextView(this).apply {
            text     = formatTime(totalTime)
            textSize = 16f
            setTextColor(Color.parseColor(
                when {
                    rank == 1  -> "#FFD700"
                    isMe       -> "#4CAF50"
                    else       -> "#AAAAAA"
                }
            ))
            if (rank == 1) setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        row.addView(tvRank)
        row.addView(nameLayout)
        row.addView(tvTime)
        card.addView(row)
        return card
    }

    private fun addEmptyState() {
        val tv = TextView(this).apply {
            text      = "No scores yet — be the first!"
            textSize  = 14f
            setTextColor(Color.parseColor("#888888"))
            gravity   = Gravity.CENTER
            setPadding(0, dpToPx(24), 0, dpToPx(24))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        layoutLeaderboard.addView(tv)
    }

    private fun formatTime(seconds: Double): String {
        val mins = seconds.toInt() / 60
        val secs = seconds.toInt() % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun showLoading(loading: Boolean) {
        layoutLoading.visibility = if (loading) View.VISIBLE else View.GONE
        layoutResults.visibility = if (loading) View.GONE   else View.VISIBLE
    }
}
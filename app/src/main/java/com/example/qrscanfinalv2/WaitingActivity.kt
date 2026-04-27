package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class WaitingActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    private lateinit var tvStatus        : TextView
    private lateinit var tvSubStatus     : TextView
    private lateinit var tvPreScore      : TextView
    private lateinit var tvGameDetected  : TextView
    private lateinit var tvGameScore     : TextView
    private lateinit var tvGameTime      : TextView
    private lateinit var layoutGameDone  : LinearLayout
    private lateinit var progressBar     : ProgressBar
    private lateinit var btnContinue     : Button

    // Firestore real-time listener — must be removed when activity stops
    private var gameListener: ListenerRegistration? = null
    private var gameDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        tvStatus       = findViewById(R.id.tvStatus)
        tvSubStatus    = findViewById(R.id.tvSubStatus)
        tvPreScore     = findViewById(R.id.tvPreScore)
        tvGameDetected = findViewById(R.id.tvGameDetected)
        tvGameScore    = findViewById(R.id.tvGameScore)
        tvGameTime     = findViewById(R.id.tvGameTime)
        layoutGameDone = findViewById(R.id.layoutGameDone)
        progressBar    = findViewById(R.id.progressBar)
        btnContinue    = findViewById(R.id.btnContinue)

        // Show pre quiz score passed from PreQuizActivity
        val preScore = intent.getIntExtra("preQuizScore", 0)
        tvPreScore.text = "Your Pre-Quiz Score: $preScore / 5"

        // Hide game done panel initially
        layoutGameDone.visibility = View.GONE
        btnContinue.visibility    = View.GONE

        // Start watching Firebase for game completion
        startListeningForGame()

        // Manual continue button (fallback — hidden until game detected)
        btnContinue.setOnClickListener {
            goToPostQuiz()
        }
    }

    private fun startListeningForGame() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text          = "Walk to the Projection Station"
        tvSubStatus.text       = "Waiting for your game to complete..."
        progressBar.visibility = View.VISIBLE

        // ── Real-time Firestore listener ──────────────────
        // Watches leaderboard collection for a document with
        // matching uid + gameNumber=1
        // Python game writes here on completion → auto navigate
        gameListener = db.collection("leaderboard")
            .whereEqualTo("uid",        uid)
            .whereEqualTo("gameNumber", 1)
            .addSnapshotListener { snapshots, error ->

                if (error != null || snapshots == null) return@addSnapshotListener
                if (gameDetected) return@addSnapshotListener  // prevent double trigger
                if (snapshots.isEmpty)                        return@addSnapshotListener

                // ── Game score found in leaderboard! ─────
                val doc       = snapshots.documents[0]
                val gameScore = doc.getLong("gameScore")?.toInt() ?: 0
                val gameTime  = doc.getDouble("totalTime")        ?: 0.0

                gameDetected           = true
                progressBar.visibility = View.GONE

                // Format time as MM:SS
                val mins    = gameTime.toInt() / 60
                val secs    = gameTime.toInt() % 60
                val timeStr = String.format("%02d:%02d", mins, secs)

                // Show game result panel
                tvStatus.text       = "Game Complete!"
                tvSubStatus.text    = "Your score has been recorded."
                tvGameScore.text    = "Game Score: $gameScore / 6"
                tvGameTime.text     = "Time: $timeStr"
                tvGameDetected.text = "Great job! Moving to post-quiz..."

                layoutGameDone.visibility = View.VISIBLE
                btnContinue.visibility    = View.VISIBLE

                // ── Auto navigate after 3 seconds ────────
                btnContinue.postDelayed({
                    if (!isFinishing) goToPostQuiz()
                }, 3000)
            }
    }

    private fun goToPostQuiz() {
        startActivity(Intent(this, ThuparamayaFactsActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Always remove listener to prevent memory leaks
        gameListener?.remove()
    }
}
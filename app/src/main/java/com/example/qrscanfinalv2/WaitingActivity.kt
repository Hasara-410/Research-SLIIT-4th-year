package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WaitingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        val step = intent.getIntExtra("step", 1)
        val preQuizScore = intent.getIntExtra("preQuizScore", 0)

        val tvTitle = findViewById<TextView>(R.id.tvWaitingTitle)
        val tvMessage = findViewById<TextView>(R.id.tvWaitingMessage)
        val tvScore = findViewById<TextView>(R.id.tvScoreInfo)
        val btnDone = findViewById<Button>(R.id.btnDone)

        when (step) {
            1 -> {
                tvTitle.text = "Pre Quiz Complete! 🎉"
                tvMessage.text = "Great job! Now please go to the\nProjection Station to begin\nyour heritage experience."
                tvScore.text = "Your Pre Quiz Score: $preQuizScore / 5"
                btnDone.text = "I'm at the Projection Station"
                btnDone.setOnClickListener {
                    // After projection + games, come back for post quiz
                    // For now navigate to PostQuizActivity
                    // In real use, operator will trigger this
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
            }
            else -> {
                tvTitle.text = "Well Done! 🎉"
                tvMessage.text = "Please proceed to the next station."
                tvScore.text = ""
                btnDone.text = "Continue"
                btnDone.setOnClickListener { finish() }
            }
        }
    }
}
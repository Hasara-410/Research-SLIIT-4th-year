package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PreQuizResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_quiz_result)

        val score     = intent.getIntExtra("preQuizScore", 0)
        val tvScore   = findViewById<TextView>(R.id.tvPreQuizScore)
        val tvMessage = findViewById<TextView>(R.id.tvScoreMessage)
        val btnGo     = findViewById<Button>(R.id.btnGoToCenter)

        // Show score
        tvScore.text = "$score / 5"

        // Dynamic message based on score
        tvMessage.text = when {
            score == 5 -> "Excellent! Perfect score!"
            score >= 3 -> "Good job! Keep it up!"
            else       -> "Don't worry, you'll learn more!"
        }

        // Navigate to WaitingActivity when user is at projection center
        btnGo.setOnClickListener {
            val intent = Intent(this, WaitingActivity::class.java)
            intent.putExtra("preQuizScore", score)
            startActivity(intent)
            finish()
        }
    }
}
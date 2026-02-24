package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Screen01Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen01)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnNext = findViewById<Button>(R.id.btnNext)

        // Screen 01: Back should just do nothing / close
        btnBack.setOnClickListener {
            finish()
        }

        // Go to Screen 02
        btnNext.setOnClickListener {
            startActivity(Intent(this, Screen02Activity::class.java))
        }
    }
}
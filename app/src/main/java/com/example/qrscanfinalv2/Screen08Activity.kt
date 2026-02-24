package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Screen08Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen08)

        // Back button
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Next button
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            startActivity(Intent(this, Screen09Activity::class.java))
        }

        // ✅ Live Location Map button (your component)
        findViewById<Button>(R.id.btnLiveLocationMap).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
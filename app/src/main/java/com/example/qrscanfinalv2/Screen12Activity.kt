package com.example.qrscanfinalv2

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Screen12Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen12)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        // Last screen: Next can close app or go to Screen08/menu
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            finish()
        }
    }
}
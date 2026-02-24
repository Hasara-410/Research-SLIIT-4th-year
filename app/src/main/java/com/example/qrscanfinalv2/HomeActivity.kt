package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // ✅ Your feature cards are LinearLayouts now (Views), not MaterialButtons
        val btnMr = findViewById<View>(R.id.btnMR)              // NOTE: btnMR (capital R) in XML
        val btnMap = findViewById<View>(R.id.btnMap)
        val btnInscription = findViewById<View>(R.id.btnInscription)
        val btnGame = findViewById<View>(R.id.btnGame)

        btnMr.setOnClickListener {
            startActivity(Intent(this, MrReconstructionActivity::class.java))
        }

        btnMap.setOnClickListener {
            // ✅ open your map component
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnInscription.setOnClickListener {
            startActivity(Intent(this, InscriptionOcrActivity::class.java))
        }

        btnGame.setOnClickListener {
            startActivity(Intent(this, ArtifactInfoActivity::class.java))
        }

        // Bell click (optional)
        val imgBell = findViewById<ImageView>(R.id.imgBell)
        imgBell.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        // ✅ Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
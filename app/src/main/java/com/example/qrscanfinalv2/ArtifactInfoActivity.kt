package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ArtifactInfoActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artifact_info)

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnStartJourney = findViewById<Button>(R.id.btnStartJourney)

        // Get real name from Firestore (same pattern as ProfileActivity)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("users").document(uid)
            .addSnapshotListener { snap, e ->
                if (e != null) return@addSnapshotListener
                if (snap == null || !snap.exists()) return@addSnapshotListener
                val name = snap.getString("name").orEmpty()
                tvWelcome.text = if (name.isBlank()) "Welcome, Explorer!" else "Welcome, $name!"
            }

        // Start Journey → Pre Quiz
        btnStartJourney.setOnClickListener {
            startActivity(Intent(this, PreQuizActivity::class.java))
        }
    }
}
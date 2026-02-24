package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class HomeHostActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun logAppUsage() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // (A) total app opens
        val statsRef = db.collection("app_usage")
            .document("stats")
            .collection("main")
            .document("counter")

        statsRef.set(
            mapOf("openCount" to FieldValue.increment(1)),
            SetOptions.merge()
        )

        // (B) unique app users
        val userRef = db.collection("app_usage")
            .document("users")
            .collection("all")
            .document(uid)

        userRef.set(
            mapOf(
                "uid" to uid,
                "firstSeen" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_host)

        logAppUsage()

        // For now: show splash for 1.5s then go to next screen (we will build exact flow next)
        Handler(Looper.getMainLooper()).postDelayed({
            // NEXT STEP: we will replace this with your real "Splash 1" onboarding screen
            // For now just open SignIn screen OR Home screen later
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }, 2000)
    }
}
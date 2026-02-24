package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
class SignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()


        db = FirebaseFirestore.getInstance()

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)

        val tvGoSignUp = findViewById<TextView>(R.id.tvGoSignUp)
        val tvForgot = findViewById<TextView>(R.id.tvForgot)

        // ✅ Navigate to Sign Up
        tvGoSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // ✅ Optional: forgot password
        tvForgot.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            if (email.isBlank()) {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "Reset failed", Toast.LENGTH_SHORT).show()
                }
        }

        // ✅ Sign In
        btnSignIn.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass = etPassword.text?.toString()?.trim().orEmpty()

            if (email.isBlank() || pass.isBlank()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignIn.isEnabled = false

            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    logAppUsage()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    btnSignIn.isEnabled = true
                    Toast.makeText(this, e.message ?: "Sign in failed", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun logAppUsage() {
        val uid = auth.currentUser?.uid ?: return

        val statsRef = db.collection("app_usage")
            .document("stats")
            .collection("stats")
            .document("main")

        statsRef.set(
            mapOf("openCount" to FieldValue.increment(1)),
            SetOptions.merge()
        )

        val userRef = db.collection("app_usage")
            .document("stats")
            .collection("users")
            .document(uid)

        userRef.set(
            mapOf(
                "uid" to uid,
                "lastLogin" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
    }
}
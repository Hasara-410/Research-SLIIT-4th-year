package com.example.qrscanfinalv2

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class ResetEmailSentActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_email_sent)

        auth = FirebaseAuth.getInstance()
        email = intent.getStringExtra("email").orEmpty()

        val tvMsg = findViewById<TextView>(R.id.tvSentMsg)
        val btnVerify = findViewById<MaterialButton>(R.id.btnVerify)
        val tvResend = findViewById<TextView>(R.id.tvResend)

        tvMsg.text = "We just sent a password reset link to:\n$email"

        // "Verify" just means user confirms they will open email
        btnVerify.setOnClickListener {
            Toast.makeText(this, "Open your email and reset the password.", Toast.LENGTH_LONG).show()
            finish() // or go back to SignIn
        }

        tvResend.setOnClickListener {
            if (email.isBlank()) return@setOnClickListener
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Reset email resent", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "Resend failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
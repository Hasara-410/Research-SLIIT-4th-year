package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val btnSend = findViewById<MaterialButton>(R.id.btnSendCode)

        btnSend.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            if (email.isBlank()) {
                Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    // Go to "Verify Code" UI, but it's actually "Email Sent" screen
                    val i = Intent(this, ResetEmailSentActivity::class.java)
                    i.putExtra("email", email)
                    startActivity(i)
                    finish()
                }
                .addOnFailureListener { e ->
                    btnSend.isEnabled = true
                    Toast.makeText(this, e.message ?: "Failed", Toast.LENGTH_LONG).show()
                }
        }
    }
}
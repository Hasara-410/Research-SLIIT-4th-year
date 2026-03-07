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
import com.google.firebase.firestore.FieldValue

class SignUpActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirm: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnSignUp: MaterialButton
    private lateinit var tvGoSignIn: TextView

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirm = findViewById(R.id.etConfirmPassword)
        etPhone = findViewById(R.id.etPhone)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvGoSignIn = findViewById(R.id.tvGoSignIn)

        tvGoSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        btnSignUp.setOnClickListener {

            val name = etName.text?.toString()?.trim().orEmpty()
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass = etPassword.text?.toString()?.trim().orEmpty()
            val confirm = etConfirm.text?.toString()?.trim().orEmpty()
            val phone = etPhone.text?.toString()?.trim().orEmpty()

            if (name.isBlank() || email.isBlank() || pass.isBlank() || confirm.isBlank() || phone.isBlank()) {
                toast("Please fill all fields")
                return@setOnClickListener
            }
            if (pass.length < 6) {
                toast("Password must be at least 6 characters")
                return@setOnClickListener
            }
            if (pass != confirm) {
                toast("Passwords do not match")
                return@setOnClickListener
            }

            btnSignUp.isEnabled = false


            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { res ->


                    val uid = res.user?.uid ?: run {
                        btnSignUp.isEnabled = true

                        toast("Signup failed (no UID)")
                        return@addOnSuccessListener
                    }

                    val userDoc = db.collection("users").document(uid)
                    val data = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    userDoc.set(data)
                        .addOnSuccessListener {
                            toast("Profile saved")
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            btnSignUp.isEnabled = true

                            toast("Profile save failed: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    btnSignUp.isEnabled = true

                    toast("Signup failed: ${e.message}")
                }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
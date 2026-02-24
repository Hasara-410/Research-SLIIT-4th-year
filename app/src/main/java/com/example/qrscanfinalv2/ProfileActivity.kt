package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val img = findViewById<ImageView>(R.id.imgProfile)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvPhoneTop = findViewById<TextView>(R.id.tvPhone)

        val etNameView = findViewById<TextInputEditText>(R.id.etNameView)
        val etPhoneView = findViewById<TextInputEditText>(R.id.etPhoneView)
        val etAgeView = findViewById<TextInputEditText>(R.id.etAgeView)

        val btnEdit = findViewById<MaterialButton>(R.id.btnEditProfile)

        btnEdit.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

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
                val phone = snap.getString("phone").orEmpty()
                val age = snap.getLong("age")?.toString().orEmpty()
                val photoUrl = snap.getString("photoUrl").orEmpty()

                tvName.text = if (name.isBlank()) "Your Name" else name
                tvPhoneTop.text = if (phone.isBlank()) "" else phone

                etNameView.setText(name)
                etPhoneView.setText(phone)
                etAgeView.setText(age)

                if (photoUrl.isNotBlank()) {
                    Glide.with(this).load(photoUrl).circleCrop().into(img)
                } else {
                    Glide.with(this).load(R.drawable.ic_profile_placeholder).circleCrop().into(img)
                }
            }
    }
}
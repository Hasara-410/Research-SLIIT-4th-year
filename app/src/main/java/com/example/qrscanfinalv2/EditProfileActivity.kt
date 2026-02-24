package com.example.qrscanfinalv2

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditProfileActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            val img = findViewById<ImageView>(R.id.imgProfileEdit)
            Glide.with(this).load(uri).circleCrop().into(img)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val img = findViewById<ImageView>(R.id.imgProfileEdit)
        val btnChangePhoto = findViewById<ImageView>(R.id.btnChangePhoto)

        val tvNameTop = findViewById<TextView>(R.id.tvNameEditTop)
        val tvPhoneTop = findViewById<TextView>(R.id.tvPhoneEditTop)

        val etName = findViewById<TextInputEditText>(R.id.etNameEdit)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhoneEdit)
        val etAge = findViewById<TextInputEditText>(R.id.etAgeEdit)

        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = findViewById<MaterialButton>(R.id.btnCancel)

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load existing user data
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val name = snap.getString("name").orEmpty()
                val phone = snap.getString("phone").orEmpty()
                val age = snap.getLong("age")?.toString().orEmpty()
                val photoUrl = snap.getString("photoUrl").orEmpty()

                tvNameTop.text = if (name.isBlank()) "Your Name" else name
                tvPhoneTop.text = if (phone.isBlank()) "" else phone

                etName.setText(name)
                etPhone.setText(phone)
                etAge.setText(age)

                if (photoUrl.isNotBlank()) {
                    Glide.with(this).load(photoUrl).circleCrop().into(img)
                } else {
                    Glide.with(this).load(R.drawable.ic_profile_placeholder).circleCrop().into(img)
                }
            }

        btnChangePhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        btnCancel.setOnClickListener {
            finish() // go back to Profile
        }

        btnSave.setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty()
            val phone = etPhone.text?.toString()?.trim().orEmpty()
            val ageStr = etAge.text?.toString()?.trim().orEmpty()

            val ageVal = ageStr.toLongOrNull()
            if (name.isBlank() || phone.isBlank() || ageVal == null) {
                Toast.makeText(this, "Fill Name, Phone, Age correctly", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // NOTE: If you want real photo saving -> upload to Firebase Storage, then store URL here.
            // For now we keep old photoUrl (or empty)
            val data = hashMapOf<String, Any>(
                "name" to name,
                "phone" to phone,
                "age" to ageVal
            )

            db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "Save failed", Toast.LENGTH_LONG).show()
                }
        }
    }
}
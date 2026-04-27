package com.example.qrscanfinalv2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class PostQuizActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val selectedQuestions = mutableListOf<Map<String, Any>>()
    private var currentIndex = 0
    private var score = 0

    // UI
    private lateinit var tvQuizTitle: TextView
    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvQuestion: TextView
    private lateinit var rgOptions: RadioGroup
    private lateinit var rbA: RadioButton
    private lateinit var rbB: RadioButton
    private lateinit var rbC: RadioButton
    private lateinit var rbD: RadioButton
    private lateinit var btnNext: Button
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutQuiz: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_quiz)

        tvQuizTitle   = findViewById(R.id.tvQuizTitle)
        tvProgress    = findViewById(R.id.tvProgress)
        progressBar   = findViewById(R.id.progressBar)
        tvQuestion    = findViewById(R.id.tvQuestion)
        rgOptions     = findViewById(R.id.rgOptions)
        rbA           = findViewById(R.id.rbA)
        rbB           = findViewById(R.id.rbB)
        rbC           = findViewById(R.id.rbC)
        rbD           = findViewById(R.id.rbD)
        btnNext       = findViewById(R.id.btnNext)
        layoutLoading = findViewById(R.id.layoutLoading)
        layoutQuiz    = findViewById(R.id.layoutQuiz)

        tvQuizTitle.text = "Post Quiz"
        showLoading(true)

        // Load SAME questions as pre quiz (shuffled options for validity)
        loadSameQuestionsAsPreQuiz()

        btnNext.setOnClickListener { handleNext() }
    }

    private fun loadSameQuestionsAsPreQuiz() {
        val uid = auth.currentUser?.uid ?: return

        // Step 1 — Get the same question IDs that were used in pre quiz
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { userDoc ->

                @Suppress("UNCHECKED_CAST")
                val preQuizIds = userDoc.get("preQuizAskedIds") as? List<String> ?: emptyList()

                if (preQuizIds.isEmpty()) {
                    Toast.makeText(this, "Pre quiz data not found. Please complete pre quiz first.", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Step 2 — Load all questions from Firebase
                db.collection("questions")
                    .get()
                    .addOnSuccessListener { result ->
                        selectedQuestions.clear()

                        for (doc in result) {
                            val qData = doc.data
                            val id = qData["id"] as? String ?: ""

                            // Only include questions that were in pre quiz
                            if (preQuizIds.contains(id)) {

                                // Shuffle the OPTIONS order so user can't memorize position
                                val q = qData.toMutableMap()
                                val originalOptions = (q["options"] as? List<*>)?.map { it.toString() } ?: emptyList()
                                val shuffledOptions = originalOptions.toMutableList().also { it.shuffle() }
                                q["options"] = shuffledOptions
                                selectedQuestions.add(q)
                            }
                        }

                        if (selectedQuestions.isEmpty()) {
                            Toast.makeText(this, "Could not load quiz questions.", Toast.LENGTH_LONG).show()
                            finish()
                            return@addOnSuccessListener
                        }

                        // Shuffle question ORDER as well
                        selectedQuestions.shuffle()

                        showLoading(false)
                        showQuestion(0)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to load questions: ${e.message}", Toast.LENGTH_LONG).show()
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load user data: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun showQuestion(index: Int) {
        currentIndex = index
        val question = selectedQuestions[index]

        val questionText = question["question"] as? String ?: ""
        val options = question["options"] as? List<*> ?: emptyList<String>()

        // Update progress
        tvProgress.text = "Question ${index + 1} of 5"
        progressBar.progress = ((index + 1) * 100) / 5

        tvQuestion.text = questionText

        rgOptions.clearCheck()
        rbA.text = options.getOrNull(0)?.toString() ?: ""
        rbB.text = options.getOrNull(1)?.toString() ?: ""
        rbC.text = options.getOrNull(2)?.toString() ?: ""
        rbD.text = options.getOrNull(3)?.toString() ?: ""

        // Hide C and D for true/false questions
        if (options.size <= 2) {
            rbC.visibility = View.GONE
            rbD.visibility = View.GONE
        } else {
            rbC.visibility = View.VISIBLE
            rbD.visibility = View.VISIBLE
        }

        btnNext.text = if (index == selectedQuestions.size - 1) "Submit Quiz" else "Next"
    }

    private fun handleNext() {
        val selectedId = rgOptions.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRadio = findViewById<RadioButton>(selectedId)
        val selectedAnswer = selectedRadio.text.toString()

        // Check answer
        val correctAnswer = selectedQuestions[currentIndex]["correctAnswer"] as? String ?: ""
        if (selectedAnswer == correctAnswer) score++

        if (currentIndex < selectedQuestions.size - 1) {
            showQuestion(currentIndex + 1)
        } else {
            saveScoreAndProceed()
        }
    }

    private fun saveScoreAndProceed() {
        val uid = auth.currentUser?.uid ?: return
        showLoading(true)

        // Use set + merge so it works even if document fields are missing
        db.collection("users").document(uid)
            .set(
                mapOf(
                    "postQuizScore" to score,
                    "postQuizTotal" to selectedQuestions.size,
                    "currentStep"   to 7,
                    "updatedAt"     to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                showLoading(false)
                val intent = Intent(this, ResultsActivity::class.java)
                intent.putExtra("postQuizScore", score)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to save score: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showLoading(loading: Boolean) {
        layoutLoading.visibility = if (loading) View.VISIBLE else View.GONE
        layoutQuiz.visibility    = if (loading) View.GONE   else View.VISIBLE
    }
}
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

class PreQuizActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val allQuestions = mutableListOf<Map<String, Any>>()
    private val selectedQuestions = mutableListOf<Map<String, Any>>()
    private val userAnswers = mutableListOf<String>()
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
        setContentView(R.layout.activity_pre_quiz)

        tvQuizTitle    = findViewById(R.id.tvQuizTitle)
        tvProgress     = findViewById(R.id.tvProgress)
        progressBar    = findViewById(R.id.progressBar)
        tvQuestion     = findViewById(R.id.tvQuestion)
        rgOptions      = findViewById(R.id.rgOptions)
        rbA            = findViewById(R.id.rbA)
        rbB            = findViewById(R.id.rbB)
        rbC            = findViewById(R.id.rbC)
        rbD            = findViewById(R.id.rbD)
        btnNext        = findViewById(R.id.btnNext)
        layoutLoading  = findViewById(R.id.layoutLoading)
        layoutQuiz     = findViewById(R.id.layoutQuiz)

        tvQuizTitle.text = "Pre Quiz"
        showLoading(true)
        loadQuestions()

        btnNext.setOnClickListener {
            handleNext()
        }
    }

    private fun loadQuestions() {
        db.collection("questions")
            .get()
            .addOnSuccessListener { result ->
                allQuestions.clear()
                for (doc in result) {
                    allQuestions.add(doc.data)
                }

                if (allQuestions.size < 5) {
                    Toast.makeText(this, "Not enough questions in database", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Pick 5 random questions
                selectedQuestions.clear()
                selectedQuestions.addAll(allQuestions.shuffled().take(5))

                showLoading(false)
                showQuestion(0)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load questions: ${e.message}", Toast.LENGTH_LONG).show()
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

        // Set question text
        tvQuestion.text = questionText

        // Set options
        rgOptions.clearCheck()
        rbA.text = options.getOrNull(0)?.toString() ?: ""
        rbB.text = options.getOrNull(1)?.toString() ?: ""
        rbC.text = options.getOrNull(2)?.toString() ?: ""
        rbD.text = options.getOrNull(3)?.toString() ?: ""

        // Hide D if true/false question
        if (options.size <= 2) {
            rbC.visibility = View.GONE
            rbD.visibility = View.GONE
        } else {
            rbC.visibility = View.VISIBLE
            rbD.visibility = View.VISIBLE
        }

        btnNext.text = if (index == 4) "Submit Quiz" else "Next"
    }

    private fun handleNext() {
        val selectedId = rgOptions.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRadio = findViewById<RadioButton>(selectedId)
        val selectedAnswer = selectedRadio.text.toString()
        userAnswers.add(selectedAnswer)

        // Check answer
        val correctAnswer = selectedQuestions[currentIndex]["correctAnswer"] as? String ?: ""
        if (selectedAnswer == correctAnswer) score++

        if (currentIndex < 4) {
            showQuestion(currentIndex + 1)
        } else {
            saveScoreAndProceed()
        }
    }

    private fun saveScoreAndProceed() {
        val uid = auth.currentUser?.uid ?: return
        showLoading(true)

        // Save asked question IDs to avoid repeating in post quiz
        val askedIds = selectedQuestions.map { it["id"] as? String ?: "" }

        db.collection("users").document(uid)
            .update(
                mapOf(
                    "preQuizScore" to score,
                    "preQuizTotal" to 5,
                    "preQuizAskedIds" to askedIds,
                    "currentStep" to 1,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                showLoading(false)
                val intent = Intent(this, PreQuizResultActivity::class.java)
                intent.putExtra("step", 1)
                intent.putExtra("preQuizScore", score)
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
        layoutQuiz.visibility = if (loading) View.GONE else View.VISIBLE
    }
}
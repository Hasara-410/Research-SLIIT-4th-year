package com.example.qrscanfinalv2

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.apply
import kotlin.collections.isNullOrEmpty
import kotlin.isInitialized
import kotlin.text.isEmpty
import kotlin.text.trim

class AskQuestionActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var etQuestion: EditText
    private lateinit var btnAsk: Button
    private lateinit var tvAnswer: TextView

    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private lateinit var audioManager: AudioManager

    private val heritageAI = HeritageAIEngine

    private var zoneName: String? = null
    private var placeName: String? = null

    // ✅ Voice launcher (modern replacement for onActivityResult)
    private val voiceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val list = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!list.isNullOrEmpty()) {
                    etQuestion.setText(list[0])
                    etQuestion.setSelection(etQuestion.text.length)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ask_question)

        tvTitle = findViewById(R.id.tvTitle)
        etQuestion = findViewById(R.id.etQuestion)
        btnAsk = findViewById(R.id.btnAsk)
        tvAnswer = findViewById(R.id.tvAnswer)

        // ✅ Touch mic icon inside EditText (drawableEnd)
        etQuestion.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2 // right
                val drawable = etQuestion.compoundDrawables[drawableEnd]
                if (drawable != null) {
                    if (event.rawX >= (etQuestion.right - drawable.bounds.width())) {
                        startVoiceInput()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        zoneName = intent.getStringExtra("zone")
        placeName = intent.getStringExtra("place")
        tvTitle.text = "Ask about $placeName ($zoneName)"

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts.setLanguage(Locale.US)
                ttsReady =
                    res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            } else {
                ttsReady = false
            }
        }

        btnAsk.setOnClickListener {
            val question = etQuestion.text.toString().trim()
            if (question.isEmpty()) {
                Toast.makeText(this, "Type a question first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val answer = heritageAI.answer(zoneName ?: "", placeName ?: "", question)
            tvAnswer.text = answer
            speak(answer)
        }
    }

    // ✅ starts voice input
    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your question...")
        }

        try {
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speak(text: String) {
        if (!ttsReady) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioManager.requestAudioFocus(focusRequest)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AI_TTS_${System.currentTimeMillis()}")
        }, 200)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}

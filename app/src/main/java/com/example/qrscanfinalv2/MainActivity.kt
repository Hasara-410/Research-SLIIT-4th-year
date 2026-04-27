// MainActivity.kt  (QR_Scan project)
// ✅ GPS + UI + QR + TTS
// ✅ Multi-user progress (Firestore): users/{uid}/progress_zones + users/{uid}/progress_places
package com.example.qrscanfinalv2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import java.util.Locale
import com.google.firebase.firestore.FieldValue

// -------------------- DATA CLASSES --------------------
data class Location(
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Double = 0.0,
    val visited: Boolean = false
)

data class Place(
    val placeName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Double = 0.0,
    val visited: Boolean = false
)

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnScanQr: Button
    private lateinit var googleMap: GoogleMap

    private lateinit var btnOpenAi: Button
    private lateinit var btnProgress: ImageButton

    private val handler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null

    private val circles = mutableMapOf<String, Circle>()
    private var isMapReady = false

    // Live data for drawing
    private var liveZones: List<Pair<String, Location>> = emptyList()
    private var livePlacesMap: MutableMap<String, List<Pair<String, Place>>> = mutableMapOf()

    // TTS
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private lateinit var audioManager: AudioManager

    // GPS range (ONLY detecting current zone/place)
    private var inRangeZoneName: String? = null
    private var inRangePlaceName: String? = null

    private var zoneEnterStartMs: Long? = null
    private var placeEnterStartMs: Long? = null

    // Offline visited storage
    private val prefs by lazy { getSharedPreferences("offline_visited", MODE_PRIVATE) }
    private fun zoneKey(zoneId: String) = "ZONE:$zoneId"
    private fun placeKey(zoneId: String, placeId: String) = "PLACE:$zoneId|$placeId"

    private fun isZoneVisitedLocal(zoneId: String): Boolean =
        prefs.getBoolean(zoneKey(zoneId), false)

    private fun setZoneVisitedLocal(zoneId: String, value: Boolean) {
        prefs.edit().putBoolean(zoneKey(zoneId), value).apply()
    }

    private fun isPlaceVisitedLocal(zoneId: String, placeId: String): Boolean =
        prefs.getBoolean(placeKey(zoneId, placeId), false)

    private fun setPlaceVisitedLocal(zoneId: String, placeId: String, value: Boolean) {
        prefs.edit().putBoolean(placeKey(zoneId, placeId), value).apply()
    }

    // -------------------- YOUR ZONES/PLACES --------------------
    private val zones = listOf(
        //Location("SLIIT Malabe", 6.914677, 79.973206, 500.0, false),
        Location("Sigiriya", 7.956944, 80.759722, 1000.0, false),
        //Location("My Home", 7.5772812, 80.4509638, 500.0, false),
    )

    private val zonePlaceMap = mapOf(
//        "SLIIT Malabe" to listOf(
//            Place("SLIIT Main Building", 6.914678, 79.973208, 50.0, false),
//            Place("SLIIT New Building", 6.9155, 79.9739, 50.0, false)
//        ),
        "Sigiriya" to listOf(
            Place("Sigiriya Museum", 7.956970, 80.751570, 80.0, false),
            Place("Water Gardens", 7.956819, 80.755592, 80.0, false),
            Place("Boulder Gardens", 7.958560, 80.759230, 80.0, false),
            Place("Cobra Hood Cave", 7.955961, 80.758440, 40.0, false),
            Place("Mirror Wall", 7.956706, 80.759566, 40.0, false),
            Place("Lion Gate", 7.957565, 80.759900, 40.0, false),
            Place("Summit", 7.957201, 80.759068, 40.0, false)
        ),
//        "My Home" to listOf(
//            Place("Main Gate", 7.5772998, 80.4511324, 15.0, false),
//            Place("Gate2", 7.5771083, 80.4510794, 15.0, false)
//        ),
    )

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // ✅ QR Activity launcher (CameraX + MLKit)
    private val qrLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val qrText = result.data?.getStringExtra("qr") ?: return@registerForActivityResult
                handleQrResult(qrText)
            }
        }

    // -------------------- LIFECYCLE --------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts.setLanguage(Locale.US)
                tts.setSpeechRate(1.0f)
                ttsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
                if (!ttsReady) Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show()
            } else {
                ttsReady = false
                Toast.makeText(this, "TTS init failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Bind views
        locationTextView = findViewById(R.id.locationTextView)
        statusTextView = findViewById(R.id.statusTextView)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnScanQr = findViewById(R.id.btnScanQr)
        btnOpenAi = findViewById(R.id.btnOpenAi)
        btnProgress = findViewById(R.id.btnProgress)

        // Progress button
        btnProgress.setOnClickListener {
            val intent = Intent(this, ProgressActivity::class.java)
            intent.putExtra("zone", inRangeZoneName)
            startActivity(intent)
        }

        // AI button
        btnOpenAi.setOnClickListener {
            val zone = inRangeZoneName
            val place = inRangePlaceName

            if (zone == null || place == null) {
                Toast.makeText(this, "Go inside a place first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPlaceVisitedLocal(zone, place)) {
                Toast.makeText(this, "Unlock this place first (scan PLACE QR)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, AskQuestionActivity::class.java)
            intent.putExtra("zone", zone)
            intent.putExtra("place", place)
            startActivity(intent)
        }

        // Scan QR
        btnScanQr.isEnabled = false
        btnScanQr.setOnClickListener {
            if (inRangeZoneName == null) {
                Toast.makeText(this, "Go inside a zone first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 200)
                return@setOnClickListener
            }

            qrLauncher.launch(Intent(this, QRScanActivity::class.java))
        }

        // Refresh (reset)
        btnRefresh.setOnClickListener { resetAllVisited() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkLocationPermission()

        ensureUserSignedIn {
            // ✅ log map usage (opens + unique users)
            logMapUsage()

            seedFirestoreOnceIfNeeded()

            // if map already ready, sync now
            if (isMapReady) syncFromFirestore()
        }


    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true

        val sliitLocation = LatLng(6.914677, 79.973206)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sliitLocation, 16f))

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        }

        loadLocalZonesIntoLive()
        drawFromLiveData()

        ensureUserSignedIn {
            syncFromFirestore()
            startAutoRefresh()

            // ✅ ADD THIS LINE
            logMapUsage()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isMapReady) {
            drawFromLiveData()
            ensureUserSignedIn { syncFromFirestore() }
        }
    }

    override fun onStart() {
        super.onStart()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onStop() {
        super.onStop()
        refreshRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }

    // -------------------- PERMISSIONS --------------------
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 123)
        } else {
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            123 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED && isMapReady
                    ) {
                        googleMap.isMyLocationEnabled = true
                    }
                } else {
                    Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show()
                }
            }

            200 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    qrLauncher.launch(Intent(this, QRScanActivity::class.java))
                } else {
                    Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // -------------------- AUTO REFRESH --------------------
    private fun startAutoRefresh() {
        refreshRunnable = Runnable {
            ensureUserSignedIn { syncFromFirestore() }
            forceRecheckNow()
            handler.postDelayed(refreshRunnable!!, 10_000)
        }
        handler.post(refreshRunnable!!)
    }

    // -------------------- GPS --------------------
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        )
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(3f)
            .setWaitForAccurateLocation(true)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { loc ->
                Log.d("GPS_METRICS", "acc=${loc.accuracy}m lat=${loc.latitude} lng=${loc.longitude}")
                val lat = loc.latitude
                val lng = loc.longitude
                locationTextView.text = "Lat: $lat, Lng: $lng"

                val currentLatLng = LatLng(lat, lng)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))

                checkZoneAndPlaces(lat, lng, loc.accuracy)
            }
        }
    }

    // GPS ONLY: detect current zone/place
    private fun checkZoneAndPlaces(currentLat: Double, currentLng: Double, gpsAccuracy: Float) {
        if (liveZones.isEmpty()) {
            loadLocalZonesIntoLive()
            drawFromLiveData()
        }

        var currentZoneName: String? = null
        var currentPlaceName: String? = null

        for ((zoneId, zone) in liveZones) {
            val distZone = calculateDistance(currentLat, currentLng, zone.latitude, zone.longitude)
            val insideZone = distZone <= zone.radius

            if (insideZone) {
                currentZoneName = zone.locationName
                currentPlaceName = null

                val places = livePlacesMap[zoneId] ?: emptyList()
                for ((_, place) in places) {
                    val distPlace = calculateDistance(currentLat, currentLng, place.latitude, place.longitude)
                    if (distPlace <= place.radius) currentPlaceName = place.placeName
                }
            }
        }

        val prevZone = inRangeZoneName
        val prevPlace = inRangePlaceName

        inRangeZoneName = currentZoneName
        inRangePlaceName = currentPlaceName

        // -------------------- EVALUATION METRICS (AUTO) --------------------

// ZONE enter timing
        if (prevZone != currentZoneName) {
            // leaving zone or switching zone -> reset start time
            zoneEnterStartMs = if (currentZoneName != null) System.currentTimeMillis() else null
        } else {
            // still in same zone -> keep time
            if (currentZoneName != null && zoneEnterStartMs == null) zoneEnterStartMs = System.currentTimeMillis()
        }

// PLACE enter timing
        if (prevPlace != currentPlaceName) {
            placeEnterStartMs = if (currentPlaceName != null) System.currentTimeMillis() else null
        } else {
            if (currentPlaceName != null && placeEnterStartMs == null) placeEnterStartMs = System.currentTimeMillis()
        }

// Log zone event (only when we newly enter a zone)
        if (prevZone == null && currentZoneName != null) {
            val delaySec = ((System.currentTimeMillis() - (zoneEnterStartMs ?: System.currentTimeMillis())) / 1000.0)
            recordEvaluation("ZONE", currentZoneName, null, gpsAccuracy, delaySec)
        }

// Log place event (only when we newly enter a place)
        if (prevPlace == null && currentPlaceName != null && currentZoneName != null) {
            val delaySec = ((System.currentTimeMillis() - (placeEnterStartMs ?: System.currentTimeMillis())) / 1000.0)
            recordEvaluation("PLACE", currentZoneName, currentPlaceName, gpsAccuracy, delaySec)
        }

        // Store last zone for progress screen fallback
        if (currentZoneName != null) {
            getSharedPreferences("progress_zone", MODE_PRIVATE)
                .edit()
                .putString("last_zone", currentZoneName)
                .apply()
        }

        btnScanQr.isEnabled = (inRangeZoneName != null)
        updateBottomStatus(currentZoneName, currentPlaceName)
        updateAiAvailability()
    }

    // -------------------- QR RESULT --------------------
    private fun handleQrResult(qr: String) {
        val parts = qr.split("|").map { it.trim() }
        if (parts.isEmpty()) {
            Toast.makeText(this, "Invalid QR", Toast.LENGTH_SHORT).show()
            return
        }

        val type = parts[0].uppercase()

        when (type) {
            "ZONE" -> {
                if (parts.size < 2) {
                    Toast.makeText(this, "Invalid ZONE QR", Toast.LENGTH_SHORT).show()
                    return
                }
                val zoneNameFromQr = parts[1]

                if (inRangeZoneName == null) {
                    Toast.makeText(this, "Go inside a zone first", Toast.LENGTH_SHORT).show()
                    return
                }
                if (zoneNameFromQr != inRangeZoneName) {
                    Toast.makeText(this, "QR doesn't match your current zone", Toast.LENGTH_SHORT).show()
                    return
                }

                // local visited
                setZoneVisitedLocal(zoneNameFromQr, true)
                drawFromLiveData()

                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) {
                    Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show()
                    return
                }

                // Firestore (multi-user)
                db.collection("users")
                    .document(uid)
                    .collection("progress_zones")
                    .document(zoneNameFromQr)
                    .set(
                        mapOf(
                            "visited" to true,
                            "timestamp" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener { syncFromFirestore() }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Zone visited write failed", e)
                        Toast.makeText(this, "Firestore write failed (zone)", Toast.LENGTH_SHORT).show()
                    }

                speak("Zone unlocked. Welcome to $zoneNameFromQr zone.")
                Toast.makeText(this, "Zone unlocked!", Toast.LENGTH_SHORT).show()
            }

            "PLACE" -> {
                if (parts.size < 3) {
                    Toast.makeText(this, "Invalid PLACE QR", Toast.LENGTH_SHORT).show()
                    return
                }
                val zoneNameFromQr = parts[1]
                val placeNameFromQr = parts[2]

                if (inRangeZoneName == null || zoneNameFromQr != inRangeZoneName) {
                    Toast.makeText(this, "QR doesn't match your current zone", Toast.LENGTH_SHORT).show()
                    return
                }
                if (inRangePlaceName == null || placeNameFromQr != inRangePlaceName) {
                    Toast.makeText(this, "Go closer to the correct place marker", Toast.LENGTH_SHORT).show()
                    return
                }

                // local visited
                setPlaceVisitedLocal(zoneNameFromQr, placeNameFromQr, true)
                drawFromLiveData()
                updateAiAvailability()

                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) {
                    Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show()
                    return
                }

                // Firestore (multi-user)
                val placeDocId = "${zoneNameFromQr}__${placeNameFromQr}"

                db.collection("users")
                    .document(uid)
                    .collection("progress_places")
                    .document(placeDocId)
                    .set(
                        mapOf(
                            "zone" to zoneNameFromQr,
                            "place" to placeNameFromQr,
                            "visited" to true,
                            "timestamp" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener { syncFromFirestore() }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Place visited write failed", e)
                        Toast.makeText(this, "Firestore write failed (place)", Toast.LENGTH_SHORT).show()
                    }

                speak("Place unlocked. You are at $placeNameFromQr.")
                Toast.makeText(this, "Place unlocked!", Toast.LENGTH_SHORT).show()
            }

            else -> Toast.makeText(this, "Unknown QR format", Toast.LENGTH_SHORT).show()
        }
    }

    // -------------------- LIVE DATA BUILD (LOCAL GEOMETRY) --------------------
    private fun loadLocalZonesIntoLive() {
        liveZones = zones.map { it.locationName to it }
        livePlacesMap.clear()
        for ((zName, places) in zonePlaceMap) {
            livePlacesMap[zName] = places.map { it.placeName to it }
        }
    }

    // -------------------- SYNC FROM FIRESTORE (USER PROGRESS ONLY) --------------------
    private fun syncFromFirestore() {
        if (!isMapReady) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val zoneVisited = mutableMapOf<String, Boolean>()
        val placeVisited = mutableMapOf<String, MutableMap<String, Boolean>>() // zone -> (place -> visited)

        fun applyVisited() {
            // zones
            liveZones = zones.map { z ->
                val visitedNow = (zoneVisited[z.locationName] == true) || isZoneVisitedLocal(z.locationName)
                z.locationName to z.copy(visited = visitedNow)
            }

            // places
            livePlacesMap.clear()
            for ((zoneName, places) in zonePlaceMap) {
                val pv = placeVisited[zoneName] ?: mutableMapOf()
                livePlacesMap[zoneName] = places.map { p ->
                    val visitedNow = (pv[p.placeName] == true) || isPlaceVisitedLocal(zoneName, p.placeName)
                    p.placeName to p.copy(visited = visitedNow)
                }
            }

            drawFromLiveData()
        }

        // 1) fetch zone progress
        db.collection("users").document(uid).collection("progress_zones")
            .get(Source.SERVER)
            .addOnSuccessListener { zs ->
                for (doc in zs.documents) {
                    zoneVisited[doc.id] = (doc.getBoolean("visited") == true)
                }

                // 2) fetch place progress
                db.collection("users").document(uid).collection("progress_places")
                    .get(Source.SERVER)
                    .addOnSuccessListener { ps ->
                        for (doc in ps.documents) {
                            val z = doc.getString("zone") ?: continue
                            val p = doc.getString("place") ?: continue
                            val v = doc.getBoolean("visited") == true
                            if (!placeVisited.containsKey(z)) placeVisited[z] = mutableMapOf()
                            placeVisited[z]!![p] = v
                        }
                        applyVisited()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Fetch places failed", e)
                        applyVisited()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Fetch zones failed", e)
                applyVisited()
            }
    }

    // -------------------- DRAW UI --------------------
    private fun drawFromLiveData() {
        if (!isMapReady) return
        if (liveZones.isEmpty()) loadLocalZonesIntoLive()

        googleMap.clear()
        circles.clear()

        for ((zoneId, zone) in liveZones) {
            val zoneLocation = LatLng(zone.latitude, zone.longitude)

            val zoneVisitedNow = zone.visited || isZoneVisitedLocal(zoneId)
            val circleColor = if (zoneVisitedNow) 0x3000FF00 else 0x30FF0000

            val circle = googleMap.addCircle(
                CircleOptions()
                    .center(zoneLocation)
                    .radius(zone.radius)
                    .strokeColor(Color.BLACK)
                    .strokeWidth(4f)
                    .fillColor(circleColor)
            )
            circles[zoneId] = circle

            val places = livePlacesMap[zoneId] ?: emptyList()
            for ((placeId, place) in places) {
                val placeLocation = LatLng(place.latitude, place.longitude)

                val placeVisitedNow = place.visited || isPlaceVisitedLocal(zoneId, placeId)
                val markerColor =
                    if (placeVisitedNow) BitmapDescriptorFactory.HUE_GREEN
                    else BitmapDescriptorFactory.HUE_RED

                googleMap.addMarker(
                    MarkerOptions()
                        .position(placeLocation)
                        .title(place.placeName)
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                )
            }
        }
    }

    // -------------------- HELPERS --------------------
    private fun forceRecheckNow() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) checkZoneAndPlaces(loc.latitude, loc.longitude, loc.accuracy)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun updateBottomStatus(zoneName: String?, placeName: String?) {
        val statusText = when {
            placeName != null -> "Current Zone: $zoneName\nCurrent Place: $placeName"
            zoneName != null -> "Current Zone: $zoneName"
            else -> "Status: Not inside any zone"
        }
        statusTextView.text = statusText
    }

    // -------------------- TTS --------------------
    private fun speak(text: String) {
        if (!::tts.isInitialized || !ttsReady) {
            Toast.makeText(this, "TTS not ready yet", Toast.LENGTH_SHORT).show()
            return
        }

        if (::audioManager.isInitialized) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setOnAudioFocusChangeListener { }
                    .build()

                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "QR_TTS_${System.currentTimeMillis()}")
        }, 300)
    }

    // -------------------- RESET (LOCAL + USER FIRESTORE) --------------------
    private fun resetAllVisited() {
        // 1) clear local
        prefs.edit().clear().apply()

        // 2) UI red immediately
        loadLocalZonesIntoLive()
        drawFromLiveData()

        // 3) recheck status
        forceRecheckNow()

        // 4) clear user firestore progress
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Reset locally (not signed in)", Toast.LENGTH_SHORT).show()
            return
        }

        // delete all docs in both collections
        db.collection("users").document(uid).collection("progress_zones")
            .get(Source.SERVER)
            .addOnSuccessListener { snap ->
                for (doc in snap.documents) doc.reference.delete()
                db.collection("users").document(uid).collection("progress_places")
                    .get(Source.SERVER)
                    .addOnSuccessListener { ps ->
                        for (doc in ps.documents) doc.reference.delete()
                        Toast.makeText(this, "All visits reset (Firebase + Local)", Toast.LENGTH_SHORT).show()
                        syncFromFirestore()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Reset locally (Firebase issue)", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Reset locally (Firebase offline)", Toast.LENGTH_SHORT).show()
            }
    }

    // -------------------- SEED GLOBAL DATA ONCE (OPTIONAL) --------------------
    // Keep this ONLY if you still want a zones collection storing static geometry.
    private fun seedFirestoreOnceIfNeeded() {
        val seedPrefs = getSharedPreferences("seed_flags", MODE_PRIVATE)
        val alreadySeeded = seedPrefs.getBoolean("seeded_v1", false)
        if (alreadySeeded) return

        // Seed static zones + places to /zones (NOT visited per-user)
        for (z in zones) {
            val zoneRef = db.collection("zones").document(z.locationName)
            zoneRef.set(
                mapOf(
                    "locationName" to z.locationName,
                    "latitude" to z.latitude,
                    "longitude" to z.longitude,
                    "radius" to z.radius
                ),
                SetOptions.merge()
            )

            val places = zonePlaceMap[z.locationName] ?: emptyList()
            for (p in places) {
                zoneRef.collection("places").document(p.placeName).set(
                    mapOf(
                        "placeName" to p.placeName,
                        "latitude" to p.latitude,
                        "longitude" to p.longitude,
                        "radius" to p.radius
                    ),
                    SetOptions.merge()
                )
            }
        }

        seedPrefs.edit().putBoolean("seeded_v1", true).apply()
        Toast.makeText(this, "✅ Firestore seeded (static zones + places)", Toast.LENGTH_LONG).show()
    }

    // -------------------- AI BUTTON ENABLE --------------------
    private fun updateAiAvailability() {
        val zone = inRangeZoneName
        val place = inRangePlaceName

        val canAsk = (zone != null && place != null && isPlaceVisitedLocal(zone, place))
        btnOpenAi.isEnabled = canAsk

        if (canAsk) btnOpenAi.setTextColor(Color.parseColor("#22B14C"))
        else btnOpenAi.setTextColor(Color.parseColor("#9E9E9E"))
    }

    // -------------------- AUTH --------------------
    private fun ensureUserSignedIn(onDone: (() -> Unit)? = null) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onDone?.invoke()
            return
        }

        // Not logged in -> send to SignIn
        Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    private fun requireLoggedIn(onDone: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // Not logged in -> send to SignIn
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        onDone()
    }

    private fun logMapUsage() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 1) total opens counter (increments every time map screen opens)
        val statsRef = db.collection("feature_usage")
            .document("map")
            .collection("stats")
            .document("main")

        statsRef.set(mapOf("openCount" to com.google.firebase.firestore.FieldValue.increment(1)), SetOptions.merge())

        // 2) unique users for map (create a doc once)
        val uniqueUserRef = db.collection("feature_usage")
            .document("map")
            .collection("users")
            .document(uid)

        uniqueUserRef.set(
            mapOf(
                "firstSeen" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "uid" to uid
            ),
            SetOptions.merge()
        )
    }

    private fun logGeofenceEvaluation(
        type: String,              // "ZONE" or "PLACE"
        zone: String,
        place: String?,
        gpsAccuracy: Float,
        detectionDelaySec: Double
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        val data = hashMapOf(
            "uid" to uid,
            "type" to type,
            "zone" to zone,
            "place" to (place ?: ""),
            "gpsAccuracyMeters" to gpsAccuracy.toDouble(),
            "detectionDelaySec" to detectionDelaySec,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("geofence_evaluation")
            .add(data)
            .addOnFailureListener { e ->
                Log.e("GEOFENCE_EVAL", "Failed to write evaluation log", e)
            }
    }

    private fun updateEvaluationSummary(
        type: String,
        zone: String,
        place: String?,
        gpsAccuracy: Double,
        delaySec: Double
    ) {
        val summaryRef = db.collection("geofence_summary").document("main")

        val updateMap = hashMapOf<String, Any>(
            "totalCount" to FieldValue.increment(1),
            "sumAccuracy" to FieldValue.increment(gpsAccuracy),
            "sumDelay" to FieldValue.increment(delaySec)
        )

        // zone level
        updateMap["zones.$zone.count"] = FieldValue.increment(1)
        updateMap["zones.$zone.sumAccuracy"] = FieldValue.increment(gpsAccuracy)
        updateMap["zones.$zone.sumDelay"] = FieldValue.increment(delaySec)

        // place level (optional)
        if (!place.isNullOrBlank()) {
            val key = "${zone}__${place}".replace(".", "_")
            updateMap["places.$key.count"] = FieldValue.increment(1)
            updateMap["places.$key.sumAccuracy"] = FieldValue.increment(gpsAccuracy)
            updateMap["places.$key.sumDelay"] = FieldValue.increment(delaySec)
        }

        summaryRef.set(updateMap, SetOptions.merge())
    }


    private fun recordEvaluation(
        type: String,
        zone: String,
        place: String?,
        gpsAccuracy: Float,
        detectionDelaySec: Double
    ) {
        logGeofenceEvaluation(type, zone, place, gpsAccuracy, detectionDelaySec)
        updateEvaluationSummary(type, zone, place, gpsAccuracy.toDouble(), detectionDelaySec)
    }
}
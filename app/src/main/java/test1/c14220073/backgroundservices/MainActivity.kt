package test1.c14220073.backgroundservices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnStart: Button
    private lateinit var btnTraining: Button

    private var isTrainingModeActive = false

    companion object {
        const val ACTION_SERVICE_STOPPED_JUMP = "test1.c14220073.backgroundservices.ACTION_SERVICE_STOPPED_JUMP"
        const val REQUEST_CODE_PERMISSIONS = 101
    }

    private val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        tvResult = findViewById(R.id.tvResult)
        btnStart = findViewById(R.id.btnStart)
        btnTraining = findViewById(R.id.btnTraining)

        handleIntent(intent)

        // Tombol Tugas 1
        btnStart.setOnClickListener {
            isTrainingModeActive = false
            checkPermissionAndStart()
        }

        // Tombol Tugas 2
        btnTraining.setOnClickListener {
            isTrainingModeActive = true
            checkPermissionAndStart()
        }
    }

    private fun checkPermissionAndStart() {
        if (hasAllPermissions()) {
            startTrackerService()
        } else {
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun hasAllPermissions(): Boolean {
        return permissionsToRequest.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun startTrackerService() {
        val serviceIntent = Intent(this, TrackerService::class.java).apply {
            putExtra("IS_TRAINING_MODE", isTrainingModeActive) // Mengirim mode ke Service
        }
        ContextCompat.startForegroundService(this, serviceIntent)

        statusTextView.text = if (isTrainingModeActive) "Mode Latihan berjalan...\nLakukan lompatan Anda!" else "Background service sedang berjalan..."
        tvResult.text = ""
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startTrackerService()
                finish()
            } else {
                statusTextView.text = "Izin ditolak, service tidak dapat dijalankan."
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SERVICE_STOPPED_JUMP) {
            val isTrainingFinished = intent.getBooleanExtra("IS_TRAINING_FINISHED", false)
            val jumpCount = intent.getIntExtra("JUMP_COUNT", 0)

            if (isTrainingFinished) {
                // LOGIKA TUGAS 2
                statusTextView.text = "Latihan Berhasil Diselesaikan!"
                tvResult.text = "Total Lompatan: $jumpCount"

                // Buka otomatis kalender untuk mencatat event
                val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, "Latihan Kebugaran")
                    putExtra(CalendarContract.Events.DESCRIPTION, "Berhasil menyelesaikan $jumpCount lompatan menggunakan aplikasi Tracker.")
                }
                startActivity(calendarIntent)
            } else {
                // LOGIKA TUGAS 1
                statusTextView.text = "Background services sudah berhenti karena Anda melompat"
                tvResult.text = ""
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
}
package test1.c14220073.backgroundservices

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.abs

class TrackerService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING = "tracker_service_running"
    private val NOTIFICATION_CHANNEL_ID_JUMP_DETECTED = "tracker_jump_detected"
    private val NOTIFICATION_ID_SERVICE_RUNNING = 1
    private val NOTIFICATION_ID_JUMP_DETECTED = 2

    // Variabel tambahan untuk Tugas 2
    private var isTrainingMode = false
    private var jumpCount = 0
    private var canCountJump = true // Mencegah penghitungan ganda dalam satu pantulan

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Menerima perintah dari MainActivity apakah ini Mode Latihan
        isTrainingMode = intent?.getBooleanExtra("IS_TRAINING_MODE", false) ?: false
        jumpCount = 0

        startForeground(NOTIFICATION_ID_SERVICE_RUNNING, buildServiceRunningNotification())
        startJumpDetection()
        return START_STICKY
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val serviceChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING, "Service Running Channel", NotificationManager.IMPORTANCE_LOW)
            val jumpChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID_JUMP_DETECTED, "Jump Detected Channel", NotificationManager.IMPORTANCE_HIGH)
            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(jumpChannel)
        }
    }

    private fun startJumpDetection() {
        accelerometer?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    private fun buildServiceRunningNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val titleText = if (isTrainingMode) "Mode Latihan Berjalan" else "Pelacak Aktivitas Latar"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING)
            .setContentTitle(titleText)
            .setContentText("Aplikasi Tracker Services sedang berjalan...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val y = event.values[1]

            if (isTrainingMode) {
                // LOGIKA TUGAS 2: Mode Latihan (Threshold 12)
                if (abs(y) > 12.0 && canCountJump) {
                    jumpCount++
                    canCountJump = false // Mengunci sementara
                    Log.d("TrackerService", "Lompatan ke-$jumpCount terdeteksi!")

                    // Lebih dari 2 langkah (Berarti minimal 3)
                    if (jumpCount > 2) {
                        sensorManager.unregisterListener(this)
                        showJumpNotification(isTrainingFinished = true, count = jumpCount)
                        stopSelf()
                    }
                } else if (abs(y) < 5.0) {
                    canCountJump = true // Membuka kunci jika ponsel sudah stabil
                }
            } else {
                // LOGIKA TUGAS 1: Mode Biasa (Threshold 15.0)
                if (abs(y) > 15.0) {
                    sensorManager.unregisterListener(this)
                    showJumpNotification(isTrainingFinished = false, count = 1)
                    stopSelf()
                }
            }
        }
    }

    private fun showJumpNotification(isTrainingFinished: Boolean, count: Int) {
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_SERVICE_STOPPED_JUMP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("IS_TRAINING_FINISHED", isTrainingFinished)
            putExtra("JUMP_COUNT", count)
        }
        val pendingMainActivityIntent = PendingIntent.getActivity(this, 1, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationTitle = if (isTrainingFinished) "Latihan Selesai!" else "Lompatan Terdeteksi"
        val notificationText = if (isTrainingFinished) "Mencapai $count lompatan. Ketuk untuk melihat kalender." else "Service Berhenti. Ketuk untuk membuka aplikasi."

        val jumpNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_JUMP_DETECTED)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingMainActivityIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try { NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_JUMP_DETECTED, jumpNotification) }
        catch (e: SecurityException) { Log.e("TrackerService", "Error notifikasi: ${e.message}") }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
    override fun onBind(p0: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
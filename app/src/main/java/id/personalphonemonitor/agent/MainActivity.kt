package id.personalphonemonitor.agent

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var workerUrl: EditText
    private lateinit var deviceId: EditText
    private lateinit var token: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 36)
        }
        fun label(text: String) = TextView(this).apply {
            this.text = text
            textSize = 15f
            setPadding(0, 18, 0, 6)
        }
        workerUrl = EditText(this).apply {
            hint = "https://...workers.dev"
            setSingleLine(true)
            setText("https://personal-phone-monitor.rosadahwk.workers.dev")
        }
        deviceId = EditText(this).apply { hint = "Device ID"; setSingleLine(true) }
        token = EditText(this).apply {
            hint = "Device Token"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        status = TextView(this).apply { text = "Status: Belum dikonfigurasi"; textSize = 14f }

        root.addView(TextView(this).apply {
            text = "Personal Phone Monitor V2"
            textSize = 22f
        })
        root.addView(TextView(this).apply {
            text = "Aplikasi ini mengirim status baterai dan notifikasi yang diizinkan ke dashboard milik Anda. Ikon aplikasi tetap terlihat."
            textSize = 13f
        })
        root.addView(label("Worker URL")); root.addView(workerUrl)
        root.addView(label("Device ID")); root.addView(deviceId)
        root.addView(label("Device Token")); root.addView(token)
        root.addView(Button(this).apply {
            text = "Simpan konfigurasi"
            setOnClickListener { saveConfig() }
        })
        root.addView(Button(this).apply {
            text = "Aktifkan akses notifikasi"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        })
        root.addView(Button(this).apply {
            text = "Minta izin notifikasi aplikasi"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= 33) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200
                    )
                } else status.text = "Izin notifikasi sistem tidak diperlukan pada versi Android ini."
            }
        })
        root.addView(Button(this).apply {
            text = "Tes koneksi dan aktifkan heartbeat"
            setOnClickListener { startMonitoring() }
        })
        root.addView(Button(this).apply {
            text = "Hentikan heartbeat"
            setOnClickListener {
                WorkManager.getInstance(this@MainActivity).cancelUniqueWork("ppm_heartbeat")
                status.text = "Heartbeat dihentikan. Akses notifikasi dapat dimatikan melalui Pengaturan Android."
            }
        })
        root.addView(status)
        setContentView(root)
        workerUrl.setText(getSharedPreferences("monitor_config", 0)
            .getString("worker_url", "https://personal-phone-monitor.rosadahwk.workers.dev"))
        deviceId.setText(getSharedPreferences("monitor_config", 0).getString("device_id", ""))
        token.setText(SecureStore.read(this, "device_token"))
    }

    private fun saveConfig() {
        val url = workerUrl.text.toString().trim().trimEnd('/')
        val id = deviceId.text.toString().trim()
        val secret = token.text.toString().trim()
        if (!url.startsWith("https://") || id.isBlank() || secret.isBlank()) {
            status.text = "Periksa Worker URL (HTTPS), Device ID, dan Device Token."
            return
        }
        getSharedPreferences("monitor_config", 0).edit()
            .putString("worker_url", url).putString("device_id", id).apply()
        SecureStore.save(this, "device_token", secret)
        token.setText("")
        status.text = "Konfigurasi tersimpan terenkripsi di perangkat."
    }

    private fun startMonitoring() {
        val savedToken = SecureStore.read(this, "device_token")
        if (savedToken.isBlank()) {
            status.text = "Simpan Device ID dan Device Token terlebih dahulu."
            return
        }
        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ppm_heartbeat", ExistingPeriodicWorkPolicy.UPDATE, request
        )
        thread {
            val result = try { ApiClient.heartbeat(this) } catch (e: Exception) { e.message ?: "Gagal" }
            runOnUiThread { status.text = "Tes heartbeat: $result\nPembaruan berkala dijadwalkan (minimal sekitar 15 menit)." }
        }
        status.text = "Heartbeat dijadwalkan. Pastikan akses notifikasi diberikan jika ingin mengirim notifikasi."
    }
}

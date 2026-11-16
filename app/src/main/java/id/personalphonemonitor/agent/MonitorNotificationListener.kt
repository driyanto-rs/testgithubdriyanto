package id.personalphonemonitor.agent

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.concurrent.Executors

class MonitorNotificationListener : NotificationListenerService() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        // Konten hanya dikirim jika akses notifikasi telah diberikan secara eksplisit.
        executor.execute {
            try {
                val appName = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(sbn.packageName, 0)
                    ).toString()
                } catch (_: Exception) { sbn.packageName }
                ApiClient.notification(this, appName, sbn.packageName, title, text)
            } catch (e: Exception) {
                Log.w("PPM", "Pengiriman notifikasi gagal", e)
            }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}

package id.personalphonemonitor.agent

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private fun request(context: Context, path: String, payload: JSONObject): String {
        val prefs = context.getSharedPreferences("monitor_config", Context.MODE_PRIVATE)
        val base = prefs.getString("worker_url", "https://personal-phone-monitor.rosadahwk.workers.dev")
            ?.trimEnd('/') ?: return "Worker URL belum diatur"
        val deviceId = prefs.getString("device_id", "") ?: ""
        // Token dibaca langsung dari penyimpanan terenkripsi Android.
        val plainToken = SecureStore.read(context, "device_token")
        if (deviceId.isBlank() || plainToken.isBlank()) return "Device ID/Token belum diatur"

        val connection = (URL(base + path).openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $plainToken")
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code in 200..299) "HTTP $code: $body" else "HTTP $code: $body"
        } finally {
            connection.disconnect()
        }
    }

    fun heartbeat(context: Context): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        val batteryIntent = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val status = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN
        ) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val network = when {
            caps == null -> "offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
        val deviceId = context.getSharedPreferences("monitor_config", Context.MODE_PRIVATE)
            .getString("device_id", "") ?: ""
        val body = JSONObject()
            .put("device_id", deviceId)
            .put("battery_level", level)
            .put("is_charging", if (charging) 1 else 0)
            .put("network_type", network)
        return request(context, "/api/heartbeat", body)
    }

    fun notification(context: Context, appName: String, packageName: String, title: String, content: String): String {
        val deviceId = context.getSharedPreferences("monitor_config", Context.MODE_PRIVATE)
            .getString("device_id", "") ?: ""
        val body = JSONObject()
            .put("device_id", deviceId)
            .put("app_name", appName)
            .put("package_name", packageName)
            .put("title", title)
            .put("content", content)
        return request(context, "/api/notifications", body)
    }
}

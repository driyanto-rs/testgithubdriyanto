package id.personalphonemonitor.agent

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class HeartbeatWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val result = ApiClient.heartbeat(applicationContext)
            if (result.startsWith("HTTP 2")) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

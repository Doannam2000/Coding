package com.nantcompany.clipy.export.job

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nantcompany.clipy.export.output.OutputMedia

class ProcessingWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @androidx.media3.common.util.UnstableApi
    private val jobManager = ClipyExportProvider.getJobManager(context)

    @androidx.media3.common.util.UnstableApi
    override suspend fun doWork(): Result {
        val requestJson = inputData.getString("request_json") ?: return Result.failure()
        val request = ProcessingRequest.fromJson(requestJson) ?: return Result.failure()

        setForeground(createForegroundInfo(0, "Preparing export..."))

        val result = jobManager.process(request, object : ProcessingJobManager.ProgressCallback {
            override fun onProgress(event: ProcessEvent.ProgressUpdate) {
                setProgressAsync(workDataOf("progress" to event.percent, "status" to event.status))
                notificationManager.notify(NOTIFICATION_ID, createNotification(event.percent, event.status, true))
            }
        })

        return when (result) {
            is ProcessEvent.Completed -> {
                showSuccessNotification(result.output)
                Result.success(workDataOf("output_path" to result.output.path, "output_id" to result.output.id))
            }
            is ProcessEvent.Cancelled -> {
                notificationManager.cancel(NOTIFICATION_ID)
                Result.failure(workDataOf("status" to "cancelled"))
            }
            is ProcessEvent.Failed -> {
                showFailureNotification(result.error.message ?: "Processing error")
                Result.failure(workDataOf("error" to (result.error.message ?: "Unknown error")))
            }
            else -> Result.failure()
        }
    }

    private fun createForegroundInfo(progress: Int, status: String): ForegroundInfo {
        createChannel()
        val notification = createNotification(progress, status, true)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Clipy Media Processing", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(progress: Int, status: String, isRunning: Boolean): Notification {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Clipy is $status")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isRunning)
            .setAutoCancel(!isRunning)
        if (isRunning) builder.setProgress(100, progress, false)
        return builder.build()
    }

    private fun showSuccessNotification(output: OutputMedia) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Clipy: Export Complete")
            .setContentText("Finished creating ${output.fileName}")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(SUCCESS_NOTIFICATION_ID, notification)
    }

    private fun showFailureNotification(message: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Clipy: Export Failed")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(FAILURE_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "clipy_processing_channel"
        private const val NOTIFICATION_ID = 8888
        private const val SUCCESS_NOTIFICATION_ID = 8889
        private const val FAILURE_NOTIFICATION_ID = 8890
    }
}

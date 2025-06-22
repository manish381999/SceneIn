package com.tie.vibein.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tie.vibein.chat.data.repository.ChatRepository

class DeliveryReportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_MESSAGE_IDS = "KEY_MESSAGE_IDS"
        // Use a different tag to easily filter this worker's logs
        private const val TAG = "DELIVERY_WORKER_LIFECYCLE"
    }

    override suspend fun doWork(): Result {
        // --- THIS IS THE NEW LOG MESSAGE ---
        Log.d(TAG, "WORKER HAS STARTED EXECUTION. The OS has finally allowed this job to run.")

        val messageIds = inputData.getStringArray(KEY_MESSAGE_IDS)?.toList()

        if (messageIds.isNullOrEmpty()) {
            return Result.success()
        }

        Log.d(TAG, "Worker is now attempting to report delivery for IDs: $messageIds")

        return try {
            ChatRepository().markMessagesAsDelivered(messageIds)
            Log.d(TAG, "Successfully reported delivery.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report delivery. Retrying...", e)
            Result.retry()
        }
    }
}
package com.hotcs.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hotcs.app.data.HotRepository
import com.hotcs.app.data.Settings

class NotifyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val ctx = applicationContext
            val url = Settings.url(ctx)
            if (url.isBlank()) return Result.success()

            val repo = HotRepository(ctx)
            val items = repo.fetch(url)
            if (items.isEmpty()) return Result.success()

            val already = repo.lastNotifiedIds()
            val newIds = items.map { it.id }.filter { it !in already }.take(5)

            if (newIds.isNotEmpty()) {
                Notifier.ensureChannel(ctx)
                newIds.forEach { id ->
                    val item = items.first { it.id == id }
                    Notifier.show(ctx, item.id, item.title, item.summary)
                }
                repo.saveNotifiedIds((already + newIds).takeLast(200))
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

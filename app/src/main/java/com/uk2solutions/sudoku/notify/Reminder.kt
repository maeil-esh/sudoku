package com.uk2solutions.sudoku.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.uk2solutions.sudoku.MainActivity
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.data.GameDao
import com.uk2solutions.sudoku.data.GameEntity
import com.uk2solutions.sudoku.data.SettingsRepository
import com.uk2solutions.sudoku.domain.game.Streak
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun gameDao(): GameDao
    fun settingsRepository(): SettingsRepository
}

object ReminderScheduler {
    private const val WORK_NAME = "daily_reminder"

    /** 매일 [hour]시 무렵(시스템이 절전 상태 등에 따라 조금 늦출 수 있음)에 확인 작업을 돌린다. */
    fun schedule(context: Context, hour: Int) {
        val now = LocalDateTime.now()
        var target = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, 0))
        if (!target.isAfter(now)) target = target.plusDays(1)
        val delay = Duration.between(now, target).toMinutes()
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
}

class ReminderWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(ctx, ReminderEntryPoint::class.java)
        if (!ep.settingsRepository().settings.first().notifyEnabled) return Result.success()
        val today = LocalDate.now()
        // 오늘의 도전을 이미 끝냈으면 알리지 않는다.
        if (ep.gameDao().observeDaily(today.toString()).first()?.status == GameEntity.STATUS_COMPLETED) return Result.success()
        val streak = Streak.current(ep.gameDao().observeDailyCompleted().first().map { LocalDate.parse(it) }.toSet(), today)
        Notifier.show(ctx, streak)
        return Result.success()
    }
}

object Notifier {
    private const val CHANNEL_ID = "daily_reminder"

    fun show(context: Context, streak: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notify_channel_name), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val body = if (streak > 0) context.getString(R.string.notify_body_streak, streak) else context.getString(R.string.notify_body)
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(context.getString(R.string.notify_title))
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1, n)
    }
}

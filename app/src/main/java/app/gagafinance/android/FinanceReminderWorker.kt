package app.gagafinance.android

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** User-opt-in, privacy-preserving reminder; no names or monetary amounts on the lockscreen. */
object FinanceReminderSettings {
    private const val PREFS = "finance_reminder_prefs"
    private const val ENABLED = "enabled"
    private const val WORK = "daily_local_finance_reminders"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false)

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun update(context: Context, enabled: Boolean) {
        require(!enabled || canNotify(context)) {
            "Enable notifications in Android Settings first"
        }
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED, enabled).apply()

        val manager = WorkManager.getInstance(app)
        if (!enabled) {
            manager.cancelUniqueWork(WORK)
            return
        }
        createChannel(app)
        val current = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(current)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = (next.timeInMillis - current.timeInMillis).coerceAtLeast(0)
        val request = PeriodicWorkRequestBuilder<FinanceReminderWorker>(
            24, TimeUnit.HOURS
        ).setInitialDelay(delay, TimeUnit.MILLISECONDS).build()

        manager.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private const val CHANNEL = "gaga_finance_daily_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL,
                "Daily finance reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Optional generic alerts for overdue and upcoming manual bills"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun channelId(): String = CHANNEL
}

class FinanceReminderWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        if (!FinanceReminderSettings.isEnabled(context) ||
            !FinanceReminderSettings.canNotify(context)
        ) return@withContext Result.success()

        try {
            val store = LocalFinanceStore(context)
            val focus = try {
                DailyFocusMath.calculate(
                    entries = emptyList(),
                    budgets = emptyList(),
                    goals = emptyList(),
                    bills = store.listBills(),
                    loans = store.listLoans(),
                    repayments = store.listLoanRepayments()
                )
            } finally { store.close() }

            val dueBills = focus.overdueBills.size + focus.upcomingBills.size
            val dueLoans = focus.overdueLoans.size + focus.upcomingLoans.size
            if (dueBills + dueLoans == 0) return@withContext Result.success()

            val today = DailyFocusMath.dayStart(System.currentTimeMillis())
            val prefs = context.getSharedPreferences("finance_reminder_prefs",
                Context.MODE_PRIVATE)
            if (prefs.getLong("last_alert_day", -1) == today) {
                return@withContext Result.success()
            }

            FinanceReminderSettings.createChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(
                context, FinanceReminderSettings.channelId()
            )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("GaGa Finance · Check your due items")
                .setContentText(
                    dueBills.toString() + " bill items · " +
                        dueLoans.toString() + " loan plans to review"
                )
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            if (FinanceReminderSettings.canNotify(context)) {
                try {
                    NotificationManagerCompat.from(context).notify(701, notification)
                    prefs.edit().putLong("last_alert_day", today).apply()
                } catch (_: SecurityException) {
                    // Permission revoked during execution: don't crash, don't retry aggressively.
                }
            }
            Result.success()
        } catch (_: Exception) {
            // Avoid infinite retries or lockscreen disclosure on an invalid local database.
            Result.failure()
        }
    }
}

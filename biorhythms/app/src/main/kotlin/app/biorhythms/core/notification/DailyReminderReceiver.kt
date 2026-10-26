package app.biorhythms.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MORNING_REMINDER -> {
                NotificationHelper.sendMorningNotification(context)
            }
            ACTION_EVENING_REMINDER -> {
                NotificationHelper.sendEveningNotification(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // 开机后重新调度每日闹钟
                NotificationHelper.scheduleDailyAlarms(context)
            }
        }
    }

    companion object {
        const val ACTION_MORNING_REMINDER = "app.biorhythms.ACTION_MORNING_REMINDER"
        const val ACTION_EVENING_REMINDER = "app.biorhythms.ACTION_EVENING_REMINDER"
    }
}

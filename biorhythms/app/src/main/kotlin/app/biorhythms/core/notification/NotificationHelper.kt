package app.biorhythms.core.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.biorhythms.MainActivity
import app.biorhythms.R
import app.biorhythms.core.algorithm.BioCalculator
import app.biorhythms.core.algorithm.RhythmType
import app.biorhythms.data.DataRepository
import java.time.LocalDate
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_ID = "biorhythm_care_channel"
    private const val CHANNEL_NAME = "生物节律关怀预警"
    private const val NOTIFICATION_ID_MORNING = 1001
    private const val NOTIFICATION_ID_EVENING = 1002
    private const val NOTIFICATION_ID_TEST = 1003

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "每日晨间状态播报与晚间临界日警示"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 发送晨间 08:00 播报
     */
    fun sendMorningNotification(context: Context) {
        val repo = DataRepository.getInstance(context)
        if (!repo.notificationsEnabled.value) return
        val profile = repo.selectedProfile.value ?: repo.profiles.value.firstOrNull() ?: return
        val today = LocalDate.now()
        val days = BioCalculator.getDiffDays(profile.birthDate, today)
        val overall = BioCalculator.getOverallEvaluation(days)

        val criticalTypes = mutableListOf<String>()
        if (BioCalculator.isCriticalDay(RhythmType.PHYSIC, days)) criticalTypes.add("体力")
        if (BioCalculator.isCriticalDay(RhythmType.EMOTIONAL, days)) criticalTypes.add("情绪")
        if (BioCalculator.isCriticalDay(RhythmType.INTELLECT, days)) criticalTypes.add("智力")

        val title = if (criticalTypes.isNotEmpty()) {
            "⚠️ 今日临界日提醒 · ${profile.name}"
        } else {
            "☀️ 晨间节律播报 · ${profile.name}"
        }

        val content = if (criticalTypes.isNotEmpty()) {
            "今日为【${criticalTypes.joinToString("、")}】临界穿零日，机体易敏感波动，请注意防范疲劳并多加体恤自身。"
        } else {
            "今日全天综合运势：✦ ${overall.label}。祝您精力充沛，从容顺遂！"
        }

        sendNotification(context, NOTIFICATION_ID_MORNING, title, content)
    }

    /**
     * 发送晚间 20:00 临界日预警
     */
    fun sendEveningNotification(context: Context) {
        val repo = DataRepository.getInstance(context)
        if (!repo.notificationsEnabled.value) return
        val profile = repo.selectedProfile.value ?: repo.profiles.value.firstOrNull() ?: return
        val tomorrow = LocalDate.now().plusDays(1)
        val daysTomorrow = BioCalculator.getDiffDays(profile.birthDate, tomorrow)

        val criticalTypes = mutableListOf<String>()
        if (BioCalculator.isCriticalDay(RhythmType.PHYSIC, daysTomorrow)) criticalTypes.add("体力")
        if (BioCalculator.isCriticalDay(RhythmType.EMOTIONAL, daysTomorrow)) criticalTypes.add("情绪")
        if (BioCalculator.isCriticalDay(RhythmType.INTELLECT, daysTomorrow)) criticalTypes.add("智力")

        if (criticalTypes.isNotEmpty()) {
            val title = "🌙 明日临界日关怀预警 · ${profile.name}"
            val content = "明天将进入【${criticalTypes.joinToString("、")}】临界日，情绪或精力可能较易波动，今晚宜早睡休养，明天尽量避免重大争执。"
            sendNotification(context, NOTIFICATION_ID_EVENING, title, content)
        } else {
            val overall = BioCalculator.getOverallEvaluation(daysTomorrow)
            val title = "🌙 明日节律前瞻 · ${profile.name}"
            val content = "明天全天状态平稳顺遂 (✦ ${overall.label})，今晚祝您好梦，明日扬帆起航！"
            sendNotification(context, NOTIFICATION_ID_EVENING, title, content)
        }
    }

    /**
     * 即刻测试发送通知
     */
    fun sendTestNotification(context: Context) {
        createNotificationChannel(context)
        val repo = DataRepository.getInstance(context)
        val profile = repo.selectedProfile.value ?: repo.profiles.value.firstOrNull()
        val name = profile?.name ?: "自己"
        val title = "🔔 生物节律通知已成功就绪 · $name"
        val content = "系统已为您配置好每日 08:00 晨间状态播报与 20:00 晚间临界日警示。守护身心，随顺自然。"
        sendNotification(context, NOTIFICATION_ID_TEST, title, content)
    }

    private fun sendNotification(context: Context, id: Int, title: String, content: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * 配置每日定点 AlarmManager
     */
    fun scheduleDailyAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 晨间 08:00
        scheduleAlarmAt(context, alarmManager, 8, 0, 101, DailyReminderReceiver.ACTION_MORNING_REMINDER)
        // 晚间 20:00
        scheduleAlarmAt(context, alarmManager, 20, 0, 102, DailyReminderReceiver.ACTION_EVENING_REMINDER)
    }

    private fun scheduleAlarmAt(
        context: Context,
        alarmManager: AlarmManager,
        hour: Int,
        minute: Int,
        requestCode: Int,
        action: String
    ) {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

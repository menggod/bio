package app.biorhythms.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import app.biorhythms.MainActivity
import app.biorhythms.R
import app.biorhythms.core.algorithm.BioCalculator
import app.biorhythms.core.algorithm.RhythmType
import app.biorhythms.data.DataRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class BioWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            try {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            } catch (e: Throwable) {
                Log.e("BioWidgetProvider", "Error updating widget id: $appWidgetId", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_UPDATE_WIDGET ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, BioWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    onUpdate(context, appWidgetManager, appWidgetIds)
                }
            } catch (e: Throwable) {
                Log.e("BioWidgetProvider", "Error in onReceive update", e)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "app.biorhythms.ACTION_UPDATE_WIDGET"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_biorhythm)

            // 点击小部件跳转打开主页
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            try {
                val repo = DataRepository.getInstance(context)
                val profile = repo.selectedProfile.value ?: repo.profiles.value.firstOrNull()
                val today = LocalDate.now()

                if (profile != null) {
                    val days = BioCalculator.getDiffDays(profile.birthDate, today)
                    val pVal = BioCalculator.getValue(RhythmType.PHYSIC, days.toDouble())
                    val eVal = BioCalculator.getValue(RhythmType.EMOTIONAL, days.toDouble())
                    val iVal = BioCalculator.getValue(RhythmType.INTELLECT, days.toDouble())
                    val pPercent = BioCalculator.getPercent(RhythmType.PHYSIC, days.toDouble()).toInt().coerceIn(0, 100)
                    val ePercent = BioCalculator.getPercent(RhythmType.EMOTIONAL, days.toDouble()).toInt().coerceIn(0, 100)
                    val iPercent = BioCalculator.getPercent(RhythmType.INTELLECT, days.toDouble()).toInt().coerceIn(0, 100)
                    val overall = BioCalculator.getOverallEvaluation(days)

                    val dateFormatter = DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINESE)

                    views.setTextViewText(R.id.widget_text_name, profile.name)
                    views.setTextViewText(R.id.widget_text_date, today.format(dateFormatter))

                    // 数值
                    views.setTextViewText(R.id.widget_text_physic_val, formatValue(pVal))
                    views.setTextViewText(R.id.widget_text_emotional_val, formatValue(eVal))
                    views.setTextViewText(R.id.widget_text_intellect_val, formatValue(iVal))

                    // 进度条 (0~100)
                    views.setProgressBar(R.id.widget_progress_physic_bar, 100, pPercent, false)
                    views.setProgressBar(R.id.widget_progress_emotional_bar, 100, ePercent, false)
                    views.setProgressBar(R.id.widget_progress_intellect_bar, 100, iPercent, false)

                    views.setTextViewText(R.id.widget_text_status, "✦ ${overall.label}")

                    val hasNotes = repo.notes.value.any { it.profileId == profile.id && it.date == today }
                    views.setViewVisibility(R.id.widget_text_notes_badge, if (hasNotes) View.VISIBLE else View.GONE)
                } else {
                    views.setTextViewText(R.id.widget_text_name, "生物节律")
                    views.setTextViewText(R.id.widget_text_status, "✦ 点击进入应用设置档案")
                }
            } catch (e: Throwable) {
                Log.e("BioWidgetProvider", "Error computing biorhythms for widget", e)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatValue(v: Double): String {
            val prefix = if (v >= 0) "+" else ""
            return "$prefix${String.format("%.0f", v)}%"
        }

        fun updateAllWidgets(context: Context) {
            try {
                val intent = Intent(context, BioWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_WIDGET
                }
                context.sendBroadcast(intent)

                val intent2x2 = Intent(context, BioWidgetProvider2x2::class.java).apply {
                    action = ACTION_UPDATE_WIDGET
                }
                context.sendBroadcast(intent2x2)
            } catch (e: Throwable) {
                Log.e("BioWidgetProvider", "Error sending widget broadcast", e)
            }
        }
    }
}

package app.biorhythms.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import app.biorhythms.MainActivity
import app.biorhythms.R
import app.biorhythms.core.algorithm.BioCalculator
import app.biorhythms.core.algorithm.RhythmType
import app.biorhythms.data.DataRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class BioWidgetProvider2x2 : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            try {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            } catch (e: Throwable) {
                Log.e("BioWidgetProvider2x2", "Error updating 2x2 widget id: $appWidgetId", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == BioWidgetProvider.ACTION_UPDATE_WIDGET ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, BioWidgetProvider2x2::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    onUpdate(context, appWidgetManager, appWidgetIds)
                }
            } catch (e: Throwable) {
                Log.e("BioWidgetProvider2x2", "Error in 2x2 onReceive update", e)
            }
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_biorhythm_2x2)

            // 点击小部件跳转打开主页
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_2x2_container, pendingIntent)

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

                    val dateFormatter = DateTimeFormatter.ofPattern("M/d E", Locale.CHINESE)

                    views.setTextViewText(R.id.widget_2x2_text_name, profile.name)
                    views.setTextViewText(R.id.widget_2x2_text_date, today.format(dateFormatter))

                    views.setTextViewText(R.id.widget_2x2_text_status, "✦ ${overall.label}")

                    views.setTextViewText(R.id.widget_2x2_text_physic_val, formatValue(pVal))
                    views.setTextViewText(R.id.widget_2x2_text_emotional_val, formatValue(eVal))
                    views.setTextViewText(R.id.widget_2x2_text_intellect_val, formatValue(iVal))

                    views.setProgressBar(R.id.widget_2x2_progress_physic, 100, pPercent, false)
                    views.setProgressBar(R.id.widget_2x2_progress_emotional, 100, ePercent, false)
                    views.setProgressBar(R.id.widget_2x2_progress_intellect, 100, iPercent, false)
                } else {
                    views.setTextViewText(R.id.widget_2x2_text_name, "生物节律")
                    views.setTextViewText(R.id.widget_2x2_text_status, "✦ 点击设置")
                }
            } catch (e: Throwable) {
                Log.e("BioWidgetProvider2x2", "Error computing biorhythms for 2x2 widget", e)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatValue(v: Double): String {
            val prefix = if (v >= 0) "+" else ""
            return "$prefix${String.format("%.0f", v)}%"
        }
    }
}

package app.biorhythms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.biorhythms.core.notification.NotificationHelper
import app.biorhythms.ui.MainApp
import app.biorhythms.ui.theme.BioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化通知通道与每日提醒调度
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.scheduleDailyAlarms(this)

        // Android 13+ 通知权限动态申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            BioTheme {
                MainApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        app.biorhythms.core.widget.BioWidgetProvider.updateAllWidgets(this)
    }
}

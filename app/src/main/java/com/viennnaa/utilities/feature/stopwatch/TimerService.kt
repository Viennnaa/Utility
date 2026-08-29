package com.viennnaa.utilities.feature.stopwatch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.viennnaa.utilities.MainActivity
import com.viennnaa.utilities.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps an ongoing notification while the stopwatch or timer is running, and
 * raises an alert when a countdown reaches zero.
 *
 * The service is not the source of truth for the time. It is handed the same
 * monotonic timestamp the screen uses and derives its text from that, so the
 * two can never drift apart: there is only one number, computed twice.
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                val isTimer = intent.getBooleanExtra(EXTRA_IS_TIMER, false)
                val reference = intent.getLongExtra(EXTRA_REFERENCE, SystemClock.elapsedRealtime())
                ServiceCompat.startForeground(
                    this,
                    ONGOING_ID,
                    buildOngoing(isTimer, reference),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    },
                )
                startTicking(isTimer, reference)
            }
        }
        // Not sticky: without its reference timestamp a restarted service would
        // have nothing meaningful to show.
        return START_NOT_STICKY
    }

    private fun startTicking(isTimer: Boolean, reference: Long) {
        ticker?.cancel()
        ticker = scope.launch {
            val manager = NotificationManagerCompat.from(this@TimerService)
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                if (isTimer && now >= reference) {
                    notifyFinished(manager)
                    stopSelf()
                    return@launch
                }
                if (hasNotificationPermission()) {
                    manager.notify(ONGOING_ID, buildOngoing(isTimer, reference))
                }
                delay(1_000L)
            }
        }
    }

    private fun notifyFinished(manager: NotificationManagerCompat) {
        if (!hasNotificationPermission()) return
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(getString(R.string.stopwatch_notification_done_title))
            .setContentText(getString(R.string.stopwatch_notification_done_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        manager.notify(ALERT_ID, notification)
    }

    private fun buildOngoing(isTimer: Boolean, reference: Long): Notification {
        val now = SystemClock.elapsedRealtime()
        val text = if (isTimer) {
            formatTimer((reference - now).coerceAtLeast(0L))
        } else {
            formatStopwatch((now - reference).coerceAtLeast(0L))
        }
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, TimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(
                getString(
                    if (isTimer) {
                        R.string.stopwatch_notification_timer
                    } else {
                        R.string.stopwatch_notification_stopwatch
                    },
                ),
            )
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent())
            .addAction(0, getString(R.string.stopwatch_notification_stop), stopIntent)
            .build()
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun hasNotificationPermission(): Boolean =
        NotificationManagerCompat.from(this).areNotificationsEnabled()

    private fun createChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                getString(R.string.stopwatch_channel_ongoing),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT,
                getString(R.string.stopwatch_channel_alert),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }

    override fun onDestroy() {
        ticker?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "com.viennnaa.utilities.timer.START"
        private const val ACTION_STOP = "com.viennnaa.utilities.timer.STOP"
        private const val EXTRA_IS_TIMER = "isTimer"
        private const val EXTRA_REFERENCE = "reference"
        private const val CHANNEL_ONGOING = "timer_ongoing"
        private const val CHANNEL_ALERT = "timer_alert"
        private const val ONGOING_ID = 1
        private const val ALERT_ID = 2

        /**
         * @param reference for a stopwatch, when it started; for a timer, when it
         *   ends. Both are [SystemClock.elapsedRealtime] values.
         */
        fun start(context: Context, isTimer: Boolean, reference: Long) {
            val intent = Intent(context, TimerService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_IS_TIMER, isTimer)
                .putExtra(EXTRA_REFERENCE, reference)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, TimerService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}

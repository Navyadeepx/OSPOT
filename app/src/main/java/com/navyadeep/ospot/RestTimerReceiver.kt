package com.navyadeep.ospot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class RestTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_DISMISS_TIMER) {
            stopTimerVibration(context)
            return
        }

        val timerVibration = intent.getBooleanExtra(EXTRA_TIMER_VIBRATION, true)
        if (timerVibration) {
            startTimerVibration(context)
        }
    }

    companion object {
        const val REQUEST_CODE_ALARM = 2001
        const val EXTRA_TIMER_VIBRATION = "extra_timer_vibration"
        const val ACTION_DISMISS_TIMER = "com.navyadeep.ospot.ACTION_DISMISS_TIMER"

        fun schedule(context: Context, durationSeconds: Int, timerVibration: Boolean) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val triggerAtMillis = System.currentTimeMillis() + durationSeconds * 1000L

            val intent = Intent(context, RestTimerReceiver::class.java).apply {
                putExtra(EXTRA_TIMER_VIBRATION, timerVibration)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val showIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                    pendingIntent
                )
            } catch (e: Exception) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } catch (e2: Exception) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                        }
                    } catch (e3: Exception) {
                        e3.printStackTrace()
                    }
                }
            }
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, RestTimerReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ALARM,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            stopTimerVibration(context)
        }
    }
}

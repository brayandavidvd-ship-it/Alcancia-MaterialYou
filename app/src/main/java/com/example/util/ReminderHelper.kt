package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        val prefs = context.getSharedPreferences("alcancia_settings_cache", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("remindersEnabled", false)
        val timeStr = prefs.getString("reminderTime", "20:00") ?: "20:00"
        val freq = prefs.getString("reminderFrequency", "DAILY") ?: "DAILY"

        // If system rebooted or app was updated, just reschedule without firing an alert immediately
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            if (enabled) {
                ReminderHelper.scheduleSavingsReminder(context, true, timeStr, freq)
            }
            return
        }

        // Alarm triggered: keep CPU awake briefly while sending notification
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Alcancía:ReminderWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(5000L) // 5s safety timeout
        }

        try {
            if (enabled) {
                ReminderHelper.sendSavingsNotification(context, isTest = false)
                // Schedule the next occurrence
                ReminderHelper.scheduleSavingsReminder(context, true, timeStr, freq)
            }
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}
        }
    }
}

object ReminderHelper {
    const val CHANNEL_ID = "alcancia_reminders_channel"
    private const val NOTIFICATION_ID = 2001
    private const val ALARM_REQUEST_CODE = 3001
    private const val PREFS_NAME = "alcancia_settings_cache"
    private const val KEY_CUSTOM_REMINDER_MESSAGE = "customReminderMessage"
    const val DEFAULT_REMINDER_MESSAGE =
        "No olvides registrar tus movimientos de hoy y mantener al día tus metas de ahorro."

    fun getReminderMessage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_CUSTOM_REMINDER_MESSAGE, null)?.trim()
        return if (saved.isNullOrBlank()) DEFAULT_REMINDER_MESSAGE else saved
    }

    fun setReminderMessage(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val clean = message.trim()
        if (clean.isBlank() || clean == DEFAULT_REMINDER_MESSAGE) {
            prefs.edit().remove(KEY_CUSTOM_REMINDER_MESSAGE).apply()
        } else {
            prefs.edit().putString(KEY_CUSTOM_REMINDER_MESSAGE, clean).apply()
        }
    }

    fun resetReminderMessage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CUSTOM_REMINDER_MESSAGE).apply()
        return DEFAULT_REMINDER_MESSAGE
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                } catch (_: Exception) {}
            }
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Recordatorios de ahorro"
            val descriptionText = "Notificaciones para registrar gastos y abonar a tus metas"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendSavingsNotification(context: Context, isTest: Boolean = false) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = if (isTest) "¡Notificación de prueba de Alcancía! 🐷" else "Hora de revisar tus finanzas 🐷"
        val message = getReminderMessage(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = if (isTest) NOTIFICATION_ID + 1 else NOTIFICATION_ID
        notificationManager.notify(id, notification)
    }

    fun scheduleSavingsReminder(context: Context, enabled: Boolean, timeStr: String, frequency: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.alcancia.ACTION_SAVINGS_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If scheduled time has already passed today, advance to tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis() + 1000L) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Adjust calendar for selected frequency (Daily, Weekdays, Weekends)
        adjustCalendarForFrequency(calendar, frequency)

        val triggerAtMillis = calendar.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // If exact alarm permission was revoked, fallback safely to setAndAllowWhileIdle
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun adjustCalendarForFrequency(calendar: Calendar, frequency: String) {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        when (frequency) {
            "WEEKDAYS" -> {
                // If it falls on Saturday (7) -> jump 2 days to Monday
                // If Sunday (1) -> jump 1 day to Monday
                if (dayOfWeek == Calendar.SATURDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 2)
                } else if (dayOfWeek == Calendar.SUNDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            "WEEKENDS" -> {
                // Advance until Saturday or Sunday
                while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY &&
                    calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
                ) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            else -> {
                // DAILY: already set for today/tomorrow
            }
        }
    }
}

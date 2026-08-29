package com.medcare.app.background;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.utils.AppointmentStatus;
import com.medcare.app.utils.BackupManager;
import com.medcare.app.utils.PreferencesManager;

import java.util.List;

public class BackgroundScheduler {
    private static final int ALARM_REQUEST_CODE = 4101;
    private static final long FOLLOW_UP_BUFFER_MS = 2 * 60 * 1000;

    private BackgroundScheduler() {}

    public static boolean isExempt(Context context) {
        PowerManager pm = context.getSystemService(PowerManager.class);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static boolean canScheduleExact(Context context) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = context.getSystemService(AlarmManager.class);
        return am != null && am.canScheduleExactAlarms();
    }

    public static long nextEventAt(Context context) {
        PreferencesManager prefs = new PreferencesManager(context);
        if (!prefs.isLoggedIn() || !prefs.isBackgroundWorkEnabled()) return -1;
        long ownerId = prefs.getLoggedInUserId();
        if (ownerId == -1) return -1;
        long now = System.currentTimeMillis();
        long earliest = -1;

        if (prefs.hasBackupPassword()) {
            long period = BackupManager.periodMillis(prefs.getBackupFrequency());
            if (period > 0) {
                long next = prefs.getLastBackupTime() + period;
                if (next < now) next = now;
                earliest = next;
            }
        }

        try {
            AppDatabase db = AppDatabase.getInstance(context);
            List<Appointment> appointments = db.appointmentDao().getAllAppointments(ownerId);
            for (Appointment a : appointments) {
                if (a.getStatus() == null) continue;
                if (!AppointmentStatus.SCHEDULED.equals(a.getStatus())
                        && !AppointmentStatus.RESCHEDULED.equals(a.getStatus())) continue;
                long start = AppointmentRepository.toEpochMillis(a.getDate(), a.getTime());
                if (start <= 0) continue;
                int dur = a.getDuration() > 0 ? a.getDuration() : 0;
                long end = start + dur * 60000L;

                if (prefs.isRemindersEnabled()) {
                    int leadMin = prefs.getReminderLeadMinutes();
                    if (leadMin > 0) {
                        long remindAt = start - leadMin * 60000L;
                        String key = a.getId() + ":" + leadMin;
                        if (remindAt > now && !prefs.getNotifiedReminderKeys().contains(key)
                                && (earliest == -1 || remindAt < earliest)) {
                            earliest = remindAt;
                        }
                    }
                }

                long followAt = end + FOLLOW_UP_BUFFER_MS;
                if (followAt > now && (earliest == -1 || followAt < earliest)) {
                    earliest = followAt;
                }
            }
        } catch (Exception ignored) {}

        return earliest;
    }

    public static void rescheduleAll(Context context) {
        AppDatabase.getExecutor().execute(() -> {
            AlarmManager am = context.getSystemService(AlarmManager.class);
            if (am == null) return;
            PendingIntent pi = alarmPendingIntent(context);
            am.cancel(pi);
            long next = nextEventAt(context);
            if (next <= 0) return;
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
            }
        });
    }

    public static void cancel(Context context) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) return;
        am.cancel(alarmPendingIntent(context));
    }

    private static PendingIntent alarmPendingIntent(Context context) {
        Intent intent = new Intent(context, BackgroundAlarmReceiver.class);
        return PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
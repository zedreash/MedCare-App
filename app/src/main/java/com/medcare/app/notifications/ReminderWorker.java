package com.medcare.app.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.medcare.app.MainActivity;
import com.medcare.app.R;
import com.medcare.app.background.BackgroundScheduler;
import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.utils.AppointmentStatus;
import com.medcare.app.utils.PreferencesManager;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReminderWorker extends Worker {
    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        fireDueNow(getApplicationContext());
        BackgroundScheduler.rescheduleAll(getApplicationContext());
        return Result.success();
    }

    public static void fireDueNow(Context context) {
        PreferencesManager prefs = new PreferencesManager(context);
        if (!prefs.isLoggedIn()) return;
        if (!prefs.isRemindersEnabled()) return;
        int leadMin = prefs.getReminderLeadMinutes();
        if (leadMin <= 0) return;
        if (!ReminderScheduler.canPostNotifications(context)) return;

        long ownerId = prefs.getLoggedInUserId();
        if (ownerId == -1) return;

        try {
            AppDatabase db = AppDatabase.getInstance(context);
            List<Appointment> appointments = db.appointmentDao().getAllAppointments(ownerId);
            Set<String> notified = new HashSet<>(prefs.getNotifiedReminderKeys());
            Calendar now = Calendar.getInstance();
            for (Appointment a : appointments) {
                if (a.getStatus() != null
                        && !a.getStatus().equals(AppointmentStatus.SCHEDULED)
                        && !a.getStatus().equals(AppointmentStatus.RESCHEDULED)) continue;
                Calendar start = parseDateTime(a.getDate(), a.getTime());
                if (start == null) continue;
                if (now.before(start)) {
                    long diffMin = (start.getTimeInMillis() - now.getTimeInMillis()) / 60000;
                    if (diffMin <= leadMin) {
                        String key = a.getId() + ":" + leadMin;
                        if (!notified.contains(key)) {
                            String patientName = "";
                            try {
                                var patient = db.patientDao().getPatientById(a.getPatientId(), ownerId);
                                if (patient != null) patientName = patient.getFullName();
                            } catch (Exception ignored) {}
                            notifyAppointment(context, a, patientName);
                            notified.add(key);
                        }
                    }
                }
            }
            if (notified.size() > 200) {
                notified = new HashSet<>();
            }
            prefs.setNotifiedReminderKeys(notified);
        } catch (Exception ignored) {
        }
    }

    private static void notifyAppointment(Context context, Appointment appointment, String patientName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String title = appointment.getName() != null && !appointment.getName().isEmpty()
                ? appointment.getName() : context.getString(R.string.appointment_reminder);
        String text = (appointment.getTime() != null ? appointment.getTime() : "")
                + (patientName != null && !patientName.isEmpty() ? " \u00B7 " + patientName : "");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pending);

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify((int) (appointment.getId() % Integer.MAX_VALUE), builder.build());
        }
    }

    private static Calendar parseDateTime(String date, String time) {
        try {
            String[] dp = date == null ? new String[0] : date.split("/");
            String[] tp = time == null ? new String[0] : time.split(":");
            if (dp.length != 3 || tp.length != 2) return null;
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(dp[2]), Integer.parseInt(dp[1]) - 1,
                    Integer.parseInt(dp[0]), Integer.parseInt(tp[0]), Integer.parseInt(tp[1]), 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        } catch (Exception e) {
            return null;
        }
    }
}
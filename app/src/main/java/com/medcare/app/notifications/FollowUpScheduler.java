package com.medcare.app.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.utils.AppointmentStatus;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class FollowUpScheduler {
    public static final String CHANNEL_ID = "appointment_followups";
    private static final String WORK_NAME = "appointment_followups";
    private static final String ONE_OFF_PREFIX = "followup_";
    private static final long END_BUFFER_MS = 2 * 60 * 1000;

    private FollowUpScheduler() {}

    public static void createChannel(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Appointment follow-ups",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Asks whether a patient showed up after an appointment ends");
        nm.createNotificationChannel(channel);
    }

    public static void ensureScheduled(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                FollowUpWorker.class, 30, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static void scheduleOneOff(Context context, long appointmentId, long delayMinutes) {
        Data data = new Data.Builder().putLong("appointmentId", appointmentId).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FollowUpWorker.class)
                .setInputData(data)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context).enqueue(request);
    }

    public static void scheduleCheck(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FollowUpWorker.class).build();
        WorkManager.getInstance(context).enqueue(request);
    }

    public static void scheduleAtEnd(Context context, long appointmentId, long endEpochMillis) {
        long delayMs = endEpochMillis - System.currentTimeMillis() + END_BUFFER_MS;
        if (delayMs < 0) delayMs = 0;
        Data data = new Data.Builder().putLong("appointmentId", appointmentId).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FollowUpWorker.class)
                .setInputData(data)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_OFF_PREFIX + appointmentId, ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancelAtEnd(Context context, long appointmentId) {
        WorkManager.getInstance(context).cancelUniqueWork(ONE_OFF_PREFIX + appointmentId);
    }

    public static void backfill(Context context, long ownerId) {
        AppDatabase.getExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                long now = System.currentTimeMillis();
                List<Appointment> appointments = db.appointmentDao().getAllAppointments(ownerId);
                for (Appointment a : appointments) {
                    String status = a.getStatus();
                    if (status == null) continue;
                    if (!AppointmentStatus.SCHEDULED.equals(status)
                            && !AppointmentStatus.RESCHEDULED.equals(status)) continue;
                    long end = AppointmentRepository.toEpochMillis(a.getDate(), a.getTime());
                    if (end <= 0) continue;
                    int dur = a.getDuration() > 0 ? a.getDuration() : 0;
                    long endEpoch = end + dur * 60000L;
                    if (endEpoch > now) {
                        scheduleAtEnd(context, a.getId(), endEpoch);
                    }
                }
            } catch (Exception ignored) {}
        });
    }
}
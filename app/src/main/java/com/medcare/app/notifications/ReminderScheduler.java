package com.medcare.app.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class ReminderScheduler {
    public static final String CHANNEL_ID = "appointment_reminders";
    private static final String WORK_NAME = "appointment_reminders";

    public static void createChannel(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Appointment reminders",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Reminders before scheduled appointments");
        nm.createNotificationChannel(channel);
    }

    public static void ensureScheduled(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 1, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
}
package com.medcare.app.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.medcare.app.R;
import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.notifications.FollowUpWorker;
import com.medcare.app.notifications.ReminderWorker;
import com.medcare.app.utils.BackupManager;
import com.medcare.app.utils.PreferencesManager;

public class MedCareBackgroundService extends Service {
    public static final String CHANNEL_ID = "background_work";

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(1, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppDatabase.getExecutor().execute(() -> {
            try {
                Context context = getApplicationContext();
                PreferencesManager prefs = new PreferencesManager(context);
                if (prefs.isLoggedIn() && prefs.isBackgroundWorkEnabled()) {
                    BackupManager.maybeRunScheduledBackupSync(context);
                    ReminderWorker.fireDueNow(context);
                    FollowUpWorker.fireDueNow(context, -1);
                    BackgroundScheduler.rescheduleAll(context);
                }
            } catch (Exception ignored) {
            } finally {
                stopSelf();
            }
        });
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.background_work_channel_name),
                NotificationManager.IMPORTANCE_MIN);
        channel.setDescription(getString(R.string.background_work_channel_description));
        nm.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_export)
                .setContentTitle(getString(R.string.background_work_notification_title))
                .setContentText(getString(R.string.background_work_notification_text))
                .setOngoing(true)
                .setSilent(true)
                .build();
    }
}
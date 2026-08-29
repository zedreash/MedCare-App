package com.medcare.app.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.medcare.app.background.BackgroundScheduler;
import com.medcare.app.utils.BackupManager;

import java.util.concurrent.TimeUnit;

public class BackupWorker extends Worker {
    private static final String WORK_NAME = "scheduled_backups";

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void ensureScheduled(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                BackupWorker.class, 1, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        BackupManager.maybeRunScheduledBackupSync(getApplicationContext());
        BackgroundScheduler.rescheduleAll(getApplicationContext());
        return Result.success();
    }
}
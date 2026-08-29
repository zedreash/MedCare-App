package com.medcare.app.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.medcare.app.notifications.BackupWorker;
import com.medcare.app.notifications.FollowUpScheduler;
import com.medcare.app.notifications.ReminderScheduler;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        BackupWorker.ensureScheduled(context);
        ReminderScheduler.ensureScheduled(context);
        FollowUpScheduler.ensureScheduled(context);
        BackgroundScheduler.rescheduleAll(context);
    }
}
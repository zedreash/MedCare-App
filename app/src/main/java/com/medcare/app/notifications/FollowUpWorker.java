package com.medcare.app.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.medcare.app.FollowUpPromptActivity;
import com.medcare.app.R;
import com.medcare.app.background.BackgroundScheduler;
import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.utils.AppointmentStatus;
import com.medcare.app.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FollowUpWorker extends Worker {
    public FollowUpWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long specificId = getInputData().getLong("appointmentId", -1);
        fireDueNow(getApplicationContext(), specificId);
        BackgroundScheduler.rescheduleAll(getApplicationContext());
        return Result.success();
    }

    public static void fireDueNow(Context context, long specificId) {
        PreferencesManager prefs = new PreferencesManager(context);
        long ownerId = prefs.getLoggedInUserId();
        if (ownerId == -1) return;
        if (!ReminderScheduler.canPostNotifications(context)) return;

        try {
            AppDatabase db = AppDatabase.getInstance(context);
            List<Appointment> appointments;
            if (specificId != -1) {
                appointments = new ArrayList<>();
                Appointment a = db.appointmentDao().getAppointmentById(specificId, ownerId);
                if (a != null) appointments.add(a);
            } else {
                appointments = db.appointmentDao().getAllAppointments(ownerId);
            }

            long now = System.currentTimeMillis();
            Set<String> prompted = new HashSet<>(prefs.getFollowUpPromptedIds());
            for (Appointment a : appointments) {
                String status = a.getStatus();
                if (status == null) continue;
                if (!AppointmentStatus.SCHEDULED.equals(status)
                        && !AppointmentStatus.RESCHEDULED.equals(status)) continue;
                long end = endEpoch(a);
                if (end <= 0 || end > now) continue;
                if (now - end > 24 * 60 * 60 * 1000L) continue;
                String key = String.valueOf(a.getId());
                if (specificId == -1 && prompted.contains(key)) continue;
                postFollowUp(context, db, a, ownerId);
                prompted.add(key);
            }
            prefs.setFollowUpPromptedIds(prompted);
        } catch (Exception ignored) {}
    }

    private static long endEpoch(Appointment a) {
        long start = AppointmentRepository.toEpochMillis(a.getDate(), a.getTime());
        if (start <= 0) return -1;
        int dur = a.getDuration() > 0 ? a.getDuration() : 0;
        return start + dur * 60000L;
    }

    private static void postFollowUp(Context context, AppDatabase db, Appointment a, long ownerId) {
        String patientName = "";
        try {
            var patient = db.patientDao().getPatientById(a.getPatientId(), ownerId);
            if (patient != null) patientName = patient.getFullName();
        } catch (Exception ignored) {}

        String title = context.getString(R.string.followup_title);
        String text = (a.getName() != null ? a.getName() : "")
                + (a.getTime() != null ? " \u00B7 " + a.getTime() : "")
                + (patientName != null && !patientName.isEmpty() ? " \u00B7 " + patientName : "");

        int notifId = (int) (a.getId() % Integer.MAX_VALUE);

        Intent open = new Intent(context, FollowUpPromptActivity.class);
        open.putExtra("id", a.getId());
        open.putExtra("ownerId", ownerId);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentPI = PendingIntent.getActivity(context, notifId,
                open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent showedUp = new Intent(context, FollowUpActionReceiver.class)
                .setAction(FollowUpActionReceiver.ACTION_SHOWED_UP)
                .putExtra("id", a.getId())
                .putExtra("ownerId", ownerId);
        PendingIntent showedPI = PendingIntent.getBroadcast(context, notifId * 3 + 1,
                showedUp, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent noShow = new Intent(context, FollowUpActionReceiver.class)
                .setAction(FollowUpActionReceiver.ACTION_NO_SHOW)
                .putExtra("id", a.getId())
                .putExtra("ownerId", ownerId);
        PendingIntent noShowPI = PendingIntent.getBroadcast(context, notifId * 3 + 2,
                noShow, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent askAgain = new Intent(context, FollowUpPromptActivity.class);
        askAgain.putExtra("id", a.getId());
        askAgain.putExtra("ownerId", ownerId);
        askAgain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent askPI = PendingIntent.getActivity(context, notifId * 3 + 3,
                askAgain, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FollowUpScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(contentPI)
                .addAction(R.drawable.ic_check, context.getString(R.string.followup_showed_up), showedPI)
                .addAction(R.drawable.ic_close, context.getString(R.string.followup_no_show), noShowPI)
                .addAction(R.drawable.ic_reschedule, context.getString(R.string.followup_ask_again), askPI);

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(notifId, builder.build());
        }
    }
}
package com.medcare.app.notifications;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.utils.AppointmentStatus;
import com.medcare.app.utils.PreferencesManager;

import java.util.HashSet;
import java.util.Set;

public class FollowUpActionReceiver extends BroadcastReceiver {
    public static final String ACTION_SHOWED_UP = "com.medcare.app.action.SHOWED_UP";
    public static final String ACTION_NO_SHOW = "com.medcare.app.action.NO_SHOW";

    @Override
    public void onReceive(Context context, Intent intent) {
        final long id = intent.getLongExtra("id", -1);
        final long ownerId = intent.getLongExtra("ownerId", -1);
        if (id == -1 || ownerId == -1) return;
        final boolean showedUp = ACTION_SHOWED_UP.equals(intent.getAction());
        final PreferencesManager prefs = new PreferencesManager(context);

        AppointmentRepository repo = new AppointmentRepository(context);
        repo.getAppointmentById(id, ownerId, appointment -> {
            if (appointment == null) {
                clearPrompted(context, prefs, id);
                return;
            }
            appointment.setStatus(showedUp ? AppointmentStatus.COMPLETED : AppointmentStatus.NO_SHOW);
            repo.update(appointment, result -> {
                clearPrompted(context, prefs, id);
            });
        });
    }

    private static void clearPrompted(Context context, PreferencesManager prefs, long id) {
        Set<String> prompted = new HashSet<>(prefs.getFollowUpPromptedIds());
        prompted.remove(String.valueOf(id));
        prefs.setFollowUpPromptedIds(prompted);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.cancel((int) (id % Integer.MAX_VALUE));
        }
    }
}
package com.medcare.app.data.repository;

import android.content.Context;
import android.app.NotificationManager;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.db.AppointmentDao;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.LogEntry;
import com.medcare.app.utils.AuditLogger;
import com.medcare.app.utils.AppointmentStatus;
import com.medcare.app.utils.PreferencesManager;
import com.medcare.app.notifications.FollowUpScheduler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AppointmentRepository {
    private final AppointmentDao appointmentDao;
    private final Context context;

    public interface Callback<T> {
        void onResult(T result);
    }

    public AppointmentRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.appointmentDao = db.appointmentDao();
        this.context = context.getApplicationContext();
    }

    public long insert(Appointment appointment) {
        return appointmentDao.insert(appointment);
    }

    public void insert(Appointment appointment, Callback<Long> callback) {
        AppDatabase.getExecutor().execute(() -> {
            long id = appointmentDao.insert(appointment);
            AuditLogger.log(context, appointment.getOwnerId(), LogEntry.ACTION_CREATE, "appointment", id, appointment.getName());
            AppDatabase.runOnMainThread(() -> callback.onResult(id));
        });
    }

    public void update(Appointment appointment) {
        appointmentDao.update(appointment);
    }

    public void update(Appointment appointment, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            appointmentDao.update(appointment);
            AuditLogger.log(context, appointment.getOwnerId(), LogEntry.ACTION_UPDATE, "appointment", appointment.getId(), appointment.getName());
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    public void delete(Appointment appointment) {
        appointmentDao.delete(appointment);
    }

    public void delete(Appointment appointment, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            appointmentDao.delete(appointment);
            AuditLogger.log(context, appointment.getOwnerId(), LogEntry.ACTION_DELETE, "appointment", appointment.getId(), appointment.getName());
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    public List<Appointment> getAllAppointments(long ownerId) {
        return appointmentDao.getAllAppointments(ownerId);
    }

    public void getAllAppointments(long ownerId, Callback<List<Appointment>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<Appointment> appointments = appointmentDao.getAllAppointments(ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(appointments));
        });
    }

    public Appointment getAppointmentById(long id, long ownerId) {
        return appointmentDao.getAppointmentById(id, ownerId);
    }

    public void getAppointmentById(long id, long ownerId, Callback<Appointment> callback) {
        AppDatabase.getExecutor().execute(() -> {
            Appointment appointment = appointmentDao.getAppointmentById(id, ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(appointment));
        });
    }

    public List<Appointment> getAppointmentsByPatientId(long patientId, long ownerId) {
        return appointmentDao.getAppointmentsByPatientId(patientId, ownerId);
    }

    public void getAppointmentsByPatientId(long patientId, long ownerId, Callback<List<Appointment>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<Appointment> appointments = appointmentDao.getAppointmentsByPatientId(patientId, ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(appointments));
        });
    }

    public List<Appointment> getAppointmentsByDate(String date, long ownerId) {
        return appointmentDao.getAppointmentsByDate(date, ownerId);
    }

    public void getAppointmentsByDate(String date, long ownerId, Callback<List<Appointment>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<Appointment> appointments = appointmentDao.getAppointmentsByDate(date, ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(appointments));
        });
    }

    public int getAppointmentCount(long ownerId) {
        return appointmentDao.getAppointmentCount(ownerId);
    }

    public void getAppointmentCount(long ownerId, Callback<Integer> callback) {
        AppDatabase.getExecutor().execute(() -> {
            int count = appointmentDao.getAppointmentCount(ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(count));
        });
    }

    public int getAppointmentCountByDate(String date, long ownerId) {
        return appointmentDao.getAppointmentCountByDate(date, ownerId);
    }

    public void getAppointmentCountByDate(String date, long ownerId, Callback<Integer> callback) {
        AppDatabase.getExecutor().execute(() -> {
            int count = appointmentDao.getAppointmentCountByDate(date, ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(count));
        });
    }

    public void deleteAllByOwner(long ownerId) {
        appointmentDao.deleteAllByOwner(ownerId);
    }

    public void deleteAllByOwner(long ownerId, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            appointmentDao.deleteAllByOwner(ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    public void deleteByRecurrenceGroup(Long groupId, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            appointmentDao.deleteByRecurrenceGroup(groupId);
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    public List<Appointment> getByRecurrenceGroup(Long groupId) {
        return appointmentDao.getByRecurrenceGroup(groupId);
    }

    public void getByRecurrenceGroup(Long groupId, Callback<List<Appointment>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<Appointment> list = appointmentDao.getByRecurrenceGroup(groupId);
            AppDatabase.runOnMainThread(() -> callback.onResult(list));
        });
    }

    public void rescheduleSeries(Long groupId, long deltaMillis, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<Appointment> list = appointmentDao.getByRecurrenceGroup(groupId);
            for (Appointment a : list) {
                String[] shifted = shiftDateTime(a.getDate(), a.getTime(), deltaMillis);
                if (shifted == null) continue;
                a.setDate(shifted[0]);
                a.setTime(shifted[1]);
                a.setStatus(AppointmentStatus.RESCHEDULED);
                clearReminderState(a.getId());
                scheduleFollowUp(a);
                appointmentDao.update(a);
                AuditLogger.log(context, a.getOwnerId(), LogEntry.ACTION_UPDATE, "appointment", a.getId(), a.getName());
            }
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    private void clearReminderState(long id) {
        try {
            PreferencesManager prefs = new PreferencesManager(context);
            Set<String> prompted = new HashSet<>(prefs.getFollowUpPromptedIds());
            prompted.remove(String.valueOf(id));
            prefs.setFollowUpPromptedIds(prompted);
            Set<String> notified = new HashSet<>(prefs.getNotifiedReminderKeys());
            notified.remove(id + ":" + prefs.getReminderLeadMinutes());
            prefs.setNotifiedReminderKeys(notified);
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.cancel((int) (id % Integer.MAX_VALUE));
            }
        } catch (Exception ignored) {}
    }

    private void scheduleFollowUp(Appointment a) {
        try {
            long end = toEpochMillis(a.getDate(), a.getTime());
            if (end <= 0) return;
            int dur = a.getDuration() > 0 ? a.getDuration() : 0;
            FollowUpScheduler.scheduleAtEnd(context, a.getId(), end + dur * 60000L);
        } catch (Exception ignored) {}
    }

    public static long toEpochMillis(String date, String time) {
        try {
            if (date == null || time == null) return -1;
            String[] d = date.split("/");
            if (d.length != 3) return -1;
            String[] t = time.split(":");
            if (t.length != 2) return -1;
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(d[2]), Integer.parseInt(d[1]) - 1,
                    Integer.parseInt(d[0]), Integer.parseInt(t[0]),
                    Integer.parseInt(t[1]), 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String[] shiftDateTime(String date, String time, long deltaMillis) {
        long epoch = toEpochMillis(date, time);
        if (epoch <= 0) return null;
        epoch += deltaMillis;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epoch);
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return new String[]{df.format(cal.getTime()), tf.format(cal.getTime())};
    }
}

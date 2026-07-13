package com.medcare.app.data.repository;

import android.content.Context;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.db.AppointmentDao;
import com.medcare.app.data.entity.Appointment;

import java.util.List;

public class AppointmentRepository {
    private final AppointmentDao appointmentDao;

    public interface Callback<T> {
        void onResult(T result);
    }

    public AppointmentRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.appointmentDao = db.appointmentDao();
    }

    public long insert(Appointment appointment) {
        return appointmentDao.insert(appointment);
    }

    public void insert(Appointment appointment, Callback<Long> callback) {
        AppDatabase.getExecutor().execute(() -> {
            long id = appointmentDao.insert(appointment);
            AppDatabase.runOnMainThread(() -> callback.onResult(id));
        });
    }

    public void update(Appointment appointment) {
        appointmentDao.update(appointment);
    }

    public void update(Appointment appointment, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            appointmentDao.update(appointment);
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    public void delete(Appointment appointment) {
        appointmentDao.delete(appointment);
    }

    public void delete(Appointment appointment, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            appointmentDao.delete(appointment);
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
}

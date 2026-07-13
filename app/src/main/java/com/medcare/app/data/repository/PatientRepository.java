package com.medcare.app.data.repository;

import android.content.Context;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.db.PatientDao;
import com.medcare.app.data.entity.Patient;

import java.util.List;

public class PatientRepository {
    private final PatientDao patientDao;

    public interface Callback<T> {
        void onResult(T result);
    }

    public PatientRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.patientDao = db.patientDao();
    }

    public long insert(Patient patient) {
        return patientDao.insert(patient);
    }

    public void insert(Patient patient, Callback<Long> callback) {
        AppDatabase.getExecutor().execute(() -> {
            long id = patientDao.insert(patient);
            AppDatabase.runOnMainThread(() -> callback.onResult(id));
        });
    }

    public void update(Patient patient) {
        patientDao.update(patient);
    }

    public void update(Patient patient, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            patientDao.update(patient);
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    public void delete(Patient patient) {
        patientDao.delete(patient);
    }

    public void delete(Patient patient, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            patientDao.delete(patient);
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    public List<Patient> getAllPatients(long ownerId) {
        return patientDao.getAllPatients(ownerId);
    }

    public void getAllPatients(long ownerId, Callback<List<Patient>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<Patient> patients = patientDao.getAllPatients(ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(patients));
        });
    }

    public Patient getPatientById(long id, long ownerId) {
        return patientDao.getPatientById(id, ownerId);
    }

    public void getPatientById(long id, long ownerId, Callback<Patient> callback) {
        AppDatabase.getExecutor().execute(() -> {
            Patient patient = patientDao.getPatientById(id, ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(patient));
        });
    }

    public List<Patient> searchPatients(String query, long ownerId) {
        return patientDao.searchPatients(query, ownerId);
    }

    public void searchPatients(String query, long ownerId, Callback<List<Patient>> callback) {
        AppDatabase.getExecutor().execute(() -> {
            List<Patient> patients = patientDao.searchPatients(query, ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(patients));
        });
    }

    public int getPatientCount(long ownerId) {
        return patientDao.getPatientCount(ownerId);
    }

    public void getPatientCount(long ownerId, Callback<Integer> callback) {
        AppDatabase.getExecutor().execute(() -> {
            int count = patientDao.getPatientCount(ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(count));
        });
    }
}

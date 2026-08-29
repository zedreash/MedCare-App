package com.medcare.app.data.repository;

import android.content.Context;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.db.PatientDao;
import com.medcare.app.data.entity.LogEntry;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.entity.PatientAttachment;
import com.medcare.app.utils.AuditLogger;

import java.io.File;
import java.util.List;

public class PatientRepository {
    private final PatientDao patientDao;
    private final Context context;

    public interface Callback<T> {
        void onResult(T result);
    }

    public PatientRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.patientDao = db.patientDao();
        this.context = context.getApplicationContext();
    }

    public long insert(Patient patient) {
        return patientDao.insert(patient);
    }

    public void insert(Patient patient, Callback<Long> callback) {
        AppDatabase.getExecutor().execute(() -> {
            long id = patientDao.insert(patient);
            AuditLogger.log(context, patient.getOwnerId(), LogEntry.ACTION_CREATE, "patient", id, patient.getFullName());
            AppDatabase.runOnMainThread(() -> callback.onResult(id));
        });
    }

    public void update(Patient patient) {
        patientDao.update(patient);
    }

    public void update(Patient patient, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            patientDao.update(patient);
            AuditLogger.log(context, patient.getOwnerId(), LogEntry.ACTION_UPDATE, "patient", patient.getId(), patient.getFullName());
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    public void delete(Patient patient) {
        patientDao.delete(patient);
    }

    public void delete(Patient patient, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            patientDao.delete(patient);
            AuditLogger.log(context, patient.getOwnerId(), LogEntry.ACTION_DELETE, "patient", patient.getId(), patient.getFullName());
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

    public int getPatientCount(long ownerId) {
        return patientDao.getPatientCount(ownerId);
    }

    public void getPatientCount(long ownerId, Callback<Integer> callback) {
        AppDatabase.getExecutor().execute(() -> {
            int count = patientDao.getPatientCount(ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(count));
        });
    }

    public void deleteAllByOwner(long ownerId) {
        patientDao.deleteAllByOwner(ownerId);
    }

    public void deleteAllByOwner(long ownerId, Callback<Void> callback) {
        AppDatabase.getExecutor().execute(() -> {
            patientDao.deleteAllByOwner(ownerId);
            AppDatabase.runOnMainThread(() -> callback.onResult(null));
        });
    }

    public void deleteAttachmentFilesForOwner(long ownerId) {
        AppDatabase.getExecutor().execute(() -> {
            try {
                List<Patient> patients = patientDao.getAllPatients(ownerId);
                for (Patient p : patients) {
                    for (PatientAttachment a : AppDatabase.getInstance(context)
                            .patientAttachmentDao().getByPatient(p.getId())) {
                        if (a.getFilePath() != null) {
                            File f = new File(a.getFilePath());
                            if (f.exists()) f.delete();
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }
}

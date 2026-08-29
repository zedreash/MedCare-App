package com.medcare.app.data.repository;

import android.content.Context;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.db.PatientAllergyDao;
import com.medcare.app.data.db.PatientAttachmentDao;
import com.medcare.app.data.db.PatientHistoryDao;
import com.medcare.app.data.db.PatientMedicationDao;
import com.medcare.app.data.entity.PatientAllergy;
import com.medcare.app.data.entity.PatientAttachment;
import com.medcare.app.data.entity.PatientHistory;
import com.medcare.app.data.entity.PatientMedication;

import java.util.List;

public class PatientExtrasRepository {
    public interface Callback<T> {
        void onResult(T result);
    }

    private final PatientMedicationDao medicationDao;
    private final PatientAllergyDao allergyDao;
    private final PatientHistoryDao historyDao;
    private final PatientAttachmentDao attachmentDao;

    public PatientExtrasRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        medicationDao = db.patientMedicationDao();
        allergyDao = db.patientAllergyDao();
        historyDao = db.patientHistoryDao();
        attachmentDao = db.patientAttachmentDao();
    }

    public void getMedications(long patientId, Callback<List<PatientMedication>> cb) {
        AppDatabase.getExecutor().execute(() -> {
            List<PatientMedication> list = medicationDao.getByPatient(patientId);
            AppDatabase.runOnMainThread(() -> cb.onResult(list));
        });
    }

    public void insertMedication(PatientMedication m, Callback<Long> cb) {
        AppDatabase.getExecutor().execute(() -> {
            long id = medicationDao.insert(m);
            AppDatabase.runOnMainThread(() -> cb.onResult(id));
        });
    }

    public void updateMedication(PatientMedication m, Callback<Void> cb) {
        AppDatabase.getExecutor().execute(() -> {
            medicationDao.update(m);
            AppDatabase.runOnMainThread(() -> cb.onResult(null));
        });
    }

    public void deleteMedication(PatientMedication m, Callback<Void> cb) {
        AppDatabase.getExecutor().execute(() -> {
            medicationDao.delete(m);
            AppDatabase.runOnMainThread(() -> cb.onResult(null));
        });
    }

    public void getAllergies(long patientId, Callback<List<PatientAllergy>> cb) {
        AppDatabase.getExecutor().execute(() -> {
            List<PatientAllergy> list = allergyDao.getByPatient(patientId);
            AppDatabase.runOnMainThread(() -> cb.onResult(list));
        });
    }

    public void insertAllergy(PatientAllergy a, Callback<Long> cb) {
        AppDatabase.getExecutor().execute(() -> {
            long id = allergyDao.insert(a);
            AppDatabase.runOnMainThread(() -> cb.onResult(id));
        });
    }

    public void updateAllergy(PatientAllergy a, Callback<Void> cb) {
        AppDatabase.getExecutor().execute(() -> {
            allergyDao.update(a);
            AppDatabase.runOnMainThread(() -> cb.onResult(null));
        });
    }

    public void deleteAllergy(PatientAllergy a, Callback<Void> cb) {
        AppDatabase.getExecutor().execute(() -> {
            allergyDao.delete(a);
            AppDatabase.runOnMainThread(() -> cb.onResult(null));
        });
    }

    public void getHistory(long patientId, Callback<List<PatientHistory>> cb) {
        AppDatabase.getExecutor().execute(() -> {
            List<PatientHistory> list = historyDao.getByPatient(patientId);
            AppDatabase.runOnMainThread(() -> cb.onResult(list));
        });
    }

    public void insertHistory(PatientHistory h, Callback<Long> cb) {
        AppDatabase.getExecutor().execute(() -> {
            long id = historyDao.insert(h);
            AppDatabase.runOnMainThread(() -> cb.onResult(id));
        });
    }

    public void updateHistory(PatientHistory h, Callback<Void> cb) {
        AppDatabase.getExecutor().execute(() -> {
            historyDao.update(h);
            AppDatabase.runOnMainThread(() -> cb.onResult(null));
        });
    }

    public void deleteHistory(PatientHistory h, Callback<Void> cb) {
        AppDatabase.getExecutor().execute(() -> {
            historyDao.delete(h);
            AppDatabase.runOnMainThread(() -> cb.onResult(null));
        });
    }

    public void getAttachments(long patientId, Callback<List<PatientAttachment>> cb) {
        AppDatabase.getExecutor().execute(() -> {
            List<PatientAttachment> list = attachmentDao.getByPatient(patientId);
            AppDatabase.runOnMainThread(() -> cb.onResult(list));
        });
    }

    public void insertAttachment(PatientAttachment a, Callback<Long> cb) {
        AppDatabase.getExecutor().execute(() -> {
            long id = attachmentDao.insert(a);
            AppDatabase.runOnMainThread(() -> cb.onResult(id));
        });
    }

    public void deleteAttachment(PatientAttachment a, Callback<Void> cb) {
        AppDatabase.getExecutor().execute(() -> {
            attachmentDao.delete(a);
            AppDatabase.runOnMainThread(() -> cb.onResult(null));
        });
    }
}
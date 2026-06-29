package com.medcare.app.data.repository;
import android.content.Context;
import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.db.PatientDao;
import com.medcare.app.data.entity.Patient;
import java.util.List;
public class PatientRepository {
    private final PatientDao patientDao;
    public PatientRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.patientDao = db.patientDao();
    }
    public long insert(Patient patient) {
        return patientDao.insert(patient);
    }
    public void update(Patient patient) {
        patientDao.update(patient);
    }
    public void delete(Patient patient) {
        patientDao.delete(patient);
    }
    public List<Patient> getAllPatients() {
        return patientDao.getAllPatients();
    }
    public Patient getPatientById(long id) {
        return patientDao.getPatientById(id);
    }
    public List<Patient> searchPatients(String query) {
        return patientDao.searchPatients(query);
    }
}

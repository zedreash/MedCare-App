package com.medcare.app.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.medcare.app.data.entity.PatientMedication;

import java.util.List;

@Dao
public interface PatientMedicationDao {
    @Insert
    long insert(PatientMedication medication);
    @Update
    void update(PatientMedication medication);
    @Delete
    void delete(PatientMedication medication);
    @Query("SELECT * FROM patient_medications WHERE patient_id = :patientId ORDER BY created_at DESC")
    List<PatientMedication> getByPatient(long patientId);
    @Query("SELECT * FROM patient_medications")
    List<PatientMedication> getAll();
    @Query("DELETE FROM patient_medications WHERE patient_id = :patientId")
    void deleteAllByPatient(long patientId);
}
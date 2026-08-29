package com.medcare.app.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.medcare.app.data.entity.PatientAllergy;

import java.util.List;

@Dao
public interface PatientAllergyDao {
    @Insert
    long insert(PatientAllergy allergy);
    @Update
    void update(PatientAllergy allergy);
    @Delete
    void delete(PatientAllergy allergy);
    @Query("SELECT * FROM patient_allergies WHERE patient_id = :patientId ORDER BY created_at DESC")
    List<PatientAllergy> getByPatient(long patientId);
    @Query("SELECT * FROM patient_allergies")
    List<PatientAllergy> getAll();
    @Query("DELETE FROM patient_allergies WHERE patient_id = :patientId")
    void deleteAllByPatient(long patientId);
}
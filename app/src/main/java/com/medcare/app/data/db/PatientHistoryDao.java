package com.medcare.app.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.medcare.app.data.entity.PatientHistory;

import java.util.List;

@Dao
public interface PatientHistoryDao {
    @Insert
    long insert(PatientHistory history);
    @Update
    void update(PatientHistory history);
    @Delete
    void delete(PatientHistory history);
    @Query("SELECT * FROM patient_history WHERE patient_id = :patientId ORDER BY created_at DESC")
    List<PatientHistory> getByPatient(long patientId);
    @Query("SELECT * FROM patient_history")
    List<PatientHistory> getAll();
    @Query("DELETE FROM patient_history WHERE patient_id = :patientId")
    void deleteAllByPatient(long patientId);
}
package com.medcare.app.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.medcare.app.data.entity.PatientAttachment;

import java.util.List;

@Dao
public interface PatientAttachmentDao {
    @Insert
    long insert(PatientAttachment attachment);
    @Delete
    void delete(PatientAttachment attachment);
    @Query("SELECT * FROM patient_attachments WHERE patient_id = :patientId ORDER BY created_at DESC")
    List<PatientAttachment> getByPatient(long patientId);
    @Query("SELECT * FROM patient_attachments")
    List<PatientAttachment> getAll();
    @Query("DELETE FROM patient_attachments WHERE patient_id = :patientId")
    void deleteAllByPatient(long patientId);
}
package com.medcare.app.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.medcare.app.data.entity.Appointment;

import java.util.List;

@Dao
public interface AppointmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Appointment appointment);

    @Update
    void update(Appointment appointment);

    @Delete
    void delete(Appointment appointment);

    @Query("SELECT * FROM appointments ORDER BY date DESC, time DESC")
    List<Appointment> getAllAppointments();

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    Appointment getAppointmentById(long id);

    @Query("SELECT * FROM appointments WHERE patient_id = :patientId ORDER BY date DESC")
    List<Appointment> getAppointmentsByPatientId(long patientId);

    @Query("SELECT * FROM appointments WHERE date = :date ORDER BY time ASC")
    List<Appointment> getAppointmentsByDate(String date);
}

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
    @Query("SELECT * FROM appointments WHERE owner_id = :ownerId ORDER BY date DESC, time DESC")
    List<Appointment> getAllAppointments(long ownerId);
    @Query("SELECT * FROM appointments WHERE id = :id AND owner_id = :ownerId LIMIT 1")
    Appointment getAppointmentById(long id, long ownerId);
    @Query("SELECT * FROM appointments WHERE patient_id = :patientId AND owner_id = :ownerId ORDER BY date DESC")
    List<Appointment> getAppointmentsByPatientId(long patientId, long ownerId);
    @Query("SELECT * FROM appointments WHERE date = :date AND owner_id = :ownerId ORDER BY time ASC")
    List<Appointment> getAppointmentsByDate(String date, long ownerId);
    @Query("SELECT COUNT(*) FROM appointments WHERE owner_id = :ownerId")
    int getAppointmentCount(long ownerId);
    @Query("SELECT COUNT(*) FROM appointments WHERE date = :date AND owner_id = :ownerId")
    int getAppointmentCountByDate(String date, long ownerId);
}

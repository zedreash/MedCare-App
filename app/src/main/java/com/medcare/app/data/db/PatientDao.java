package com.medcare.app.data.db;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.medcare.app.data.entity.Patient;
import java.util.List;
@Dao
public interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Patient patient);
    @Update
    void update(Patient patient);
    @Delete
    void delete(Patient patient);
    @Query("SELECT * FROM patients WHERE owner_id = :ownerId ORDER BY created_at DESC")
    List<Patient> getAllPatients(long ownerId);
    @Query("SELECT * FROM patients WHERE id = :id AND owner_id = :ownerId LIMIT 1")
    Patient getPatientById(long id, long ownerId);
    @Query("SELECT * FROM patients WHERE owner_id = :ownerId AND (full_name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')")
    List<Patient> searchPatients(String query, long ownerId);
    @Query("SELECT COUNT(*) FROM patients WHERE owner_id = :ownerId")
    int getPatientCount(long ownerId);
}

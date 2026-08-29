package com.medcare.app.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.medcare.app.data.entity.LogEntry;

import java.util.List;

@Dao
public interface LogDao {
    @Insert
    long insert(LogEntry entry);
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit")
    List<LogEntry> getRecent(int limit);
    @Query("SELECT * FROM logs WHERE user_id = :userId ORDER BY timestamp DESC")
    List<LogEntry> getByOwner(long userId);
    @Query("DELETE FROM logs WHERE user_id = :userId")
    void deleteByOwner(long userId);
    @Query("DELETE FROM logs")
    void clear();
}
package com.medcare.app.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.medcare.app.data.entity.MetaEntity;

@Dao
public interface MetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void put(MetaEntity entity);
    @Query("SELECT * FROM meta WHERE `key` = :key LIMIT 1")
    MetaEntity get(String key);
}
package com.medcare.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "meta")
public class MetaEntity {
    @NonNull
    @PrimaryKey
    private String key;
    private String value;

    public MetaEntity(@NonNull String key, String value) {
        this.key = key;
        this.value = value;
    }

    @NonNull
    public String getKey() { return key; }
    public void setKey(@NonNull String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
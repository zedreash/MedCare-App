package com.medcare.app.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "patient_history",
        foreignKeys = @ForeignKey(
                entity = Patient.class,
                parentColumns = "id",
                childColumns = "patient_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("patient_id"))
public class PatientHistory {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "patient_id")
    private long patientId;
    private String title;
    private String details;
    private String date;
    @ColumnInfo(name = "created_at")
    private long createdAt;

    public PatientHistory(long patientId, String title, String details, String date, long createdAt) {
        this.patientId = patientId;
        this.title = title;
        this.details = details;
        this.date = date;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPatientId() { return patientId; }
    public void setPatientId(long patientId) { this.patientId = patientId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
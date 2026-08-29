package com.medcare.app.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "patient_medications",
        foreignKeys = @ForeignKey(
                entity = Patient.class,
                parentColumns = "id",
                childColumns = "patient_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("patient_id"))
public class PatientMedication {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "patient_id")
    private long patientId;
    private String name;
    private String dosage;
    private boolean active;
    @ColumnInfo(name = "created_at")
    private long createdAt;

    public PatientMedication(long patientId, String name, String dosage, boolean active, long createdAt) {
        this.patientId = patientId;
        this.name = name;
        this.dosage = dosage;
        this.active = active;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPatientId() { return patientId; }
    public void setPatientId(long patientId) { this.patientId = patientId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
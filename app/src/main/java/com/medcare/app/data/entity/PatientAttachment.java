package com.medcare.app.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "patient_attachments",
        foreignKeys = @ForeignKey(
                entity = Patient.class,
                parentColumns = "id",
                childColumns = "patient_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("patient_id"))
public class PatientAttachment {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "patient_id")
    private long patientId;
    @ColumnInfo(name = "file_path")
    private String filePath;
    private String name;
    private String type;
    private String note;
    @ColumnInfo(name = "created_at")
    private long createdAt;

    public PatientAttachment(long patientId, String filePath, String name, String type, String note, long createdAt) {
        this.patientId = patientId;
        this.filePath = filePath;
        this.name = name;
        this.type = type;
        this.note = note;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPatientId() { return patientId; }
    public void setPatientId(long patientId) { this.patientId = patientId; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
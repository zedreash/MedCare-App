package com.medcare.app.data.entity;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
@Entity(tableName = "appointments",
        foreignKeys = @ForeignKey(
                entity = Patient.class,
                parentColumns = "id",
                childColumns = "patient_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("patient_id"))
public class Appointment {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "patient_id")
    private long patientId;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "date")
    private String date;
    @ColumnInfo(name = "time")
    private String time;
    @ColumnInfo(name = "notes")
    private String notes;
    @ColumnInfo(name = "duration", defaultValue = "30")
    private int duration;
    @ColumnInfo(name = "owner_id")
    private long ownerId;
    @ColumnInfo(name = "created_at")
    private long createdAt;
    public Appointment(long patientId, String name, String date, String time, int duration, String notes, long createdAt) {
        this.patientId = patientId;
        this.name = name;
        this.date = date;
        this.time = time;
        this.duration = duration;
        this.notes = notes;
        this.createdAt = createdAt;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPatientId() { return patientId; }
    public void setPatientId(long patientId) { this.patientId = patientId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

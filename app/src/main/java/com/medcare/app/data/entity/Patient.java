package com.medcare.app.data.entity;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "patients")
public class Patient {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "full_name")
    private String fullName;
    @ColumnInfo(name = "phone")
    private String phone;
    @ColumnInfo(name = "diagnosis")
    private String diagnosis;
    @ColumnInfo(name = "notes")
    private String notes;
    @ColumnInfo(name = "address")
    private String address;
    @ColumnInfo(name = "latitude")
    private double latitude;
    @ColumnInfo(name = "longitude")
    private double longitude;
    @ColumnInfo(name = "created_at")
    private long createdAt;
    public Patient(String fullName, String phone, String diagnosis, String notes, String address, long createdAt) {
        this.fullName = fullName;
        this.phone = phone;
        this.diagnosis = diagnosis;
        this.notes = notes;
        this.address = address;
        this.createdAt = createdAt;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

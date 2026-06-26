package com.medcare.app.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "tz_number")
    private String tzNumber;

    @ColumnInfo(name = "full_name")
    private String fullName;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "phone")
    private String phone;

    @ColumnInfo(name = "date_of_birth")
    private String dateOfBirth;

    @ColumnInfo(name = "password")
    private String password;

    public User(String tzNumber, String fullName, String email, String phone, String dateOfBirth, String password) {
        this.tzNumber = tzNumber;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.password = password;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTzNumber() { return tzNumber; }
    public void setTzNumber(String tzNumber) { this.tzNumber = tzNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

package com.medcare.app.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.entity.User;

@Database(entities = {User.class, Patient.class, Appointment.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract PatientDao patientDao();
    public abstract AppointmentDao appointmentDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "medcare_database"
                    ).allowMainThreadQueries()
                     .fallbackToDestructiveMigration()
                     .build();
                }
            }
        }
        return INSTANCE;
    }
}

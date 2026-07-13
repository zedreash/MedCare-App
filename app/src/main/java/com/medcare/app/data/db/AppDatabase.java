package com.medcare.app.data.db;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.entity.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {User.class, Patient.class, Appointment.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

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
                    ).fallbackToDestructiveMigration()
                     .build();
                }
            }
        }
        return INSTANCE;
    }

    public static ExecutorService getExecutor() {
        return databaseExecutor;
    }

    public static void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}

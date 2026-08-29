package com.medcare.app.data.db;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.LogEntry;
import com.medcare.app.data.entity.MetaEntity;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.entity.PatientAllergy;
import com.medcare.app.data.entity.PatientAttachment;
import com.medcare.app.data.entity.PatientHistory;
import com.medcare.app.data.entity.PatientMedication;
import com.medcare.app.data.entity.User;
import com.medcare.app.utils.DeviceSecret;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {User.class, Patient.class, Appointment.class,
        PatientMedication.class, PatientAllergy.class, PatientHistory.class,
        PatientAttachment.class, LogEntry.class, MetaEntity.class},
        version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public abstract UserDao userDao();
    public abstract PatientDao patientDao();
    public abstract AppointmentDao appointmentDao();
    public abstract PatientMedicationDao patientMedicationDao();
    public abstract PatientAllergyDao patientAllergyDao();
    public abstract PatientHistoryDao patientHistoryDao();
    public abstract PatientAttachmentDao patientAttachmentDao();
    public abstract LogDao logDao();
    public abstract MetaDao metaDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    System.loadLibrary("sqlcipher");
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "medcare_database"
                    ).openHelperFactory(new SupportOpenHelperFactory(
                            DeviceSecret.getOrCreate(context.getApplicationContext())))
                     .build();
                }
            }
        }
        return INSTANCE;
    }

    public static ExecutorService getExecutor() {
        return databaseExecutor;
    }

    public static void ensureUsable(Context context) {
        final Context appContext = context.getApplicationContext();
        databaseExecutor.execute(() -> {
            try {
                SupportSQLiteDatabase db = getInstance(appContext).getOpenHelper().getWritableDatabase();
                db.query("SELECT 1").close();
            } catch (Throwable ignored) {
                try {
                    synchronized (AppDatabase.class) {
                        if (INSTANCE != null) {
                            INSTANCE.close();
                            INSTANCE = null;
                        }
                    }
                } catch (Throwable ignored2) {}
                appContext.deleteDatabase("medcare_database");
                try {
                    appContext.getSharedPreferences("medcare_secure", Context.MODE_PRIVATE)
                            .edit().clear().commit();
                } catch (Throwable ignored3) {}
            }
        });
    }

    public static void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    public static void closeAndResetInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}

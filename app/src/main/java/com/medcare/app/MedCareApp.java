package com.medcare.app;
import android.app.Application;
import com.medcare.app.data.db.AppDatabase;
public class MedCareApp extends Application {
    private AppDatabase database;
    @Override
    public void onCreate() {
        super.onCreate();
        database = AppDatabase.getInstance(this);
    }
    public AppDatabase getDatabase() {
        return database;
    }
}

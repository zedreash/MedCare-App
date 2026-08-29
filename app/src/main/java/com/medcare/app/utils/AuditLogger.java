package com.medcare.app.utils;

import android.content.Context;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.entity.LogEntry;

public final class AuditLogger {
    private AuditLogger() {}

    public static void log(Context context, long userId, String action,
                           String entityType, Long entityId, String detail) {
        AppDatabase.getExecutor().execute(() -> {
            try {
                AppDatabase.getInstance(context).logDao().insert(
                        new LogEntry(System.currentTimeMillis(), userId,
                                action, entityType, entityId, detail));
            } catch (Exception ignored) {
            }
        });
    }
}
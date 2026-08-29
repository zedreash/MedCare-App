package com.medcare.app.utils;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class BackupStorage {
    public static final String FOLDER = "Documents/MedCareBackups";

    public static class BackupFile {
        public String name;
        public Uri uri;
        public File file;
        public long dateMillis;
        public long size;
        public String email;
    }

    private BackupStorage() {}

    public static OutputStream openWrite(Context context, String name) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, DataTransfer.MIME_TYPE);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER);
            Uri uri = context.getContentResolver().insert(
                    MediaStore.Files.getContentUri("external"), values);
            if (uri == null) throw new IOException("Could not create backup file");
            OutputStream out = context.getContentResolver().openOutputStream(uri, "w");
            if (out == null) throw new IOException("Could not open backup file");
            return out;
        } else {
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "MedCareBackups");
            if (!dir.exists()) dir.mkdirs();
            return new FileOutputStream(new File(dir, name));
        }
    }

    public static List<BackupFile> list(Context context) {
        List<BackupFile> result = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 29) {
            Uri collection = MediaStore.Files.getContentUri("external");
            String[] projection = {
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATE_ADDED,
                    MediaStore.MediaColumns.SIZE
            };
            String selection = MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ? AND "
                    + MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ?";
            String[] args = {FOLDER + "%", "%.medcare"};
            try (Cursor cursor = context.getContentResolver().query(
                    collection, projection, selection, args,
                    MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        BackupFile b = new BackupFile();
                        long id = cursor.getLong(0);
                        b.name = cursor.getString(1);
                        b.dateMillis = cursor.getLong(2) * 1000L;
                        b.size = cursor.getLong(3);
                        b.uri = ContentUris.withAppendedId(collection, id);
                        result.add(b);
                    }
                }
            }
        } else {
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "MedCareBackups");
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".medcare")) {
                        BackupFile b = new BackupFile();
                        b.name = f.getName();
                        b.file = f;
                        b.dateMillis = f.lastModified();
                        b.size = f.length();
                        result.add(b);
                    }
                }
            }
            Collections.sort(result, (a, b) -> Long.compare(b.dateMillis, a.dateMillis));
        }
        return result;
    }

    public static InputStream openRead(Context context, BackupFile backup) throws IOException {
        if (backup.uri != null) {
            InputStream in = context.getContentResolver().openInputStream(backup.uri);
            if (in == null) throw new IOException("Could not open backup");
            return in;
        }
        if (backup.file != null) {
            return new FileInputStream(backup.file);
        }
        throw new IOException("Invalid backup");
    }

    public static boolean delete(Context context, BackupFile backup) {
        if (backup.uri != null) {
            return context.getContentResolver().delete(backup.uri, null, null) > 0;
        }
        return backup.file != null && backup.file.delete();
    }
}
package com.medcare.app.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.medcare.app.data.db.AppDatabase;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class BackupManager {
    public static final int RETENTION_LIMIT = 30;
    private static final byte[] MAGIC = "MEDBACKUP".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final int FORMAT_VERSION_V1 = 1;
    private static final int FORMAT_VERSION_V2 = 2;

    public interface Callback {
        void onDone(boolean success);
    }

    public interface RestoreCallback {
        void onDone(DataTransfer.ImportResult result);
    }

    private BackupManager() {}

    public static boolean backupNowSync(Context context) {
        try {
            PreferencesManager prefs = new PreferencesManager(context);
            String passphrase = prefs.getBackupPassword();
            if (passphrase == null || passphrase.isEmpty()) return false;

            JSONObject snapshot = DataTransfer.buildSnapshot(context);
            String email = snapshot.optJSONObject("user").optString("email", null);
            byte[] inner = DataTransfer.encrypt(snapshot.toString().getBytes("UTF-8"), passphrase);
            byte[] emailBytes = (email == null ? "" : email).getBytes("UTF-8");
            ByteBuffer buf = ByteBuffer.allocate(MAGIC.length + 4 + 4 + emailBytes.length + inner.length);
            buf.put(MAGIC);
            buf.putInt(FORMAT_VERSION_V2);
            buf.putInt(emailBytes.length);
            buf.put(emailBytes);
            buf.put(inner);

            String safeEmail = sanitizeEmail(email);
            String name = "MedCareBackup"
                    + (safeEmail != null && !safeEmail.isEmpty() ? "-" + safeEmail : "")
                    + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date())
                    + DataTransfer.FILE_EXTENSION;
            OutputStream out = BackupStorage.openWrite(context, name);
            out.write(buf.array());
            out.flush();
            out.close();

            prefs.setLastBackupTime(System.currentTimeMillis());
            prune(context);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String sanitizeEmail(String email) {
        if (email == null) return null;
        String s = email.trim().toLowerCase().replaceAll("[^a-zA-Z0-9.@_-]", "_");
        return s.isEmpty() ? null : s;
    }

    public static void backupNow(Context context, Callback callback) {
        AppDatabase.getExecutor().execute(() -> {
            boolean ok = backupNowSync(context);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) callback.onDone(ok);
            });
        });
    }

    public static void maybeRunScheduledBackupSync(Context context) {
        PreferencesManager prefs = new PreferencesManager(context);
        if (!prefs.isLoggedIn() || !prefs.hasBackupPassword()) return;
        long periodMs = periodMillis(prefs.getBackupFrequency());
        if (periodMs <= 0) return;
        if (System.currentTimeMillis() - prefs.getLastBackupTime() < periodMs) return;
        backupNowSync(context);
    }

    public static void restore(Context context, BackupStorage.BackupFile backup, RestoreCallback callback) {
        restore(context, backup, null, callback);
    }

    public static void restore(Context context, BackupStorage.BackupFile backup, String passphrase,
                               RestoreCallback callback) {
        AppDatabase.getExecutor().execute(() -> {
            DataTransfer.ImportResult result = null;
            try {
                byte[] data = readBackupFile(context, backup);
                ByteBuffer buf = ByteBuffer.wrap(data);
                byte[] readMagic = new byte[MAGIC.length];
                buf.get(readMagic);
                if (!Arrays.equals(MAGIC, readMagic)) throw new IllegalArgumentException("bad file");
                int version = buf.getInt();
                JSONObject root;
                if (version == FORMAT_VERSION_V1) {
                    byte[] enc = new byte[buf.remaining()];
                    buf.get(enc);
                    byte[] plain = DeviceSecret.decrypt(context, enc);
                    root = new JSONObject(new String(plain, "UTF-8"));
                } else if (version == FORMAT_VERSION_V2) {
                    int emailLen = buf.getInt();
                    byte[] emailBytes = new byte[emailLen];
                    buf.get(emailBytes);
                    byte[] inner = new byte[buf.remaining()];
                    buf.get(inner);
                    String p = passphrase != null && !passphrase.isEmpty() ? passphrase
                            : new PreferencesManager(context).getBackupPassword();
                    if (p == null || p.isEmpty()) throw new IllegalArgumentException("passphrase required");
                    byte[] plain = DataTransfer.decrypt(inner, p);
                    root = new JSONObject(new String(plain, "UTF-8"));
                } else {
                    throw new IllegalArgumentException("bad version");
                }
                result = DataTransfer.restoreSnapshot(context, root);
                if (result != null) {
                    new PreferencesManager(context).setLastRestoreTime(System.currentTimeMillis());
                    if (passphrase != null && !passphrase.isEmpty()
                            && !new PreferencesManager(context).hasBackupPassword()) {
                        new PreferencesManager(context).setBackupPassword(passphrase);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                final DataTransfer.ImportResult finalResult = result;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onDone(finalResult);
                });
            }
        });
    }

    public static List<BackupStorage.BackupFile> list(Context context) {
        return BackupStorage.list(context);
    }

    public static long periodMillis(String frequency) {
        switch (frequency == null ? "off" : frequency) {
            case "hourly": return 60L * 60 * 1000;
            case "daily": return 24L * 60 * 60 * 1000;
            case "weekly": return 7L * 24 * 60 * 60 * 1000;
            case "monthly": return 30L * 24 * 60 * 60 * 1000;
            case "yearly": return 365L * 24 * 60 * 60 * 1000;
            default: return 0;
        }
    }

    public static void delete(Context context, BackupStorage.BackupFile backup) {
        BackupStorage.delete(context, backup);
    }

    public static void deleteAll(Context context) {
        for (BackupStorage.BackupFile b : BackupStorage.list(context)) {
            BackupStorage.delete(context, b);
        }
    }

    public static String getAccountEmail(Context context, BackupStorage.BackupFile backup) {
        try {
            byte[] data = readBackupFile(context, backup);
            ByteBuffer buf = ByteBuffer.wrap(data);
            byte[] readMagic = new byte[MAGIC.length];
            buf.get(readMagic);
            if (!Arrays.equals(MAGIC, readMagic)) return null;
            int version = buf.getInt();
            if (version == FORMAT_VERSION_V2) {
                int emailLen = buf.getInt();
                byte[] emailBytes = new byte[emailLen];
                buf.get(emailBytes);
                String email = new String(emailBytes, "UTF-8");
                return email.isEmpty() ? null : email;
            } else if (version == FORMAT_VERSION_V1) {
                byte[] enc = new byte[buf.remaining()];
                buf.get(enc);
                byte[] plain = DeviceSecret.decrypt(context, enc);
                JSONObject root = new JSONObject(new String(plain, "UTF-8"));
                JSONObject userObj = root.optJSONObject("user");
                return userObj != null ? userObj.optString("email", null) : null;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void withAccountEmails(Context context, List<BackupStorage.BackupFile> backups) {
        for (BackupStorage.BackupFile b : backups) {
            b.email = getAccountEmail(context, b);
        }
    }

    private static byte[] readBackupFile(Context context, BackupStorage.BackupFile backup) throws Exception {
        InputStream in = BackupStorage.openRead(context, backup);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            bos.write(buffer, 0, n);
        }
        in.close();
        return bos.toByteArray();
    }

    public static void reencryptAll(Context context, String oldPassphrase, String newPassphrase,
                                    ReencryptCallback onDone) {
        AppDatabase.getExecutor().execute(() -> {
            int done = 0;
            List<BackupStorage.BackupFile> backups = BackupStorage.list(context);
            for (BackupStorage.BackupFile backup : backups) {
                try {
                    if (reencryptOne(context, backup, oldPassphrase, newPassphrase)) {
                        done++;
                    }
                } catch (Exception ignored) {
                }
            }
            final int count = done;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.onDone(count);
            });
        });
    }

    private static boolean reencryptOne(Context context, BackupStorage.BackupFile backup,
                                        String oldPassphrase, String newPassphrase) throws Exception {
        byte[] data = readBackupFile(context, backup);
        ByteBuffer buf = ByteBuffer.wrap(data);
        byte[] readMagic = new byte[MAGIC.length];
        buf.get(readMagic);
        if (!Arrays.equals(MAGIC, readMagic)) return false;
        int version = buf.getInt();
        JSONObject root;
        String email;
        if (version == FORMAT_VERSION_V2) {
            int emailLen = buf.getInt();
            byte[] emailBytes = new byte[emailLen];
            buf.get(emailBytes);
            email = new String(emailBytes, "UTF-8");
            byte[] inner = new byte[buf.remaining()];
            buf.get(inner);
            if (oldPassphrase == null || oldPassphrase.isEmpty()) return false;
            root = new JSONObject(new String(DataTransfer.decrypt(inner, oldPassphrase), "UTF-8"));
        } else if (version == FORMAT_VERSION_V1) {
            byte[] enc = new byte[buf.remaining()];
            buf.get(enc);
            root = new JSONObject(new String(DeviceSecret.decrypt(context, enc), "UTF-8"));
            JSONObject userObj = root.optJSONObject("user");
            email = userObj != null ? userObj.optString("email", null) : null;
        } else {
            return false;
        }

        byte[] inner = DataTransfer.encrypt(root.toString().getBytes("UTF-8"), newPassphrase);
        byte[] emailBytes = (email == null ? "" : email).getBytes("UTF-8");
        ByteBuffer out = ByteBuffer.allocate(MAGIC.length + 4 + 4 + emailBytes.length + inner.length);
        out.put(MAGIC);
        out.putInt(FORMAT_VERSION_V2);
        out.putInt(emailBytes.length);
        out.put(emailBytes);
        out.put(inner);

        if (backup.uri != null) {
            OutputStream os = context.getContentResolver().openOutputStream(backup.uri, "w");
            if (os == null) return false;
            os.write(out.array());
            os.flush();
            os.close();
        } else if (backup.file != null) {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(backup.file);
            fos.write(out.array());
            fos.flush();
            fos.close();
        } else {
            return false;
        }
        return true;
    }

    public interface ReencryptCallback {
        void onDone(int count);
    }

    private static void prune(Context context) {
        List<BackupStorage.BackupFile> backups = BackupStorage.list(context);
        for (int i = RETENTION_LIMIT; i < backups.size(); i++) {
            BackupStorage.delete(context, backups.get(i));
        }
    }
}
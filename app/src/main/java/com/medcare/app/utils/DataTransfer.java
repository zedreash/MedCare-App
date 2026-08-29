package com.medcare.app.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.LogEntry;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.entity.PatientAllergy;
import com.medcare.app.data.entity.PatientAttachment;
import com.medcare.app.data.entity.PatientHistory;
import com.medcare.app.data.entity.PatientMedication;
import com.medcare.app.data.entity.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class DataTransfer {
    public static final String MIME_TYPE = "application/x-medcare";
    public static final String FILE_EXTENSION = ".medcare";

    public interface Callback {
        void onDone(boolean success);
    }

    public static class ImportResult {
        public final long userId;
        public final boolean biometricEnabled;
        public final boolean needsPassword;
        public ImportResult(long userId, boolean biometricEnabled, boolean needsPassword) {
            this.userId = userId;
            this.biometricEnabled = biometricEnabled;
            this.needsPassword = needsPassword;
        }
    }

    private static final String MAGIC = "MEDCARE";
    private static final int FORMAT_VERSION = 2;
    private static final int PBKDF2_ITERATIONS = 120000;

    public static void exportData(Context context, String password, Uri uri, Callback callback) {
        AppDatabase.getExecutor().execute(() -> {
            boolean success = false;
            try {
                JSONObject root = buildSnapshot(context);
                byte[] plain = root.toString().getBytes("UTF-8");
                byte[] encrypted = encrypt(plain, password);

                OutputStream out = context.getContentResolver().openOutputStream(uri, "w");
                if (out != null) {
                    out.write(encrypted);
                    out.flush();
                    out.close();
                }
                logExport(context, true);
                success = true;
            } catch (Exception ignored) {
                logExport(context, false);
            } finally {
                final boolean result = success;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onDone(result);
                });
            }
        });
    }

    public static void importData(Context context, Uri uri, String password, Callback2 callback) {
        AppDatabase.getExecutor().execute(() -> {
            ImportResult result = null;
            try {
                byte[] encrypted = readUri(context, uri);
                byte[] plain = decrypt(encrypted, password);
                if (plain == null) {
                    throw new IllegalArgumentException("bad password or file");
                }
                JSONObject root = new JSONObject(new String(plain, "UTF-8"));
                if (!MAGIC.equals(root.optString("format"))) {
                    throw new IllegalArgumentException("bad format");
                }
                result = restoreSnapshot(context, root);
                if (result != null && result.userId != -1) {
                    PreferencesManager prefs = new PreferencesManager(context);
                    if (!prefs.hasBackupPassword() && password != null && !password.isEmpty()) {
                        prefs.setBackupPassword(password);
                    }
                }
            } catch (Exception ignored) {
                logImport(context, false, -1);
            } finally {
                final ImportResult finalResult = result;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onDone(finalResult);
                });
            }
        });
    }

    public static JSONObject buildSnapshot(Context context) throws Exception {
        AppDatabase db = AppDatabase.getInstance(context);
        PreferencesManager prefs = new PreferencesManager(context);
        long ownerId = prefs.getLoggedInUserId();
        if (ownerId == -1) throw new IllegalStateException("not logged in");

        JSONObject root = new JSONObject();
        root.put("format", MAGIC);
        root.put("version", FORMAT_VERSION);
        root.put("createdAt", System.currentTimeMillis());

        User u = db.userDao().getUserById(ownerId);
        JSONObject userObj = new JSONObject();
        if (u != null) {
            userObj.put("id", u.getId());
            userObj.put("tz", u.getTzNumber());
            userObj.put("name", u.getFullName());
            userObj.put("email", u.getEmail());
            userObj.put("phone", u.getPhone());
            userObj.put("dob", u.getDateOfBirth());
            userObj.put("clinic", u.getClinic());
            userObj.put("clinicLat", u.getClinicLat());
            userObj.put("clinicLng", u.getClinicLng());
        }
        root.put("user", userObj);
        root.put("patients", patientsToJson(db, ownerId));
        root.put("appointments", appointmentsToJson(db, ownerId));
        root.put("medications", medicationsToJson(db, ownerId));
        root.put("allergies", allergiesToJson(db, ownerId));
        root.put("history", historyToJson(db, ownerId));
        root.put("attachments", attachmentsToJson(db, ownerId));
        root.put("settings", settingsToJson(context, ownerId));
        root.put("userThemes", userThemesToJson(context, db, ownerId));
        root.put("logs", logsToJson(db, ownerId));
        return root;
    }

    public static ImportResult restoreSnapshot(Context context, JSONObject root) throws Exception {
        if (!MAGIC.equals(root.optString("format"))) {
            throw new IllegalArgumentException("bad format");
        }
        PreferencesManager prefs = new PreferencesManager(context);
        AppDatabase db = AppDatabase.getInstance(context);

        long ownerId = prefs.getLoggedInUserId();
        boolean needsPassword = false;
        if (ownerId == -1) {
            JSONObject userObj = root.optJSONObject("user");
            String email = userObj != null ? optString(userObj, "email") : null;
            User existing = email != null ? db.userDao().getUserByEmail(email) : null;
            if (existing != null) {
                ownerId = existing.getId();
                String hash = existing.getPassword();
                if (hash == null || hash.isEmpty()) {
                    needsPassword = true;
                }
            } else if (userObj != null) {
                User created = new User(
                        optString(userObj, "tz"),
                        optString(userObj, "name"),
                        email,
                        optString(userObj, "phone"),
                        optString(userObj, "dob"),
                        null);
                created.setClinic(optString(userObj, "clinic"));
                if (userObj.has("clinicLat") && !userObj.isNull("clinicLat")) {
                    created.setClinicLat(userObj.getDouble("clinicLat"));
                }
                if (userObj.has("clinicLng") && !userObj.isNull("clinicLng")) {
                    created.setClinicLng(userObj.getDouble("clinicLng"));
                }
                ownerId = db.userDao().insert(created);
                needsPassword = true;
            } else {
                throw new IllegalStateException("no account to restore into");
            }
        }

        final long targetOwner = ownerId;
        final List<File> writtenFiles = new ArrayList<>();
        final List<String> oldFiles = new ArrayList<>();
        try {
            db.runInTransaction(() -> {
                try {
                    List<Patient> oldPatients = db.patientDao().getAllPatients(targetOwner);
                    for (Patient p : oldPatients) {
                        for (PatientAttachment a : db.patientAttachmentDao().getByPatient(p.getId())) {
                            if (a.getFilePath() != null) oldFiles.add(a.getFilePath());
                        }
                    }
                    db.patientDao().deleteAllByOwner(targetOwner);
                    db.logDao().deleteByOwner(targetOwner);

                    Map<Long, Long> patientIdMap = new HashMap<>();
                    JSONArray patients = root.optJSONArray("patients");
                    if (patients != null) {
                        for (int i = 0; i < patients.length(); i++) {
                            JSONObject o = patients.getJSONObject(i);
                            long oldId = o.optLong("id", -1);
                            Patient p = new Patient(
                                    optString(o, "name"),
                                    optString(o, "phone"),
                                    optString(o, "diagnosis"),
                                    optString(o, "notes"),
                                    optString(o, "address"),
                                    o.optLong("createdAt", System.currentTimeMillis()));
                            p.setLatitude(o.optDouble("lat", 0));
                            p.setLongitude(o.optDouble("lng", 0));
                            p.setOwnerId(targetOwner);
                            p.setBloodType(optString(o, "bloodType"));
                            p.setHeightCm(o.has("height") && !o.isNull("height") ? o.getInt("height") : null);
                            p.setWeightKg(o.has("weight") && !o.isNull("weight") ? o.getDouble("weight") : null);
                            long newId = db.patientDao().insert(p);
                            patientIdMap.put(oldId, newId);
                        }
                    }
                    restoreAppointments(db, root.optJSONArray("appointments"), patientIdMap, targetOwner);
                    restoreMedications(db, root.optJSONArray("medications"), patientIdMap);
                    restoreAllergies(db, root.optJSONArray("allergies"), patientIdMap);
                    restoreHistory(db, root.optJSONArray("history"), patientIdMap);
                    restoreAttachments(context, db, root.optJSONArray("attachments"), patientIdMap, writtenFiles);
                    restoreLogs(db, root.optJSONArray("logs"), targetOwner);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            for (File f : writtenFiles) {
                if (f != null) f.delete();
            }
            throw new Exception(e.getCause() != null ? e.getCause() : e);
        }
        for (String path : oldFiles) {
            File f = new File(path);
            if (f.exists()) f.delete();
        }

        restoreSettings(context, root.optJSONObject("settings"), targetOwner);
        restoreUserThemes(context, root.optJSONArray("userThemes"), targetOwner);
        logImport(context, true, targetOwner);
        return new ImportResult(targetOwner, false, needsPassword);
    }

    public interface Callback2 {
        void onDone(ImportResult result);
    }

    private static JSONArray patientsToJson(AppDatabase db, long ownerId) throws Exception {
        JSONArray arr = new JSONArray();
        for (Patient p : db.patientDao().getAllPatients(ownerId)) {
            JSONObject o = new JSONObject();
            o.put("id", p.getId());
            o.put("name", p.getFullName());
            o.put("phone", p.getPhone());
            o.put("diagnosis", p.getDiagnosis());
            o.put("notes", p.getNotes());
            o.put("address", p.getAddress());
            o.put("lat", p.getLatitude());
            o.put("lng", p.getLongitude());
            o.put("createdAt", p.getCreatedAt());
            o.put("bloodType", p.getBloodType());
            o.put("height", p.getHeightCm());
            o.put("weight", p.getWeightKg());
            arr.put(o);
        }
        return arr;
    }

    private static JSONArray appointmentsToJson(AppDatabase db, long ownerId) throws Exception {
        JSONArray arr = new JSONArray();
        for (Appointment a : db.appointmentDao().getAllAppointments(ownerId)) {
            JSONObject o = new JSONObject();
            o.put("patientId", a.getPatientId());
            o.put("name", a.getName());
            o.put("date", a.getDate());
            o.put("time", a.getTime());
            o.put("notes", a.getNotes());
            o.put("duration", a.getDuration());
            o.put("createdAt", a.getCreatedAt());
            o.put("status", a.getStatus());
            o.put("recurrenceRule", a.getRecurrenceRule());
            o.put("recurrenceGroupId", a.getRecurrenceGroupId());
            arr.put(o);
        }
        return arr;
    }

    private static JSONArray medicationsToJson(AppDatabase db, long ownerId) throws Exception {
        JSONArray arr = new JSONArray();
        for (Patient p : db.patientDao().getAllPatients(ownerId)) {
            for (PatientMedication m : db.patientMedicationDao().getByPatient(p.getId())) {
                JSONObject o = new JSONObject();
                o.put("patientId", m.getPatientId());
                o.put("name", m.getName());
                o.put("dosage", m.getDosage());
                o.put("active", m.isActive());
                o.put("createdAt", m.getCreatedAt());
                arr.put(o);
            }
        }
        return arr;
    }

    private static JSONArray allergiesToJson(AppDatabase db, long ownerId) throws Exception {
        JSONArray arr = new JSONArray();
        for (Patient p : db.patientDao().getAllPatients(ownerId)) {
            for (PatientAllergy a : db.patientAllergyDao().getByPatient(p.getId())) {
                JSONObject o = new JSONObject();
                o.put("patientId", a.getPatientId());
                o.put("name", a.getName());
                o.put("note", a.getNote());
                o.put("createdAt", a.getCreatedAt());
                arr.put(o);
            }
        }
        return arr;
    }

    private static JSONArray historyToJson(AppDatabase db, long ownerId) throws Exception {
        JSONArray arr = new JSONArray();
        for (Patient p : db.patientDao().getAllPatients(ownerId)) {
            for (PatientHistory h : db.patientHistoryDao().getByPatient(p.getId())) {
                JSONObject o = new JSONObject();
                o.put("patientId", h.getPatientId());
                o.put("title", h.getTitle());
                o.put("details", h.getDetails());
                o.put("date", h.getDate());
                o.put("createdAt", h.getCreatedAt());
                arr.put(o);
            }
        }
        return arr;
    }

    private static JSONArray attachmentsToJson(AppDatabase db, long ownerId) throws Exception {
        JSONArray arr = new JSONArray();
        for (Patient p : db.patientDao().getAllPatients(ownerId)) {
            for (PatientAttachment a : db.patientAttachmentDao().getByPatient(p.getId())) {
                JSONObject o = new JSONObject();
                o.put("patientId", a.getPatientId());
                o.put("name", a.getName());
                o.put("type", a.getType());
                o.put("note", a.getNote());
                o.put("createdAt", a.getCreatedAt());
                String path = a.getFilePath();
                if (path != null) {
                    File f = new File(path);
                    if (f.exists()) {
                        o.put("file", android.util.Base64.encodeToString(readFile(f), android.util.Base64.NO_WRAP));
                    }
                }
                arr.put(o);
            }
        }
        return arr;
    }

    private static JSONArray logsToJson(AppDatabase db, long ownerId) throws Exception {
        JSONArray arr = new JSONArray();
        for (LogEntry e : db.logDao().getByOwner(ownerId)) {
            JSONObject o = new JSONObject();
            o.put("timestamp", e.getTimestamp());
            o.put("action", e.getAction());
            o.put("entityType", e.getEntityType());
            o.put("entityId", e.getEntityId());
            o.put("detail", e.getDetail());
            arr.put(o);
        }
        return arr;
    }

    private static JSONObject settingsToJson(Context context, long ownerId) throws Exception {
        PreferencesManager prefs = new PreferencesManager(context);
        JSONObject o = new JSONObject();
        o.put("themeMode", prefs.getThemeMode());
        o.put("themeStyle", prefs.getThemeStyle());
        o.put("patientSort", prefs.getPatientSortMode(0));
        o.put("appointmentSort", prefs.getAppointmentSortMode(0));
        o.put("duration", prefs.getDefaultAppointmentDuration());
        o.put("backupFrequency", prefs.getBackupFrequency());
        String photo = prefs.getProfilePhotoPathForUser(ownerId);
        if (photo != null) {
            File f = new File(photo);
            if (f.exists()) {
                o.put("profilePhoto", android.util.Base64.encodeToString(readFile(f), android.util.Base64.NO_WRAP));
            }
        }
        return o;
    }

    private static JSONArray userThemesToJson(Context context, AppDatabase db, long ownerId) throws Exception {
        PreferencesManager prefs = new PreferencesManager(context);
        JSONArray arr = new JSONArray();
        JSONObject o = new JSONObject();
        o.put("userId", ownerId);
        String mode = prefs.getThemeModeForUser(ownerId);
        String style = prefs.getThemeStyleForUser(ownerId);
        if (mode != null) o.put("mode", mode);
        if (style != null) o.put("style", style);
        arr.put(o);
        return arr;
    }

    private static void restoreAppointments(AppDatabase db, JSONArray appointments,
                                            Map<Long, Long> patientMap, long ownerId) throws Exception {
        if (appointments == null) return;
        for (int i = 0; i < appointments.length(); i++) {
            JSONObject o = appointments.getJSONObject(i);
            long oldPid = o.optLong("patientId", -1);
            long newPid = patientMap.containsKey(oldPid) ? patientMap.get(oldPid) : -1L;
            Appointment a = new Appointment(
                    newPid,
                    optString(o, "name"),
                    optString(o, "date"),
                    optString(o, "time"),
                    o.optInt("duration", 30),
                    optString(o, "notes"),
                    o.optLong("createdAt", System.currentTimeMillis()));
            a.setOwnerId(ownerId);
            a.setStatus(o.optString("status", AppointmentStatus.SCHEDULED));
            if (o.has("recurrenceRule") && !o.isNull("recurrenceRule")) a.setRecurrenceRule(o.getString("recurrenceRule"));
            if (o.has("recurrenceGroupId") && !o.isNull("recurrenceGroupId")) a.setRecurrenceGroupId(o.getLong("recurrenceGroupId"));
            db.appointmentDao().insert(a);
        }
    }

    private static void restoreMedications(AppDatabase db, JSONArray items,
                                          Map<Long, Long> patientMap) throws Exception {
        if (items == null) return;
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.getJSONObject(i);
            long oldPid = o.optLong("patientId", -1);
            long newPid = patientMap.containsKey(oldPid) ? patientMap.get(oldPid) : -1L;
            db.patientMedicationDao().insert(new PatientMedication(
                    newPid, optString(o, "name"), optString(o, "dosage"),
                    o.optBoolean("active", true),
                    o.optLong("createdAt", System.currentTimeMillis())));
        }
    }

    private static void restoreAllergies(AppDatabase db, JSONArray items,
                                        Map<Long, Long> patientMap) throws Exception {
        if (items == null) return;
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.getJSONObject(i);
            long oldPid = o.optLong("patientId", -1);
            long newPid = patientMap.containsKey(oldPid) ? patientMap.get(oldPid) : -1L;
            db.patientAllergyDao().insert(new PatientAllergy(
                    newPid, optString(o, "name"), optString(o, "note"),
                    o.optLong("createdAt", System.currentTimeMillis())));
        }
    }

    private static void restoreHistory(AppDatabase db, JSONArray items,
                                      Map<Long, Long> patientMap) throws Exception {
        if (items == null) return;
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.getJSONObject(i);
            long oldPid = o.optLong("patientId", -1);
            long newPid = patientMap.containsKey(oldPid) ? patientMap.get(oldPid) : -1L;
            db.patientHistoryDao().insert(new PatientHistory(
                    newPid, optString(o, "title"), optString(o, "details"),
                    optString(o, "date"),
                    o.optLong("createdAt", System.currentTimeMillis())));
        }
    }

    private static void restoreAttachments(Context context, AppDatabase db, JSONArray items,
                                           Map<Long, Long> patientMap, List<File> writtenFiles) throws Exception {
        if (items == null) return;
        File dir = new File(context.getFilesDir(), "attachments");
        if (!dir.exists()) dir.mkdirs();
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.getJSONObject(i);
            long oldPid = o.optLong("patientId", -1);
            long newPid = patientMap.containsKey(oldPid) ? patientMap.get(oldPid) : -1L;
            String name = optString(o, "name");
            String filePath = null;
            if (o.has("file")) {
                byte[] data = android.util.Base64.decode(o.getString("file"), android.util.Base64.DEFAULT);
                String safe = name != null && !name.isEmpty()
                        ? name.replaceAll("[^a-zA-Z0-9._-]", "_") : "file_" + i;
                File f = new File(dir, safe);
                writeFile(f, data);
                writtenFiles.add(f);
                filePath = f.getAbsolutePath();
            }
            db.patientAttachmentDao().insert(new PatientAttachment(
                    newPid, filePath, name, optString(o, "type"), optString(o, "note"),
                    o.optLong("createdAt", System.currentTimeMillis())));
        }
    }

    private static void restoreLogs(AppDatabase db, JSONArray logs, long ownerId) throws Exception {
        if (logs == null) return;
        for (int i = 0; i < logs.length(); i++) {
            JSONObject o = logs.getJSONObject(i);
            Long entityId = o.has("entityId") && !o.isNull("entityId") ? o.getLong("entityId") : null;
            db.logDao().insert(new LogEntry(
                    o.optLong("timestamp", System.currentTimeMillis()),
                    ownerId,
                    optString(o, "action"),
                    optString(o, "entityType"),
                    entityId,
                    optString(o, "detail")));
        }
    }

    private static void restoreSettings(Context context, JSONObject settings, long ownerId) throws Exception {
        PreferencesManager prefs = new PreferencesManager(context);
        if (settings == null) return;
        if (settings.has("themeMode")) prefs.setThemeMode(settings.getString("themeMode"));
        if (settings.has("themeStyle")) prefs.setThemeStyle(settings.getString("themeStyle"));
        if (settings.has("patientSort")) prefs.setPatientSortMode(settings.getInt("patientSort"));
        if (settings.has("appointmentSort")) prefs.setAppointmentSortMode(settings.getInt("appointmentSort"));
        if (settings.has("duration")) prefs.setDefaultAppointmentDuration(settings.getInt("duration"));
        if (settings.has("backupFrequency")) prefs.setBackupFrequency(settings.getString("backupFrequency"));
        if (settings.has("profilePhoto")) {
            try {
                byte[] data = android.util.Base64.decode(settings.getString("profilePhoto"), android.util.Base64.DEFAULT);
                File f = PreferencesManager.avatarFileFor(context, ownerId);
                if (f.getParentFile() != null) {
                    f.getParentFile().mkdirs();
                }
                writeFile(f, data);
                prefs.setProfilePhotoPathForUser(ownerId, f.getAbsolutePath());
            } catch (Exception ignored) {}
        } else {
            File stale = PreferencesManager.avatarFileFor(context, ownerId);
            if (stale.exists()) {
                stale.delete();
            }
            prefs.clearProfilePhotoPathForUser(ownerId);
        }
    }

    private static void restoreUserThemes(Context context, JSONArray themes, long ownerId) throws Exception {
        PreferencesManager prefs = new PreferencesManager(context);
        if (themes == null) return;
        for (int i = 0; i < themes.length(); i++) {
            JSONObject o = themes.getJSONObject(i);
            String mode = o.has("mode") && !o.isNull("mode") ? o.getString("mode") : null;
            String style = o.has("style") && !o.isNull("style") ? o.getString("style") : null;
            if (mode != null) prefs.setThemeModeForUser(ownerId, mode);
            if (style != null) prefs.setThemeStyleForUser(ownerId, style);
        }
    }

    static byte[] encrypt(byte[] plain, String password) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] iv = new byte[12];
        random.nextBytes(iv);

        SecretKey key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ct = cipher.doFinal(plain);

        byte[] magic = MAGIC.getBytes("US-ASCII");
        ByteBuffer buf = ByteBuffer.allocate(magic.length + 4 + salt.length + iv.length + ct.length);
        buf.put(magic);
        buf.putInt(FORMAT_VERSION);
        buf.put(salt);
        buf.put(iv);
        buf.put(ct);
        return buf.array();
    }

    static byte[] decrypt(byte[] data, String password) throws Exception {
        byte[] magic = MAGIC.getBytes("US-ASCII");
        if (data.length < magic.length + 4 + 16 + 12) return null;
        ByteBuffer buf = ByteBuffer.wrap(data);
        byte[] readMagic = new byte[magic.length];
        buf.get(readMagic);
        if (!java.util.Arrays.equals(magic, readMagic)) return null;
        int version = buf.getInt();
        if (version != FORMAT_VERSION) return null;
        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        buf.get(salt);
        buf.get(iv);
        byte[] ct = new byte[buf.remaining()];
        buf.get(ct);

        SecretKey key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(ct);
    }

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private static byte[] readUri(Context context, Uri uri) throws Exception {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) throw new IllegalArgumentException("cannot open");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        in.close();
        return out.toByteArray();
    }

    private static byte[] readFile(File file) throws Exception {
        FileInputStream in = new FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        in.close();
        return out.toByteArray();
    }

    private static void writeFile(File file, byte[] data) throws Exception {
        FileOutputStream out = new FileOutputStream(file);
        out.write(data);
        out.flush();
        out.close();
    }

    private static String optString(JSONObject o, String key) {
        return o.isNull(key) ? null : o.optString(key, null);
    }

    private static void logExport(Context context, boolean success) {
        try {
            PreferencesManager prefs = new PreferencesManager(context);
            AppDatabase db = AppDatabase.getInstance(context);
            db.logDao().insert(new LogEntry(System.currentTimeMillis(), prefs.getLoggedInUserId(),
                    LogEntry.ACTION_EXPORT, "all", null,
                    success ? "export succeeded" : "export failed"));
        } catch (Exception ignored) {}
    }

    private static void logImport(Context context, boolean success, long userId) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            db.logDao().insert(new LogEntry(System.currentTimeMillis(), userId,
                    LogEntry.ACTION_IMPORT, "all", null,
                    success ? "import succeeded" : "import failed"));
        } catch (Exception ignored) {}
    }
}
package com.medcare.app.utils;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
public class PreferencesManager {
    private static final String PREF_NAME = "medcare_prefs";
    private static final String KEY_LOGGED_IN_USER_ID = "logged_in_user_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_THEME_STYLE = "theme_style";
    private static final String KEY_PENDING_THEME_STYLE = "pending_theme_style";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_PATIENT_SORT = "patient_sort";
    private static final String KEY_APPOINTMENT_SORT = "appointment_sort";
    private static final String KEY_DEFAULT_DURATION = "default_duration";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_BIOMETRIC_TIMEOUT = "biometric_timeout";
    private static final String KEY_LAST_BACKGROUND_TIME = "last_background_time";
    private static final String KEY_LAST_UNLOCK_TIME = "last_unlock_time";
    private static final String KEY_DEVELOPER_MODE = "developer_mode";
    private static final String KEY_MOCK_PATIENT_IDS = "mock_patient_ids";
    private static final String KEY_MOCK_APPOINTMENT_IDS = "mock_appointment_ids";
    private static final String KEY_REMINDERS_ENABLED = "reminders_enabled";
    private static final String KEY_REMINDER_LEAD_MINUTES = "reminder_lead_minutes";
    private static final String KEY_NOTIFIED_REMINDER_KEYS = "notified_reminder_keys";
    private static final String KEY_FOLLOWUP_PROMPTED_IDS = "followup_prompted_ids";
    private static final String KEY_BACKUP_FREQUENCY = "backup_frequency";
    private static final String KEY_BACKGROUND_WORK_ENABLED = "background_work_enabled";
    private static final String KEY_LAST_BACKUP_TIME = "last_backup_time";
    private static final String KEY_LAST_RESTORE_TIME = "last_restore_time";
    private static final String KEY_BACKUP_PASSWORD = "backup_password";
    private final SharedPreferences preferences;
    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    public void setLoggedInUserId(long userId) {
        preferences.edit().putLong(KEY_LOGGED_IN_USER_ID, userId).apply();
        preferences.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply();
    }
    public long getLoggedInUserId() {
        return preferences.getLong(KEY_LOGGED_IN_USER_ID, -1);
    }
    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    public void clearSession() {
        long userId = getLoggedInUserId();
        String userMode = userId != -1 ? getThemeModeForUser(userId) : null;
        String userStyle = userId != -1 ? getThemeStyleForUser(userId) : null;
        String userLang = userId != -1 ? getLanguageForUser(userId) : null;
        String lang = getLanguage();
        preferences.edit().clear().commit();
        setThemeMode("system");
        setThemeStyle("blue");
        setLanguage(lang);
        if (userId != -1) {
            if (userMode != null) {
                preferences.edit().putString(themeModeKeyForUser(userId), userMode).commit();
            }
            if (userStyle != null) {
                preferences.edit().putString(themeStyleKeyForUser(userId), userStyle).commit();
            }
            if (userLang != null && !userLang.isEmpty()) {
                preferences.edit().putString(languageKeyForUser(userId), userLang).commit();
            }
        }
    }
    private String themeModeKeyForUser(long userId) {
        return "theme_mode_" + userId;
    }
    private String themeStyleKeyForUser(long userId) {
        return "theme_style_" + userId;
    }
    public String getThemeMode() {
        return preferences.getString(KEY_THEME_MODE, "system");
    }
    public void setThemeMode(String mode) {
        preferences.edit().putString(KEY_THEME_MODE, mode).apply();
        long userId = getLoggedInUserId();
        if (userId != -1) {
            preferences.edit().putString(themeModeKeyForUser(userId), mode).apply();
        }
    }
    public String getThemeModeForUser(long userId) {
        return preferences.getString(themeModeKeyForUser(userId), null);
    }

    public void setThemeModeForUser(long userId, String mode) {
        preferences.edit().putString(themeModeKeyForUser(userId), mode).apply();
    }
    public String getThemeStyle() {
        return preferences.getString(KEY_THEME_STYLE, "blue");
    }
    public void setThemeStyle(String style) {
        preferences.edit().putString(KEY_THEME_STYLE, style).apply();
        long userId = getLoggedInUserId();
        if (userId != -1) {
            preferences.edit().putString(themeStyleKeyForUser(userId), style).apply();
        }
    }
    public String getPendingThemeStyle() {
        return preferences.getString(KEY_PENDING_THEME_STYLE, null);
    }
    public void setPendingThemeStyle(String style) {
        preferences.edit().putString(KEY_PENDING_THEME_STYLE, style).apply();
    }
    public void clearPendingThemeStyle() {
        preferences.edit().remove(KEY_PENDING_THEME_STYLE).apply();
    }
    public String getThemeStyleForUser(long userId) {
        return preferences.getString(themeStyleKeyForUser(userId), null);
    }

    public void setThemeStyleForUser(long userId, String style) {
        preferences.edit().putString(themeStyleKeyForUser(userId), style).apply();
    }
    public String getLanguage() {
        return preferences.getString(KEY_LANGUAGE, "system");
    }
    public void setLanguage(String lang) {
        preferences.edit().putString(KEY_LANGUAGE, lang).commit();
    }
    private String languageKeyForUser(long userId) {
        return "language_" + userId;
    }
    public String getLanguageForUser(long userId) {
        return preferences.getString(languageKeyForUser(userId), null);
    }
    public void setLanguageForUser(long userId, String lang) {
        preferences.edit().putString(languageKeyForUser(userId), lang).apply();
    }
    public int getPatientSortMode(int defaultMode) {
        return preferences.getInt(KEY_PATIENT_SORT, defaultMode);
    }
    public void setPatientSortMode(int mode) {
        preferences.edit().putInt(KEY_PATIENT_SORT, mode).apply();
    }
    public int getAppointmentSortMode(int defaultMode) {
        return preferences.getInt(KEY_APPOINTMENT_SORT, defaultMode);
    }
    public void setAppointmentSortMode(int mode) {
        preferences.edit().putInt(KEY_APPOINTMENT_SORT, mode).apply();
    }
    public int getDefaultAppointmentDuration() {
        return preferences.getInt(KEY_DEFAULT_DURATION, 30);
    }
    public void setDefaultAppointmentDuration(int minutes) {
        preferences.edit().putInt(KEY_DEFAULT_DURATION, minutes).apply();
    }

    public boolean isBiometricEnabled() {
        return preferences.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).commit();
    }

    public String getBiometricTimeout() {
        return preferences.getString(KEY_BIOMETRIC_TIMEOUT, "immediate");
    }

    public void setBiometricTimeout(String timeout) {
        preferences.edit().putString(KEY_BIOMETRIC_TIMEOUT, timeout).commit();
    }

    public long getLastBackgroundTime() {
        return preferences.getLong(KEY_LAST_BACKGROUND_TIME, 0);
    }

    public void setLastBackgroundTime(long time) {
        preferences.edit().putLong(KEY_LAST_BACKGROUND_TIME, time).commit();
    }

    public long getLastUnlockTime() {
        return preferences.getLong(KEY_LAST_UNLOCK_TIME, 0);
    }

    public void setLastUnlockTime(long time) {
        preferences.edit().putLong(KEY_LAST_UNLOCK_TIME, time).commit();
    }

    public String getProfilePhotoPath() {
        return getProfilePhotoPathForUser(getLoggedInUserId());
    }

    public void setProfilePhotoPath(String path) {
        setProfilePhotoPathForUser(getLoggedInUserId(), path);
    }

    public void clearProfilePhotoPath() {
        clearProfilePhotoPathForUser(getLoggedInUserId());
    }

    public String getProfilePhotoPathForUser(long userId) {
        return preferences.getString(profilePhotoKeyForUser(userId), null);
    }

    public void setProfilePhotoPathForUser(long userId, String path) {
        preferences.edit().putString(profilePhotoKeyForUser(userId), path).apply();
    }

    public void clearProfilePhotoPathForUser(long userId) {
        preferences.edit().remove(profilePhotoKeyForUser(userId)).apply();
    }

    private String profilePhotoKeyForUser(long userId) {
        return "profile_photo_" + userId;
    }

    public static File avatarFileFor(Context context, long userId) {
        return new File(new File(context.getFilesDir(), "avatars"), "profile_" + userId + ".jpg");
    }

    public boolean isDeveloperMode() {
        return preferences.getBoolean(KEY_DEVELOPER_MODE, false);
    }

    public void setDeveloperMode(boolean enabled) {
        preferences.edit().putBoolean(KEY_DEVELOPER_MODE, enabled).apply();
    }

    public Set<String> getMockPatientIds() {
        return preferences.getStringSet(KEY_MOCK_PATIENT_IDS, new HashSet<String>());
    }

    public void setMockPatientIds(Set<String> ids) {
        preferences.edit().putStringSet(KEY_MOCK_PATIENT_IDS, ids).apply();
    }

    public Set<String> getMockAppointmentIds() {
        return preferences.getStringSet(KEY_MOCK_APPOINTMENT_IDS, new HashSet<String>());
    }

    public void setMockAppointmentIds(Set<String> ids) {
        preferences.edit().putStringSet(KEY_MOCK_APPOINTMENT_IDS, ids).apply();
    }

    public boolean isRemindersEnabled() {
        return preferences.getBoolean(KEY_REMINDERS_ENABLED, true);
    }

    public void setRemindersEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply();
    }

    public int getReminderLeadMinutes() {
        return preferences.getInt(KEY_REMINDER_LEAD_MINUTES, 60);
    }

    public void setReminderLeadMinutes(int minutes) {
        preferences.edit().putInt(KEY_REMINDER_LEAD_MINUTES, minutes).apply();
    }

    public Set<String> getNotifiedReminderKeys() {
        return preferences.getStringSet(KEY_NOTIFIED_REMINDER_KEYS, new HashSet<String>());
    }

    public void setNotifiedReminderKeys(Set<String> keys) {
        preferences.edit().putStringSet(KEY_NOTIFIED_REMINDER_KEYS, keys).apply();
    }

    public Set<String> getFollowUpPromptedIds() {
        return preferences.getStringSet(KEY_FOLLOWUP_PROMPTED_IDS, new HashSet<String>());
    }

    public void setFollowUpPromptedIds(Set<String> ids) {
        preferences.edit().putStringSet(KEY_FOLLOWUP_PROMPTED_IDS, ids).apply();
    }

    public void clearFollowUpPrompted(long appointmentId) {
        Set<String> set = new HashSet<>(getFollowUpPromptedIds());
        set.remove(String.valueOf(appointmentId));
        setFollowUpPromptedIds(set);
    }

    public String getBackupFrequency() {
        return preferences.getString(KEY_BACKUP_FREQUENCY, "off");
    }

    public void setBackupFrequency(String frequency) {
        preferences.edit().putString(KEY_BACKUP_FREQUENCY, frequency).apply();
    }

    public boolean isBackgroundWorkEnabled() {
        return preferences.getBoolean(KEY_BACKGROUND_WORK_ENABLED, true);
    }

    public void setBackgroundWorkEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_BACKGROUND_WORK_ENABLED, enabled).apply();
    }

    public long getLastBackupTime() {
        return preferences.getLong(KEY_LAST_BACKUP_TIME, 0);
    }

    public void setLastBackupTime(long time) {
        preferences.edit().putLong(KEY_LAST_BACKUP_TIME, time).apply();
    }

    public long getLastRestoreTime() {
        return preferences.getLong(KEY_LAST_RESTORE_TIME, 0);
    }

    public void setLastRestoreTime(long time) {
        preferences.edit().putLong(KEY_LAST_RESTORE_TIME, time).apply();
    }

    public String getBackupPassword() {
        return preferences.getString(KEY_BACKUP_PASSWORD, null);
    }

    public boolean hasBackupPassword() {
        String p = getBackupPassword();
        return p != null && !p.isEmpty();
    }

    public void setBackupPassword(String password) {
        preferences.edit().putString(KEY_BACKUP_PASSWORD, password).apply();
    }
}

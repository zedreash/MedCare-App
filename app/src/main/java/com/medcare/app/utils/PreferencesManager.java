package com.medcare.app.utils;
import android.content.Context;
import android.content.SharedPreferences;
public class PreferencesManager {
    private static final String PREF_NAME = "medcare_prefs";
    private static final String KEY_LOGGED_IN_USER_ID = "logged_in_user_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_PATIENT_SORT = "patient_sort";
    private static final String KEY_APPOINTMENT_SORT = "appointment_sort";
    private static final String KEY_DEFAULT_DURATION = "default_duration";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_BIOMETRIC_TIMEOUT = "biometric_timeout";
    private static final String KEY_LAST_BACKGROUND_TIME = "last_background_time";
    private static final String KEY_LAST_UNLOCK_TIME = "last_unlock_time";
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
        String theme = getThemeMode();
        String lang = getLanguage();
        preferences.edit().clear().commit();
        setThemeMode(theme);
        setLanguage(lang);
    }
    public String getThemeMode() {
        return preferences.getString(KEY_THEME_MODE, "system");
    }
    public void setThemeMode(String mode) {
        preferences.edit().putString(KEY_THEME_MODE, mode).apply();
    }
    public String getLanguage() {
        return preferences.getString(KEY_LANGUAGE, "system");
    }
    public void setLanguage(String lang) {
        preferences.edit().putString(KEY_LANGUAGE, lang).commit();
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
}

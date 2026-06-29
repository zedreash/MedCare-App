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
        preferences.edit().clear().commit();
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
}

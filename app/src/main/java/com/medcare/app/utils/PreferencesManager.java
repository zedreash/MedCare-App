package com.medcare.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREF_NAME = "medcare_prefs";
    private static final String KEY_LOGGED_IN_USER_ID = "logged_in_user_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

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
        preferences.edit().clear().apply();
    }
}

package com.medcare.app;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.color.MaterialColors;
import com.medcare.app.background.BackgroundScheduler;
import com.medcare.app.notifications.FollowUpScheduler;
import com.medcare.app.utils.BackupManager;
import com.medcare.app.utils.PreferencesManager;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private View customBottomNav;
    private PreferencesManager preferencesManager;
    private LinearLayout[] navTabs;
    private ImageView[] navIcons;
    private final int[] navDestinations = {
            R.id.dashboardFragment,
            R.id.patientListFragment,
            R.id.appointmentListFragment,
            R.id.clinicFragment,
            R.id.calendarFragment,
            R.id.profileFragment
    };
    private static Locale resolveLocale(String lang) {
        if ("system".equals(lang)) {
            String systemLang = Locale.getDefault().getLanguage();
            if (!systemLang.equals("en") && !systemLang.equals("ar")
                    && !systemLang.equals("iw") && !systemLang.equals("he")) {
                return new Locale("iw", "IL");
            }
            return null;
        }
        if ("he".equals(lang)) {
            return new Locale("iw", "IL");
        }
        return new Locale(lang);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        PreferencesManager prefs = new PreferencesManager(newBase);
        Locale locale = resolveLocale(prefs.getLanguage());
        if (locale == null) {
            super.attachBaseContext(newBase);
            return;
        }
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        Context localizedContext = newBase.createConfigurationContext(config);
        super.attachBaseContext(localizedContext);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesManager prefs = new PreferencesManager(this);
        if (!prefs.isLoggedIn() && !"blue".equals(prefs.getThemeStyle())) {
            prefs.setThemeStyle("blue");
        }
        String themeStyle = prefs.getThemeStyle();
        String pendingStyle = prefs.getPendingThemeStyle();
        if (pendingStyle != null && !pendingStyle.isEmpty()) {
            themeStyle = pendingStyle;
        }
        setTheme(themeStyleToRes(themeStyle));
        switch (prefs.getThemeMode()) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferencesManager = new PreferencesManager(this);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        customBottomNav = findViewById(R.id.custom_bottom_nav);

        FollowUpScheduler.scheduleCheck(this);
        long uid = preferencesManager.getLoggedInUserId();
        if (uid != -1) {
            FollowUpScheduler.backfill(this, uid);
            catchUpBackupIfDue();
            BackgroundScheduler.rescheduleAll(this);
        }

        if (!preferencesManager.isLoggedIn()) {
            int currentDest = navController.getCurrentDestination() != null
                    ? navController.getCurrentDestination().getId() : -1;
            if (currentDest != R.id.loginFragment && currentDest != R.id.registerFragment
                    && currentDest != R.id.registrationStepsFragment) {
                navController.navigate(R.id.loginFragment, null,
                        new NavOptions.Builder()
                                .setPopUpTo(R.id.dashboardFragment, true)
                                .build());
            }
        }

        String stalePending = preferencesManager.getPendingThemeStyle();
        if (stalePending != null && !stalePending.isEmpty()
                && navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() != R.id.registrationStepsFragment) {
            preferencesManager.clearPendingThemeStyle();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isFinishing() && !isDestroyed()) recreate();
            });
        }

        setupCustomBottomNav();

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean isMain = false;
            int matchedIndex = -1;
            for (int i = 0; i < navDestinations.length; i++) {
                if (destination.getId() == navDestinations[i]) {
                    isMain = true;
                    matchedIndex = i;
                    break;
                }
            }
            if (destination.getId() == R.id.settingsFragment) {
                isMain = false;
            }
            customBottomNav.setVisibility(isMain ? View.VISIBLE : View.GONE);
            if (matchedIndex != -1) {
                setTabSelected(matchedIndex);
            }
        });
    }

    private void catchUpBackupIfDue() {
        if (!preferencesManager.isLoggedIn() || !preferencesManager.hasBackupPassword()) return;
        long periodMs = BackupManager.periodMillis(preferencesManager.getBackupFrequency());
        if (periodMs <= 0) return;
        if (System.currentTimeMillis() - preferencesManager.getLastBackupTime() >= periodMs) {
            BackupManager.backupNow(this, result -> {});
        }
    }

    private void setupCustomBottomNav() {
        navTabs = new LinearLayout[]{
                findViewById(R.id.nav_dashboard),
                findViewById(R.id.nav_patients),
                findViewById(R.id.nav_appointments),
                findViewById(R.id.nav_clinic),
                findViewById(R.id.nav_calendar),
                findViewById(R.id.nav_profile)
        };
        navIcons = new ImageView[]{
                findViewById(R.id.nav_dashboard_icon),
                findViewById(R.id.nav_patients_icon),
                findViewById(R.id.nav_appointments_icon),
                findViewById(R.id.nav_clinic_icon),
                findViewById(R.id.nav_calendar_icon),
                findViewById(R.id.nav_profile_icon)
        };

        for (int i = 0; i < navTabs.length; i++) {
            final int destId = navDestinations[i];
            navTabs[i].setOnClickListener(v -> {
                if (navController.getCurrentDestination() == null) return;
                if (destId != navController.getCurrentDestination().getId()) {
                    if (!navController.popBackStack(destId, false)) {
                        NavOptions options = new NavOptions.Builder()
                                .setEnterAnim(android.R.anim.fade_in)
                                .setExitAnim(android.R.anim.fade_out)
                                .setPopEnterAnim(android.R.anim.fade_in)
                                .setPopExitAnim(android.R.anim.fade_out)
                                .build();
                        navController.navigate(destId, null, options);
                    }
                }
            });
        }
    }

    private static int themeStyleToRes(String style) {
        if (style == null) return R.style.Theme_MedCare_Blue;
        switch (style) {
            case "green":
                return R.style.Theme_MedCare_Green;
            case "purple":
                return R.style.Theme_MedCare_Purple;
            case "orange":
                return R.style.Theme_MedCare_Orange;
            default:
                return R.style.Theme_MedCare_Blue;
        }
    }

    private void setTabSelected(int index) {
        if (index < 0 || index >= navTabs.length) return;
        int primaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary,
                ContextCompat.getColor(this, R.color.primary));
        int inactiveColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant,
                ContextCompat.getColor(this, R.color.text_secondary));

        for (int i = 0; i < navTabs.length; i++) {
            navIcons[i].setColorFilter(i == index ? primaryColor : inactiveColor, PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferencesManager != null && preferencesManager.isLoggedIn()) {
            maybeShowBiometricLock();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void maybeShowBiometricLock() {
        if (!preferencesManager.isBiometricEnabled()) return;
        if (navController.getCurrentDestination() == null) return;

        int currentDestId = navController.getCurrentDestination().getId();
        if (currentDestId == R.id.biometricLockFragment
                || currentDestId == R.id.loginFragment
                || currentDestId == R.id.registerFragment) return;

        long lastBackground = preferencesManager.getLastBackgroundTime();
        if (lastBackground == 0) {
            long lastUnlock = preferencesManager.getLastUnlockTime();
            if (lastUnlock == 0 || System.currentTimeMillis() - lastUnlock > 60000) {
                navController.navigate(R.id.biometricLockFragment);
            }
            return;
        }

        long lastUnlock = preferencesManager.getLastUnlockTime();
        if (lastUnlock >= lastBackground) return;

        long elapsed = System.currentTimeMillis() - lastBackground;
        String timeout = preferencesManager.getBiometricTimeout();
        long timeoutMs;
        switch (timeout) {
            case "1min":
                timeoutMs = 60000;
                break;
            case "5min":
                timeoutMs = 300000;
                break;
            case "15min":
                timeoutMs = 900000;
                break;
            default:
                timeoutMs = 0;
                break;
        }

        if (elapsed >= timeoutMs) {
            navController.navigate(R.id.biometricLockFragment);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}

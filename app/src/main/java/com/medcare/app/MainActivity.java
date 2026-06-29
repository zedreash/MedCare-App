package com.medcare.app;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.medcare.app.utils.PreferencesManager;
import java.util.Locale;
public class MainActivity extends AppCompatActivity {
    private NavController navController;
    private BottomNavigationView bottomNav;
    private final int[] mainDestinations = {
            R.id.dashboardFragment,
            R.id.patientListFragment,
            R.id.appointmentListFragment,
            R.id.clinicFragment,
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
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId != navController.getCurrentDestination().getId()) {
                    navController.navigate(itemId);
                }
                return true;
            });
        }
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean isMain = false;
            int matchedId = -1;
            for (int id : mainDestinations) {
                if (destination.getId() == id) {
                    isMain = true;
                    matchedId = id;
                    break;
                }
            }
            if (destination.getId() == R.id.settingsFragment) {
                isMain = false;
            }
            bottomNav.setVisibility(isMain ? View.VISIBLE : View.GONE);
            if (matchedId != -1) {
                bottomNav.setSelectedItemId(matchedId);
            }
        });
    }
    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
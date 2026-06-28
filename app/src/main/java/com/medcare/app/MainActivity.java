package com.medcare.app;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNav;

    private final int[] mainDestinations = {
            R.id.patientListFragment,
            R.id.appointmentListFragment,
            R.id.clinicFragment,
            R.id.profileFragment
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean isMain = false;
            for (int id : mainDestinations) {
                if (destination.getId() == id) {
                    isMain = true;
                    break;
                }
            }
            bottomNav.setVisibility(isMain ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}

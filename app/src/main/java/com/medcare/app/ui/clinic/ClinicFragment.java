package com.medcare.app.ui.clinic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import com.medcare.app.R;

public class ClinicFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final LatLng CLINIC_LOCATION = new LatLng(32.0471, 34.8431);

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private View rootView;
    private Location userLocation;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clinic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rootView = view;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        view.findViewById(R.id.directions_button).setOnClickListener(v -> openDirections());

        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupMap();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupMap();
            } else {
                showMapWithoutLocation();
            }
        }
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            getUserLocation();
        } else {
            showClinicOnly();
        }
    }

    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showClinicOnly();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    userLocation = location;
                    if (location != null) {
                        showBothLocations(location);
                    } else {
                        showClinicOnly();
                    }
                })
                .addOnFailureListener(e -> showClinicOnly());
    }

    private void showBothLocations(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        googleMap.addMarker(new MarkerOptions()
                .position(userLatLng)
                .title(getString(R.string.your_location))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        googleMap.addMarker(new MarkerOptions()
                .position(CLINIC_LOCATION)
                .title(getString(R.string.clinic_location))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        animateToFitBoth(userLatLng, CLINIC_LOCATION);
    }

    private void showClinicOnly() {
        googleMap.addMarker(new MarkerOptions()
                .position(CLINIC_LOCATION)
                .title(getString(R.string.clinic_location)));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CLINIC_LOCATION, 15f));

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(rootView, R.string.location_permission_denied, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showMapWithoutLocation() {
        setupMap();
    }

    private void animateToFitBoth(LatLng a, LatLng b) {
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(a)
                .include(b)
                .build();

        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
    }

    private void openDirections() {
        String uri = "https://www.google.com/maps/dir/?api=1&destination="
                + CLINIC_LOCATION.latitude + "," + CLINIC_LOCATION.longitude;

        if (userLocation != null) {
            uri += "&origin=" + userLocation.getLatitude() + "," + userLocation.getLongitude();
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(webIntent);
        }
    }
}

package com.medcare.app.ui.clinic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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
    private static final LatLngBounds ISRAEL_BOUNDS = new LatLngBounds(
            new LatLng(29.5, 34.2),
            new LatLng(33.3, 35.9)
    );

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private View rootView;
    private Location userLocation;

    private boolean locationPermissionDenied = false;

    private LocationCallback locationCallback;

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
                locationPermissionDenied = true;
                setupMap();
            }
        }
    }

    private void setupMap() {
        FragmentManager fm = getChildFragmentManager();
        Fragment existing = fm.findFragmentById(R.id.map_container);
        if (existing == null) {
            fm.beginTransaction()
                    .add(R.id.map_container, new SupportMapFragment())
                    .commitNow();
        }
        SupportMapFragment mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setLatLngBoundsForCameraTarget(ISRAEL_BOUNDS);
        googleMap.setMinZoomPreference(7f);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(CLINIC_LOCATION, 17f));

        if (!locationPermissionDenied &&
                ContextCompat.checkSelfPermission(requireContext(),
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
                    if (location != null) {
                        userLocation = location;
                        showBothLocations(location);
                    } else {
                        requestFreshLocation();
                    }
                })
                .addOnFailureListener(e -> showClinicOnly());
    }

    private void requestFreshLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showClinicOnly();
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMaxUpdates(1) // we only need one fix
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    userLocation = location;
                    showBothLocations(location);
                } else {
                    showClinicOnly();
                }

                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
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
    }

    private void showClinicOnly() {
        googleMap.addMarker(new MarkerOptions()
                .position(CLINIC_LOCATION)
                .title(getString(R.string.clinic_location)));

        if (locationPermissionDenied) {
            Snackbar.make(rootView, R.string.location_permission_denied, Snackbar.LENGTH_LONG).show();
        }
    }

    private void openDirections() {
        String uri = "https://www.google.com/maps/dir/?api=1&destination="
                + CLINIC_LOCATION.latitude + "," + CLINIC_LOCATION.longitude;

        if (userLocation != null) {
            uri += "&origin=" + userLocation.getLatitude() + "," + userLocation.getLongitude();
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(webIntent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }
}
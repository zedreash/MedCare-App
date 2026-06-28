package com.medcare.app.ui.patients;

import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import com.medcare.app.R;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.utils.DateUtils;
import com.medcare.app.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PatientFormFragment extends Fragment {

    private PatientRepository patientRepository;
    private PlacesClient placesClient;
    private long patientId = -1;
    private Patient currentPatient;

    private TextInputLayout nameLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout diagnosisLayout;
    private TextInputLayout addressLayout;

    private EditText nameInput;
    private EditText phoneInput;
    private EditText diagnosisInput;
    private EditText addressInput;

    private double pendingLat;
    private double pendingLng;

    private View rootView;
    private TextView formTitle;
    private View deleteButton;

    private ListPopupWindow popup;
    private List<AutocompletePrediction> predictions = new ArrayList<>();
    private ArrayAdapter<String> predictionAdapter;
    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private AutocompleteSessionToken sessionToken;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientId = getArguments().getInt("patientId", -1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rootView = view;
        patientRepository = new PatientRepository(requireContext());

        initPlaces();
        initViews(view);
        setupAutocomplete();
        setupErrorClearListeners();

        if (patientId != -1) {
            formTitle.setText(R.string.edit_patient);
            deleteButton.setVisibility(View.VISIBLE);
            loadPatient();
        }

        view.findViewById(R.id.back_button).setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.save_button).setOnClickListener(v -> onSaveClicked());
        deleteButton.setOnClickListener(v -> onDeleteClicked());
    }

    private void initPlaces() {
        if (!Places.isInitialized()) {
            try {
                ApplicationInfo appInfo = requireContext().getPackageManager()
                        .getApplicationInfo(requireContext().getPackageName(), PackageManager.GET_META_DATA);
                String apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY");
                if (apiKey != null && !apiKey.isEmpty()) {
                    Places.initialize(requireContext(), apiKey);
                }
            } catch (PackageManager.NameNotFoundException e) {
                // API key not available, Places features disabled
            }
        }
        placesClient = Places.createClient(requireContext());
    }

    private void initViews(View view) {
        formTitle = view.findViewById(R.id.form_title);
        deleteButton = view.findViewById(R.id.delete_button);
        nameLayout = view.findViewById(R.id.name_layout);
        phoneLayout = view.findViewById(R.id.phone_layout);
        diagnosisLayout = view.findViewById(R.id.diagnosis_layout);
        addressLayout = view.findViewById(R.id.address_layout);

        nameInput = view.findViewById(R.id.name_input);
        phoneInput = view.findViewById(R.id.phone_input);
        diagnosisInput = view.findViewById(R.id.diagnosis_input);
        addressInput = view.findViewById(R.id.address_input);
    }

    private void setupAutocomplete() {
        sessionToken = AutocompleteSessionToken.newInstance();
        predictionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);

        popup = new ListPopupWindow(requireContext());
        popup.setAnchorView(addressLayout);
        popup.setAdapter(predictionAdapter);
        popup.setModal(false);
        popup.setOnItemClickListener((parent, view, position, id) -> {
            AutocompletePrediction prediction = predictions.get(position);
            selectPrediction(prediction);
        });

        addressInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> fetchPredictions(s.toString());
                debounceHandler.postDelayed(debounceRunnable, 400);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        addressInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) popup.dismiss();
        });
    }

    private void fetchPredictions(String query) {
        if (placesClient == null || query.trim().isEmpty()) {
            popup.dismiss();
            return;
        }

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(query)
                .setCountries("IL")
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    predictions = response.getAutocompletePredictions();
                    List<String> items = new ArrayList<>();
                    for (AutocompletePrediction p : predictions) {
                        items.add(p.getPrimaryText(null).toString());
                    }
                    predictionAdapter.clear();
                    predictionAdapter.addAll(items);
                    if (!items.isEmpty()) {
                        popup.show();
                    } else {
                        popup.dismiss();
                    }
                })
                .addOnFailureListener(e -> {});
    }

    private void selectPrediction(AutocompletePrediction prediction) {
        popup.dismiss();
        addressLayout.setError(null);

        List<Place.Field> fields = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG);
        FetchPlaceRequest request = FetchPlaceRequest.builder(prediction.getPlaceId(), fields).build();

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    addressInput.setText(place.getAddress());
                    if (place.getLatLng() != null) {
                        pendingLat = place.getLatLng().latitude;
                        pendingLng = place.getLatLng().longitude;
                        if (currentPatient != null) {
                            currentPatient.setLatitude(pendingLat);
                            currentPatient.setLongitude(pendingLng);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    addressInput.setText(prediction.getPrimaryText(null));
                });
    }

    private void setupErrorClearListeners() {
        nameInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) nameLayout.setError(null); });
        phoneInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) phoneLayout.setError(null); });
        diagnosisInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) diagnosisLayout.setError(null); });
    }

    private void loadPatient() {
        currentPatient = patientRepository.getPatientById(patientId);
        if (currentPatient == null) {
            Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
            Navigation.findNavController(rootView).navigateUp();
            return;
        }

        nameInput.setText(currentPatient.getFullName());
        phoneInput.setText(currentPatient.getPhone());
        diagnosisInput.setText(currentPatient.getDiagnosis());
        addressInput.setText(currentPatient.getAddress());
        pendingLat = currentPatient.getLatitude();
        pendingLng = currentPatient.getLongitude();
    }

    private void onSaveClicked() {
        if (!validateInputs()) {
            return;
        }

        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String diagnosis = diagnosisInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();

        if (patientId == -1) {
            Patient patient = new Patient(name, phone, diagnosis, "", address, DateUtils.getCurrentTimestamp());
            patient.setLatitude(pendingLat);
            patient.setLongitude(pendingLng);
            patientRepository.insert(patient);
            Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
        } else {
            currentPatient.setFullName(name);
            currentPatient.setPhone(phone);
            currentPatient.setDiagnosis(diagnosis);
            currentPatient.setAddress(address);
            currentPatient.setLatitude(pendingLat);
            currentPatient.setLongitude(pendingLng);
            patientRepository.update(currentPatient);
            Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
        }

        Navigation.findNavController(rootView).navigateUp();
    }

    private void onDeleteClicked() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    patientRepository.delete(currentPatient);
                    Snackbar.make(rootView, R.string.success_deleted, Snackbar.LENGTH_SHORT).show();
                    Navigation.findNavController(rootView).navigateUp();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean validateInputs() {
        boolean valid = true;

        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.field_required));
            valid = false;
        } else {
            nameLayout.setError(null);
        }

        if (TextUtils.isEmpty(phone)) {
            phoneLayout.setError(getString(R.string.field_required));
            valid = false;
        } else if (!ValidationUtils.isValidPhone(phone)) {
            phoneLayout.setError(getString(R.string.invalid_phone));
            valid = false;
        } else {
            phoneLayout.setError(null);
        }

        return valid;
    }
}

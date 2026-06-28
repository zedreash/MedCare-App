package com.medcare.app.ui.patients;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import com.medcare.app.R;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.utils.DateUtils;
import com.medcare.app.utils.ValidationUtils;

public class PatientFormFragment extends Fragment {

    private PatientRepository patientRepository;
    private long patientId = -1;
    private Patient currentPatient;

    private TextInputLayout nameLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout diagnosisLayout;

    private EditText nameInput;
    private EditText phoneInput;
    private EditText diagnosisInput;

    private View rootView;
    private TextView formTitle;
    private View deleteButton;

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

        initViews(view);
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

    private void initViews(View view) {
        formTitle = view.findViewById(R.id.form_title);
        deleteButton = view.findViewById(R.id.delete_button);
        nameLayout = view.findViewById(R.id.name_layout);
        phoneLayout = view.findViewById(R.id.phone_layout);
        diagnosisLayout = view.findViewById(R.id.diagnosis_layout);

        nameInput = view.findViewById(R.id.name_input);
        phoneInput = view.findViewById(R.id.phone_input);
        diagnosisInput = view.findViewById(R.id.diagnosis_input);
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
    }

    private void onSaveClicked() {
        if (!validateInputs()) {
            return;
        }

        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String diagnosis = diagnosisInput.getText().toString().trim();

        if (patientId == -1) {
            Patient patient = new Patient(name, phone, diagnosis, "", DateUtils.getCurrentTimestamp());
            patientRepository.insert(patient);
            Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
        } else {
            currentPatient.setFullName(name);
            currentPatient.setPhone(phone);
            currentPatient.setDiagnosis(diagnosis);
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

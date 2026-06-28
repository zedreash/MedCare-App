package com.medcare.app.ui.profile;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import com.medcare.app.R;
import com.medcare.app.data.entity.User;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.PreferencesManager;
import com.medcare.app.utils.ValidationUtils;

import java.util.Calendar;

public class ProfileFragment extends Fragment {

    private UserRepository userRepository;
    private PreferencesManager preferencesManager;
    private User currentUser;

    private TextInputLayout tzLayout;
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout dobLayout;

    private EditText tzInput;
    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText dobInput;

    private View rootView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rootView = view;
        userRepository = new UserRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());

        initViews(view);
        setupDatePicker();
        setupErrorClearListeners();

        loadUserProfile();

        view.findViewById(R.id.back_button).setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.save_button).setOnClickListener(v -> onSaveClicked());
        view.findViewById(R.id.logout_button).setOnClickListener(v -> onLogoutClicked());
        view.findViewById(R.id.delete_account_button).setOnClickListener(v -> onDeleteAccountClicked());
    }

    private void initViews(View view) {
        tzLayout = view.findViewById(R.id.tz_layout);
        nameLayout = view.findViewById(R.id.name_layout);
        emailLayout = view.findViewById(R.id.email_layout);
        phoneLayout = view.findViewById(R.id.phone_layout);
        dobLayout = view.findViewById(R.id.dob_layout);

        tzInput = view.findViewById(R.id.tz_input);
        nameInput = view.findViewById(R.id.name_input);
        emailInput = view.findViewById(R.id.email_input);
        phoneInput = view.findViewById(R.id.phone_input);
        dobInput = view.findViewById(R.id.dob_input);
    }

    private void setupDatePicker() {
        dobInput.setOnClickListener(v -> showDatePicker());
        dobInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    dobInput.setText(formattedDate);
                    dobLayout.setError(null);
                }, year, month, day);
        datePicker.show();
    }

    private void setupErrorClearListeners() {
        nameInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) nameLayout.setError(null); });
        emailInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) emailLayout.setError(null); });
        phoneInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) phoneLayout.setError(null); });
        dobInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) dobLayout.setError(null); });
    }

    private void loadUserProfile() {
        long userId = preferencesManager.getLoggedInUserId();
        if (userId == -1) {
            Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        currentUser = userRepository.getUserById(userId);
        if (currentUser == null) {
            Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        tzInput.setText(currentUser.getTzNumber());
        nameInput.setText(currentUser.getFullName());
        emailInput.setText(currentUser.getEmail());
        phoneInput.setText(currentUser.getPhone());
        dobInput.setText(currentUser.getDateOfBirth());
    }

    private void onSaveClicked() {
        if (!validateInputs()) {
            return;
        }

        currentUser.setFullName(nameInput.getText().toString().trim());
        currentUser.setEmail(emailInput.getText().toString().trim());
        currentUser.setPhone(phoneInput.getText().toString().trim());
        currentUser.setDateOfBirth(dobInput.getText().toString().trim());

        userRepository.update(currentUser);
        Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
    }

    private boolean validateInputs() {
        boolean valid = true;

        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String dob = dobInput.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.field_required));
            valid = false;
        } else {
            nameLayout.setError(null);
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.field_required));
            valid = false;
        } else if (!ValidationUtils.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.invalid_email));
            valid = false;
        } else {
            emailLayout.setError(null);
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

        if (TextUtils.isEmpty(dob)) {
            dobLayout.setError(getString(R.string.field_required));
            valid = false;
        } else {
            dobLayout.setError(null);
        }

        return valid;
    }

    private void onDeleteAccountClicked() {
        userRepository.delete(currentUser);
        preferencesManager.clearSession();
        Snackbar.make(rootView, R.string.success_deleted, Snackbar.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void onLogoutClicked() {
        preferencesManager.clearSession();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Navigation.findNavController(rootView)
                .navigate(R.id.action_profile_to_login);
    }
}

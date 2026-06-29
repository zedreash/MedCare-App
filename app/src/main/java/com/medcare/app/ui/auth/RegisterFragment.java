package com.medcare.app.ui.auth;
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
import java.util.Locale;
public class RegisterFragment extends Fragment {
    private UserRepository userRepository;
    private PreferencesManager preferencesManager;
    private TextInputLayout tzLayout;
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout dobLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private EditText tzInput;
    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText dobInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private View rootView;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
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
        view.findViewById(R.id.register_button).setOnClickListener(v -> onRegisterClicked());
        view.findViewById(R.id.login_link).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_register_to_login));
    }
    private void initViews(View view) {
        tzLayout = view.findViewById(R.id.tz_layout);
        nameLayout = view.findViewById(R.id.name_layout);
        emailLayout = view.findViewById(R.id.email_layout);
        phoneLayout = view.findViewById(R.id.phone_layout);
        dobLayout = view.findViewById(R.id.dob_layout);
        passwordLayout = view.findViewById(R.id.password_layout);
        confirmPasswordLayout = view.findViewById(R.id.confirm_password_layout);
        tzInput = view.findViewById(R.id.tz_input);
        nameInput = view.findViewById(R.id.name_input);
        emailInput = view.findViewById(R.id.email_input);
        phoneInput = view.findViewById(R.id.phone_input);
        dobInput = view.findViewById(R.id.dob_input);
        passwordInput = view.findViewById(R.id.password_input);
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input);
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
        Locale locale = resolveAppLocale();
        if (locale != null) {
            Locale.setDefault(locale);
        }
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, -18);
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -120);
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    dobInput.setText(formattedDate);
                    dobLayout.setError(null);
                }, year, month, day);
        datePicker.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
        datePicker.getDatePicker().setMinDate(minDate.getTimeInMillis());
        datePicker.show();
    }
    private Locale resolveAppLocale() {
        String lang = preferencesManager.getLanguage();
        if ("system".equals(lang)) {
            String sysLang = Locale.getDefault().getLanguage();
            if (!sysLang.equals("en") && !sysLang.equals("ar")
                    && !sysLang.equals("iw") && !sysLang.equals("he")) {
                return new Locale("iw", "IL");
            }
            return null;
        }
        if ("he".equals(lang)) {
            return new Locale("iw", "IL");
        }
        return new Locale(lang);
    }
    private void setupErrorClearListeners() {
        tzInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) tzLayout.setError(null); });
        nameInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) nameLayout.setError(null); });
        emailInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) emailLayout.setError(null); });
        phoneInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) phoneLayout.setError(null); });
        dobInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) dobLayout.setError(null); });
        passwordInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) passwordLayout.setError(null); });
        confirmPasswordInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) confirmPasswordLayout.setError(null); });
    }
    private void onRegisterClicked() {
        if (!validateInputs()) {
            return;
        }
        String tz = tzInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String dob = dobInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        User existingUser = userRepository.getUserByEmail(email);
        if (existingUser != null) {
            emailLayout.setError(getString(R.string.email_already_registered));
            emailInput.requestFocus();
            return;
        }
        User user = new User(tz, name, email, phone, dob, password);
        long userId = userRepository.insert(user);
        if (userId != -1) {
            preferencesManager.setLoggedInUserId(userId);
            Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
            Navigation.findNavController(rootView)
                    .navigate(R.id.action_register_to_dashboard);
        } else {
            Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
        }
    }
    private boolean validateInputs() {
        boolean valid = true;
        String tz = tzInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String dob = dobInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        if (TextUtils.isEmpty(tz)) {
            tzLayout.setError(getString(R.string.field_required));
            valid = false;
        } else {
            tzLayout.setError(null);
        }
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
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.field_required));
            valid = false;
        } else if (!ValidationUtils.isValidPassword(password)) {
            passwordLayout.setError(getString(R.string.password_too_short));
            valid = false;
        } else {
            passwordLayout.setError(null);
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.field_required));
            valid = false;
        } else if (!ValidationUtils.passwordsMatch(password, confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.password_mismatch));
            valid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }
        if (!valid) {
            Snackbar.make(rootView, R.string.field_required, Snackbar.LENGTH_SHORT).show();
        }
        return valid;
    }
}

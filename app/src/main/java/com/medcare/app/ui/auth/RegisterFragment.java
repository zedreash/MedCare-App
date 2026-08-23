package com.medcare.app.ui.auth;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.medcare.app.R;
import com.medcare.app.data.entity.User;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.FieldHint;
import com.medcare.app.utils.PasswordUtils;
import com.medcare.app.utils.PreferencesManager;
import com.medcare.app.utils.ValidationUtils;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executor;
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
    private static boolean sThemeDialogPending = false;
    private BiometricPrompt biometricPrompt;
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
        if (sThemeDialogPending) {
            sThemeDialogPending = false;
            showThemeDialog();
        }
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
        FieldHint.required(tzLayout, R.string.id_number);
        FieldHint.required(nameLayout, R.string.full_name);
        FieldHint.required(emailLayout, R.string.email);
        FieldHint.required(phoneLayout, R.string.phone);
        FieldHint.required(dobLayout, R.string.date_of_birth);
        FieldHint.required(passwordLayout, R.string.password);
        FieldHint.required(confirmPasswordLayout, R.string.confirm_password);
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
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    dobInput.setText(formattedDate);
                    dobLayout.setError(null);
                }, year, month, day);
        datePicker.show();
    }
    private void hideKeyboard() {
        View focused = getActivity().getCurrentFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            focused.clearFocus();
        }
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
        hideKeyboard();
        if (!validateInputs()) {
            return;
        }
        String tz = tzInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim().toLowerCase();
        String phone = phoneInput.getText().toString().trim();
        String dob = dobInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        userRepository.getUserByEmail(email, new UserRepository.Callback<User>() {
            @Override
            public void onResult(User existingUser) {
                if (existingUser != null) {
                    emailInput.requestFocus();
                    emailLayout.setError(getString(R.string.email_already_registered));
                    return;
                }
                userRepository.getUserByTzNumber(tz, new UserRepository.Callback<User>() {
                    @Override
                    public void onResult(User existingByTz) {
                        if (existingByTz != null) {
                            tzInput.requestFocus();
                            tzLayout.setError(getString(R.string.id_already_registered));
                            return;
                        }
                        String hashedPassword = PasswordUtils.hash(password, email);
                        User user = new User(tz, name, email, phone, dob, hashedPassword);
                        userRepository.insert(user, new UserRepository.Callback<Long>() {
                            @Override
                            public void onResult(Long userId) {
                                if (userId != -1) {
                                    preferencesManager.setLoggedInUserId(userId);
                                    Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
                                    showThemeDialog();
                                } else {
                                    Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                });
            }
        });
    }
    private void showThemeDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_theme_picker, null);
        updateThemeDialogSelection(dialogView, preferencesManager.getThemeStyle());
        ImageView blue = dialogView.findViewById(R.id.dialog_ts_blue);
        ImageView green = dialogView.findViewById(R.id.dialog_ts_green);
        ImageView purple = dialogView.findViewById(R.id.dialog_ts_purple);
        ImageView orange = dialogView.findViewById(R.id.dialog_ts_orange);
        blue.setOnClickListener(v -> selectThemeLive(dialogView, "blue"));
        green.setOnClickListener(v -> selectThemeLive(dialogView, "green"));
        purple.setOnClickListener(v -> selectThemeLive(dialogView, "purple"));
        orange.setOnClickListener(v -> selectThemeLive(dialogView, "orange"));
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.theme_style_section)
                .setMessage(R.string.choose_theme_prompt)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> promptBiometricSetup())
                .setNegativeButton(R.string.later, (dialog, which) -> promptBiometricSetup())
                .setOnCancelListener(dialog -> promptBiometricSetup())
                .show();
    }
    private void selectThemeLive(View dialogView, String style) {
        preferencesManager.setThemeStyle(style);
        updateThemeDialogSelection(dialogView, style);
        sThemeDialogPending = true;
        requireActivity().recreate();
    }
    private void updateThemeDialogSelection(View dialogView, String style) {
        applySwatchState(dialogView.findViewById(R.id.dialog_ts_blue), "blue".equals(style));
        applySwatchState(dialogView.findViewById(R.id.dialog_ts_green), "green".equals(style));
        applySwatchState(dialogView.findViewById(R.id.dialog_ts_purple), "purple".equals(style));
        applySwatchState(dialogView.findViewById(R.id.dialog_ts_orange), "orange".equals(style));
    }
    private void applySwatchState(ImageView swatch, boolean selected) {
        if (swatch == null) return;
        swatch.setImageResource(selected ? R.drawable.ic_check : 0);
    }
    private void promptBiometricSetup() {
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        if (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                == BiometricManager.BIOMETRIC_SUCCESS) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.biometric_lock)
                    .setMessage(R.string.enable_biometric_prompt)
                    .setPositiveButton(R.string.yes, (dialog, which) ->
                            authenticateToEnableBiometrics())
                    .setNegativeButton(R.string.later, (dialog, which) ->
                            finishRegistration())
                    .setOnCancelListener(dialog ->
                            finishRegistration())
                    .show();
        } else {
            finishRegistration();
        }
    }
    private void finishRegistration() {
        Navigation.findNavController(rootView).navigate(R.id.action_register_to_dashboard);
    }
    private void authenticateToEnableBiometrics() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        preferencesManager.setBiometricEnabled(true);
                        preferencesManager.setBiometricTimeout("immediate");
                        finishRegistration();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        if (isAdded()) {
                            finishRegistration();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        if (isAdded()) {
                            finishRegistration();
                        }
                    }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.enable_biometric_title))
                .setSubtitle(getString(R.string.enable_biometric_subtitle))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) {
                return;
            }
            biometricPrompt.authenticate(info);
        }, 350);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (biometricPrompt != null) {
            biometricPrompt.cancelAuthentication();
            biometricPrompt = null;
        }
    }
    private boolean isAtLeast18(String dateStr) {
        try {
            String[] parts = dateStr.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            Calendar birthDate = Calendar.getInstance();
            birthDate.set(Calendar.YEAR, year);
            birthDate.set(Calendar.MONTH, month - 1);
            birthDate.set(Calendar.DAY_OF_MONTH, day);
            Calendar eighteenYearsAgo = Calendar.getInstance();
            eighteenYearsAgo.add(Calendar.YEAR, -18);
            return !birthDate.after(eighteenYearsAgo);
        } catch (Exception e) {
            return false;
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
        } else if (!ValidationUtils.isValidIsraeliId(tz)) {
            tzLayout.setError(getString(R.string.invalid_id));
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
        } else if (!isAtLeast18(dob)) {
            dobLayout.setError(getString(R.string.invalid_age));
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
            Snackbar.make(rootView, R.string.fix_highlighted_fields, Snackbar.LENGTH_SHORT).show();
        }
        return valid;
    }
}

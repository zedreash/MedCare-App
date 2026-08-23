package com.medcare.app.ui.settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.medcare.app.R;
import com.medcare.app.data.MockDataSeeder;
import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.PreferencesManager;
import java.util.concurrent.Executor;
public class SettingsFragment extends Fragment {
    private PreferencesManager preferencesManager;
    private View rootView;
    private RadioGroup themeGroup;
    private ImageView themeStyleBlue;
    private ImageView themeStyleGreen;
    private ImageView themeStylePurple;
    private ImageView themeStyleOrange;
    private RadioGroup languageGroup;
    private TextInputLayout durationLayout;
    private EditText durationInput;
    private TextView patientSortValue;
    private TextView appointmentSortValue;
    private SwitchMaterial biometricSwitch;
    private View biometricTimeoutLayout;
    private TextView biometricTimeoutValue;
    private SwitchMaterial developerSwitch;
    private View developerOptionsLayout;
    private TextView appVersionValue;
    private TextView accountIdValue;
    private String[] timeoutKeys = {"immediate", "1min", "5min", "15min"};
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        preferencesManager = new PreferencesManager(requireContext());
        initViews(view);
        loadSettings();
        setupListeners();
    }
    private void initViews(View view) {
        themeGroup = view.findViewById(R.id.theme_group);
        themeStyleBlue = view.findViewById(R.id.theme_style_blue);
        themeStyleGreen = view.findViewById(R.id.theme_style_green);
        themeStylePurple = view.findViewById(R.id.theme_style_purple);
        themeStyleOrange = view.findViewById(R.id.theme_style_orange);
        languageGroup = view.findViewById(R.id.language_group);
        durationLayout = view.findViewById(R.id.duration_layout);
        durationInput = view.findViewById(R.id.duration_input);
        patientSortValue = view.findViewById(R.id.patient_sort_value);
        appointmentSortValue = view.findViewById(R.id.appointment_sort_value);
        biometricSwitch = view.findViewById(R.id.biometric_switch);
        biometricTimeoutLayout = view.findViewById(R.id.biometric_timeout_layout);
        biometricTimeoutValue = view.findViewById(R.id.biometric_timeout_value);
        developerSwitch = view.findViewById(R.id.developer_switch);
        developerOptionsLayout = view.findViewById(R.id.developer_options_layout);
        appVersionValue = view.findViewById(R.id.app_version_value);
        accountIdValue = view.findViewById(R.id.account_id_value);
        view.findViewById(R.id.back_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.clear_data_button).setOnClickListener(v -> onClearDataClicked());
        view.findViewById(R.id.patient_sort_card).setOnClickListener(v ->
                showSortDialog(true));
        view.findViewById(R.id.appointment_sort_card).setOnClickListener(v ->
                showSortDialog(false));
        view.findViewById(R.id.fill_mock_data_button).setOnClickListener(v -> onFillMockDataClicked());
        view.findViewById(R.id.clear_sample_data_button).setOnClickListener(v -> onClearSampleDataClicked());
        view.findViewById(R.id.reset_settings_button).setOnClickListener(v -> onResetSettingsClicked());
    }
    private void updateThemeStyleSelection(String style) {
        applySwatchState(themeStyleBlue, "blue".equals(style));
        applySwatchState(themeStyleGreen, "green".equals(style));
        applySwatchState(themeStylePurple, "purple".equals(style));
        applySwatchState(themeStyleOrange, "orange".equals(style));
    }
    private void applySwatchState(ImageView swatch, boolean selected) {
        if (swatch == null) return;
        swatch.setImageResource(selected ? R.drawable.ic_check : 0);
    }
    private void selectThemeStyle(String style) {
        if (style.equals(preferencesManager.getThemeStyle())) {
            updateThemeStyleSelection(style);
            return;
        }
        preferencesManager.setThemeStyle(style);
        updateThemeStyleSelection(style);
        requireActivity().recreate();
    }
    private void loadSettings() {
        String theme = preferencesManager.getThemeMode();
        switch (theme) {
            case "light":
                themeGroup.check(R.id.theme_light);
                break;
            case "dark":
                themeGroup.check(R.id.theme_dark);
                break;
            default:
                themeGroup.check(R.id.theme_system);
                break;
        }
        updateThemeStyleSelection(preferencesManager.getThemeStyle());
        String lang = preferencesManager.getLanguage();
        switch (lang) {
            case "en":
                languageGroup.check(R.id.lang_en);
                break;
            case "ar":
                languageGroup.check(R.id.lang_ar);
                break;
            case "he":
                languageGroup.check(R.id.lang_he);
                break;
            default:
                languageGroup.check(R.id.lang_system);
                break;
        }
        durationInput.setText(String.valueOf(preferencesManager.getDefaultAppointmentDuration()));
        updateSortDisplay(true);
        updateSortDisplay(false);
        boolean biometricEnabled = preferencesManager.isBiometricEnabled();
        biometricSwitch.setChecked(biometricEnabled);
        biometricTimeoutLayout.setVisibility(biometricEnabled ? View.VISIBLE : View.GONE);
        updateBiometricTimeoutDisplay();

        boolean devMode = preferencesManager.isDeveloperMode();
        developerSwitch.setChecked(devMode);
        developerOptionsLayout.setVisibility(devMode ? View.VISIBLE : View.GONE);
        String version = "";
        try {
            version = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {}
        appVersionValue.setText(version);
        accountIdValue.setText(String.valueOf(preferencesManager.getLoggedInUserId()));
    }
    private void setupListeners() {
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String mode;
            if (checkedId == R.id.theme_light) {
                mode = "light";
            } else if (checkedId == R.id.theme_dark) {
                mode = "dark";
            } else {
                mode = "system";
            }
            preferencesManager.setThemeMode(mode);
            requireActivity().recreate();
        });
        themeStyleBlue.setOnClickListener(v -> selectThemeStyle("blue"));
        themeStyleGreen.setOnClickListener(v -> selectThemeStyle("green"));
        themeStylePurple.setOnClickListener(v -> selectThemeStyle("purple"));
        themeStyleOrange.setOnClickListener(v -> selectThemeStyle("orange"));
        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String lang;
            if (checkedId == R.id.lang_en) {
                lang = "en";
            } else if (checkedId == R.id.lang_ar) {
                lang = "ar";
            } else if (checkedId == R.id.lang_he) {
                lang = "he";
            } else {
                lang = "system";
            }
            preferencesManager.setLanguage(lang);
            requireActivity().recreate();
        });
        biometricSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                BiometricManager bm = BiometricManager.from(requireContext());
                if (bm.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        != BiometricManager.BIOMETRIC_SUCCESS) {
                    buttonView.setChecked(false);
                    Snackbar.make(rootView, R.string.biometric_not_available, Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (preferencesManager.isBiometricEnabled()) {
                    biometricTimeoutLayout.setVisibility(View.VISIBLE);
                    return;
                }
                requestBiometricToEnable(buttonView);
            } else {
                requestBiometricToDisable(buttonView);
            }
        });
        biometricTimeoutValue.setOnClickListener(v -> showTimeoutDialog());
        developerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setDeveloperMode(isChecked);
            developerOptionsLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        durationInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveDurationSetting();
        });
        durationInput.setOnEditorActionListener((v, actionId, event) -> {
            saveDurationSetting();
            return true;
        });
    }
    private void requestBiometricToEnable(android.widget.CompoundButton buttonView) {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        preferencesManager.setBiometricEnabled(true);
                        biometricTimeoutLayout.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        buttonView.setChecked(false);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        buttonView.setChecked(false);
                    }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.enable_biometric_title))
                .setSubtitle(getString(R.string.enable_biometric_subtitle))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        prompt.authenticate(info);
    }
    private void requestBiometricToDisable(android.widget.CompoundButton buttonView) {
        if (!preferencesManager.isBiometricEnabled()) return;
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        preferencesManager.setBiometricEnabled(false);
                        biometricTimeoutLayout.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        buttonView.setChecked(true);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        buttonView.setChecked(true);
                    }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.disable_biometric_title))
                .setSubtitle(getString(R.string.disable_biometric_subtitle))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        prompt.authenticate(info);
    }
    private void saveDurationSetting() {
        String text = durationInput.getText().toString().trim();
        if (text.isEmpty()) return;
        try {
            int minutes = Integer.parseInt(text);
            if (minutes > 0 && minutes <= 1440) {
                preferencesManager.setDefaultAppointmentDuration(minutes);
                return;
            }
        } catch (NumberFormatException ignored) {}
        durationInput.setText(String.valueOf(preferencesManager.getDefaultAppointmentDuration()));
    }
    private void updateBiometricTimeoutDisplay() {
        String current = preferencesManager.getBiometricTimeout();
        int index = 0;
        for (int i = 0; i < timeoutKeys.length; i++) {
            if (timeoutKeys[i].equals(current)) {
                index = i;
                break;
            }
        }
        String[] labels = {
                getString(R.string.timeout_immediate),
                getString(R.string.timeout_1min),
                getString(R.string.timeout_5min),
                getString(R.string.timeout_15min)
        };
        biometricTimeoutValue.setText(labels[index]);
    }
    private void showTimeoutDialog() {
        String current = preferencesManager.getBiometricTimeout();
        String[] labels = {
                getString(R.string.timeout_immediate),
                getString(R.string.timeout_1min),
                getString(R.string.timeout_5min),
                getString(R.string.timeout_15min)
        };
        int currentIndex = 0;
        for (int i = 0; i < timeoutKeys.length; i++) {
            if (timeoutKeys[i].equals(current)) {
                currentIndex = i;
                break;
            }
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.biometric_timeout)
                .setSingleChoiceItems(labels, currentIndex, (dialog, which) -> {
                    preferencesManager.setBiometricTimeout(timeoutKeys[which]);
                    updateBiometricTimeoutDisplay();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void updateSortDisplay(boolean isPatient) {
        int mode = isPatient
                ? preferencesManager.getPatientSortMode(0)
                : preferencesManager.getAppointmentSortMode(0);
        String[] options = isPatient
                ? getResources().getStringArray(R.array.patient_sort_options)
                : getResources().getStringArray(R.array.appointment_sort_options);
        String label = options.length > mode ? options[mode] : "";
        if (isPatient) {
            patientSortValue.setText(label);
        } else {
            appointmentSortValue.setText(label);
        }
    }
    private void showSortDialog(boolean isPatient) {
        int currentMode = isPatient
                ? preferencesManager.getPatientSortMode(0)
                : preferencesManager.getAppointmentSortMode(0);
        String[] options = isPatient
                ? getResources().getStringArray(R.array.patient_sort_options)
                : getResources().getStringArray(R.array.appointment_sort_options);
        new AlertDialog.Builder(requireContext())
                .setTitle(isPatient ? R.string.default_patient_sort : R.string.default_appointment_sort)
                .setSingleChoiceItems(options, currentMode, (dialog, which) -> {
                    if (isPatient) {
                        preferencesManager.setPatientSortMode(which);
                    } else {
                        preferencesManager.setAppointmentSortMode(which);
                    }
                    updateSortDisplay(isPatient);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void showPasswordDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.margin_large);
        layout.setPadding(padding, padding, padding, padding);
        EditText passwordInput = new EditText(requireContext());
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint(getString(R.string.clear_data_password_hint) + " *");
        layout.addView(passwordInput);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_data_password_title)
                .setView(layout)
                .setPositiveButton(R.string.next, (dialog, which) -> {
                    String entered = passwordInput.getText().toString();
                    UserRepository userRepo = new UserRepository(requireContext());
                    long userId = preferencesManager.getLoggedInUserId();
                    userRepo.getUserById(userId, new UserRepository.Callback<com.medcare.app.data.entity.User>() {
                        @Override
                        public void onResult(com.medcare.app.data.entity.User user) {
                            if (user != null && com.medcare.app.utils.PasswordUtils.verify(
                                    entered, user.getEmail(), user.getPassword())) {
                                showConfirmationDialog();
                            } else {
                                Snackbar.make(rootView, R.string.password_incorrect, Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void showConfirmationDialog() {
        String confirmWord = getString(R.string.clear_data_confirm_word);
        String message = getString(R.string.clear_data_type_to_confirm, confirmWord);
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.margin_large);
        layout.setPadding(padding, padding, padding, padding);
        EditText confirmInput = new EditText(requireContext());
        confirmInput.setHint(confirmWord);
        layout.addView(confirmInput);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_all_data)
                .setMessage(message)
                .setView(layout)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    if (confirmWord.equals(confirmInput.getText().toString())) {
                        AppDatabase.closeAndResetInstance();
                        requireContext().deleteDatabase("medcare_database");
                        preferencesManager.clearSession();
                        Snackbar.make(rootView, R.string.data_cleared, Snackbar.LENGTH_SHORT).show();
                        final Activity activity = requireActivity();
                        Navigation.findNavController(rootView)
                                .navigate(R.id.action_settings_to_login);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                activity.recreate();
                            }
                        });
                    } else {
                        Snackbar.make(rootView, R.string.confirmation_mismatch, Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void onClearDataClicked() {
        showPasswordDialog();
    }

    private void onFillMockDataClicked() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.fill_mock_data)
                .setMessage(R.string.fill_mock_data_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    long ownerId = preferencesManager.getLoggedInUserId();
                    String lang = MockDataSeeder.resolveLanguage(preferencesManager);
                    MockDataSeeder.seed(requireContext(), ownerId, lang, success -> {
                        if (isAdded()) {
                            Snackbar.make(rootView,
                                    success ? R.string.mock_data_added : R.string.error_generic,
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void onClearSampleDataClicked() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_sample_data)
                .setMessage(R.string.clear_sample_data_message)
                .setPositiveButton(R.string.confirm, (dialog, which) ->
                        MockDataSeeder.clearSampleData(requireContext(), success -> {
                            if (isAdded()) {
                                Snackbar.make(rootView,
                                        success ? R.string.sample_data_removed : R.string.error_generic,
                                        Snackbar.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void onResetSettingsClicked() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.reset_settings)
                .setMessage(R.string.reset_settings_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    preferencesManager.setThemeMode("system");
                    preferencesManager.setThemeStyle("blue");
                    preferencesManager.setLanguage("system");
                    preferencesManager.setPatientSortMode(0);
                    preferencesManager.setAppointmentSortMode(0);
                    preferencesManager.setDefaultAppointmentDuration(30);
                    preferencesManager.setBiometricEnabled(false);
                    preferencesManager.setBiometricTimeout("immediate");
                    requireActivity().recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}

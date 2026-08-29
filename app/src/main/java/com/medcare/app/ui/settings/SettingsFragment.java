package com.medcare.app.ui.settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.medcare.app.R;
import com.medcare.app.data.MockDataSeeder;
import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.entity.User;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.notifications.ReminderScheduler;
import com.medcare.app.utils.BackupManager;
import com.medcare.app.utils.BackupStorage;
import com.medcare.app.utils.ClinicAutocomplete;
import com.medcare.app.utils.DataTransfer;
import com.medcare.app.utils.PassphraseUtils;
import com.medcare.app.utils.PreferencesManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    private TextView buildVersionValue;
    private TextView dbVersionValue;
    private TextView accountIdValue;
    private SwitchMaterial reminderSwitch;
    private ClinicAutocomplete clinicAutocomplete;
    private MaterialButton backupPasswordButton;
    private TextView backupPasswordStatus;
    private MaterialButton backgroundWorkButton;
    private TextView backgroundWorkStatus;
    private ChipGroup reminderLeadOptions;
    private View reminderCustomLayout;
    private EditText reminderCustomInput;
    private ChipGroup reminderCustomUnit;
    private TextView backupFrequencyValue;
    private SwitchMaterial backupSwitch;
    private View backupFrequencyOptions;
    private LinearLayout backupsListContainer;
    private View deleteAllBackupsButton;
    private ActivityResultLauncher<String> exportLauncher;
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

        rootView.getViewTreeObserver().addOnWindowFocusChangeListener(hasFocus -> {
            if (hasFocus && isAdded() && backgroundWorkButton != null
                    && backgroundWorkStatus != null) {
                checkBackgroundWorkWithRetry();
            }
        });
    }

    private void checkBackgroundWorkWithRetry() {
        final int[] attempts = {0};
        final Runnable[] poll = new Runnable[1];
        poll[0] = () -> {
            if (!isAdded() || backgroundWorkButton == null) return;
            boolean exempt = com.medcare.app.background.BackgroundScheduler
                    .isExempt(requireContext());
            refreshBackgroundWorkUi();
            if (!exempt && attempts[0] < 10) {
                attempts[0]++;
                new Handler(Looper.getMainLooper()).postDelayed(poll[0], 300);
            }
        };
        new Handler(Looper.getMainLooper()).post(poll[0]);
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
        buildVersionValue = view.findViewById(R.id.build_version_value);
        dbVersionValue = view.findViewById(R.id.db_version_value);
        accountIdValue = view.findViewById(R.id.account_id_value);
        reminderSwitch = view.findViewById(R.id.reminder_switch);
        reminderLeadOptions = view.findViewById(R.id.reminder_lead_options);
        TextInputLayout clinicLayout = view.findViewById(R.id.clinic_layout);
        clinicAutocomplete = new ClinicAutocomplete(this, clinicLayout, 501,
                new ClinicAutocomplete.Listener() {
                    @Override
                    public void onClinicPicked(ClinicAutocomplete.ClinicResult clinic) {
                        saveClinic(clinic.name, clinic.lat, clinic.lng);
                    }

                    @Override
                    public void onClinicBlur() {
                        if (clinicLayout.getEditText() != null) {
                            String text = clinicLayout.getEditText().getText().toString();
                            ClinicAutocomplete.ClinicResult sel =
                                    clinicAutocomplete.selectionForText(text);
                            saveClinic(text, sel != null ? sel.lat : null,
                                    sel != null ? sel.lng : null);
                        }
                    }
                });
        clinicAutocomplete.attach();
        reminderCustomLayout = view.findViewById(R.id.reminder_custom_layout);
        reminderCustomInput = view.findViewById(R.id.reminder_custom_input);
        reminderCustomUnit = view.findViewById(R.id.reminder_custom_unit);
        backupSwitch = view.findViewById(R.id.backup_switch);
        backupFrequencyOptions = view.findViewById(R.id.backup_frequency_options);
        backupsListContainer = view.findViewById(R.id.backups_list_container);
        deleteAllBackupsButton = view.findViewById(R.id.delete_all_backups_button);
        backupPasswordButton = view.findViewById(R.id.backup_password_button);
        backupPasswordStatus = view.findViewById(R.id.backup_password_status);
        backupPasswordButton.setOnClickListener(v -> showBackupPasswordDialog(null));
        backgroundWorkButton = view.findViewById(R.id.background_work_button);
        backgroundWorkStatus = view.findViewById(R.id.background_work_status);
        backgroundWorkButton.setOnClickListener(v -> onBackgroundWorkAction());
        view.findViewById(R.id.backup_now_button).setOnClickListener(v -> onBackupNow());
        deleteAllBackupsButton.setOnClickListener(v -> confirmDeleteAllBackups());
        view.findViewById(R.id.back_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.clear_data_button).setOnClickListener(v -> onClearDataClicked());
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument(DataTransfer.MIME_TYPE), uri -> {
                    if (uri != null) {
                        doExport(uri);
                    }
                });
        view.findViewById(R.id.export_data_button).setOnClickListener(v -> {
            if (!preferencesManager.hasBackupPassword()) {
                showBackupPasswordDialog(this::launchExportPicker);
            } else {
                launchExportPicker();
            }
        });
        view.findViewById(R.id.patient_sort_card).setOnClickListener(v ->
                showSortDialog(true));
        view.findViewById(R.id.appointment_sort_card).setOnClickListener(v ->
                showSortDialog(false));
        view.findViewById(R.id.fill_mock_data_button).setOnClickListener(v -> onFillMockDataClicked());
        view.findViewById(R.id.clear_sample_data_button).setOnClickListener(v -> onClearSampleDataClicked());
        view.findViewById(R.id.reset_settings_button).setOnClickListener(v -> onResetSettingsClicked());
        view.findViewById(R.id.view_log_button).setOnClickListener(v -> showActivityLog());
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
        int versionCode = 0;
        try {
            android.content.pm.PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            version = info.versionName;
            versionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {}
        appVersionValue.setText(version);
        buildVersionValue.setText(String.valueOf(versionCode));
        accountIdValue.setText(String.valueOf(preferencesManager.getLoggedInUserId()));

        final long ownerId = preferencesManager.getLoggedInUserId();
        new UserRepository(requireContext()).getUserById(ownerId, user -> {
            if (isAdded() && user != null && clinicAutocomplete != null) {
                clinicAutocomplete.setPreselected(user.getClinic(),
                        user.getClinicLat(), user.getClinicLng());
            }
        });

        boolean remindersEnabled = preferencesManager.isRemindersEnabled();
        reminderSwitch.setChecked(remindersEnabled);
        updateBackupPasswordUi();
        reminderLeadOptions.setVisibility(remindersEnabled ? View.VISIBLE : View.GONE);
        if (remindersEnabled) {
            reminderLeadOptions.check(leadToChipRes(preferencesManager.getReminderLeadMinutes()));
            updateReminderCustomVisibility();
        }
        loadBackupSettings();
        loadBackupsList();
        refreshBackgroundWorkUi();

        AppDatabase.getExecutor().execute(() -> {
            final int dbVersion;
            try {
                dbVersion = AppDatabase.getInstance(requireContext())
                        .getOpenHelper().getReadableDatabase().getVersion();
            } catch (Exception e) {
                return;
            }
            AppDatabase.runOnMainThread(() -> {
                if (isAdded()) {
                    dbVersionValue.setText(String.valueOf(dbVersion));
                }
            });
        });
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
        reminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setRemindersEnabled(isChecked);
            reminderLeadOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                reminderCustomLayout.setVisibility(View.GONE);
            } else {
                reminderLeadOptions.check(leadToChipRes(preferencesManager.getReminderLeadMinutes()));
                updateReminderCustomVisibility();
                if (Build.VERSION.SDK_INT >= 33
                        && !ReminderScheduler.canPostNotifications(requireContext())) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 300);
                }
            }
        });
        reminderLeadOptions.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) return;
            int chipId = checkedIds.get(0);
            Integer lead = chipToLead(chipId);
            if (lead != null) {
                reminderCustomLayout.setVisibility(View.GONE);
                preferencesManager.setReminderLeadMinutes(lead);
            } else {
                reminderCustomLayout.setVisibility(View.VISIBLE);
                prefillReminderCustom();
            }
        });
        reminderCustomInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveReminderCustom();
        });
        reminderCustomInput.setOnEditorActionListener((v, actionId, event) -> {
            saveReminderCustom();
            return true;
        });
        reminderCustomUnit.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds != null && !checkedIds.isEmpty()) saveReminderCustom();
        });
        backupSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            backupFrequencyOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && "off".equals(preferencesManager.getBackupFrequency())) {
                preferencesManager.setBackupFrequency("daily");
            }
            selectFrequencyChip(preferencesManager.getBackupFrequency());
            if (!isChecked) {
                preferencesManager.setBackupFrequency("off");
            }
            if (isChecked) {
                ensureBackupStoragePermission(() -> {});
            }
        });
        ((com.google.android.material.chip.ChipGroup) backupFrequencyOptions)
                .setOnCheckedStateChangeListener((group, checkedIds) -> {
                    if (checkedIds == null || checkedIds.isEmpty()) return;
                    int chipId = checkedIds.get(0);
                    String frequency = chipToFrequency(chipId);
                    if (frequency != null) {
                        preferencesManager.setBackupFrequency(frequency);
                    }
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
        String message = getString(R.string.clear_data_confirm) + "\n\n"
                + getString(R.string.clear_data_type_to_confirm, confirmWord);
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
                        final long ownerId = preferencesManager.getLoggedInUserId();
                        final Activity activity = requireActivity();
                        PatientRepository patientRepo = new PatientRepository(requireContext());
                        AppointmentRepository appointmentRepo = new AppointmentRepository(requireContext());
                        patientRepo.deleteAttachmentFilesForOwner(ownerId);
                        com.medcare.app.utils.PreferencesManager.avatarFileFor(requireContext(), ownerId).delete();
                        appointmentRepo.deleteAllByOwner(ownerId, new AppointmentRepository.Callback<Void>() {
                            @Override
                            public void onResult(Void result) {
                                patientRepo.deleteAllByOwner(ownerId, new PatientRepository.Callback<Void>() {
                                    @Override
                                    public void onResult(Void result) {
                                        UserRepository userRepo = new UserRepository(requireContext());
                                        userRepo.getUserById(ownerId, new UserRepository.Callback<User>() {
                                            @Override
                                            public void onResult(User user) {
                                                if (user != null) {
                                                    userRepo.delete(user, new UserRepository.Callback<Void>() {
                                                        @Override
                                                        public void onResult(Void result) {
                                                            finishClearData(activity);
                                                        }
                                                    });
                                                } else {
                                                    finishClearData(activity);
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    } else {
                        Snackbar.make(rootView, R.string.confirmation_mismatch, Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void finishClearData(final Activity activity) {
        com.medcare.app.background.BackgroundScheduler.cancel(requireContext());
        preferencesManager.clearSession();
        Snackbar.make(rootView, R.string.data_cleared, Snackbar.LENGTH_SHORT).show();
        Navigation.findNavController(rootView).navigate(R.id.action_settings_to_login);
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                activity.recreate();
            }
        });
    }

    private void onClearDataClicked() {
        showPasswordDialog();
    }

    private void launchExportPicker() {
        String name = "MedCareExport-"
                + DateFormat.format("yyyyMMdd-HHmm", System.currentTimeMillis())
                + DataTransfer.FILE_EXTENSION;
        exportLauncher.launch(name);
    }

    private void doExport(android.net.Uri uri) {
        String passphrase = preferencesManager.getBackupPassword();
        DataTransfer.exportData(requireContext(), passphrase, uri, success -> {
            if (isAdded()) {
                Snackbar.make(rootView,
                        success ? R.string.data_exported : R.string.error_generic,
                        Snackbar.LENGTH_SHORT).show();
            }
        });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (clinicAutocomplete != null) {
            clinicAutocomplete.clear();
        }
    }

    private void loadBackupSettings() {
        String frequency = preferencesManager.getBackupFrequency();
        boolean auto = !"off".equals(frequency);
        backupSwitch.setChecked(auto);
        backupFrequencyOptions.setVisibility(auto ? View.VISIBLE : View.GONE);
        if (auto) {
            selectFrequencyChip("off".equals(frequency) ? "daily" : frequency);
        }
    }

    private String chipToFrequency(int chipId) {
        if (chipId == R.id.backup_freq_hourly) return "hourly";
        if (chipId == R.id.backup_freq_daily) return "daily";
        if (chipId == R.id.backup_freq_weekly) return "weekly";
        if (chipId == R.id.backup_freq_monthly) return "monthly";
        if (chipId == R.id.backup_freq_yearly) return "yearly";
        return "daily";
    }

    private int frequencyToChipRes(String frequency) {
        switch (frequency == null ? "daily" : frequency) {
            case "hourly": return R.id.backup_freq_hourly;
            case "daily": return R.id.backup_freq_daily;
            case "weekly": return R.id.backup_freq_weekly;
            case "monthly": return R.id.backup_freq_monthly;
            case "yearly": return R.id.backup_freq_yearly;
            default: return R.id.backup_freq_daily;
        }
    }

    private void selectFrequencyChip(String frequency) {
        ((com.google.android.material.chip.ChipGroup) backupFrequencyOptions)
                .check(frequencyToChipRes(frequency));
    }

    private void onBackupNow() {
        if (!preferencesManager.hasBackupPassword()) {
            showBackupPasswordDialog(this::doBackupNow);
            return;
        }
        doBackupNow();
    }

    private void doBackupNow() {
        ensureBackupStoragePermission(() ->
                BackupManager.backupNow(requireContext(), success -> {
            if (isAdded()) {
                Snackbar.make(rootView,
                        success ? R.string.backup_created : R.string.error_generic,
                        Snackbar.LENGTH_SHORT).show();
                loadBackupsList();
            }
        }));
    }

    private void updateBackupPasswordUi() {
        if (backupPasswordStatus == null) return;
        boolean has = preferencesManager.hasBackupPassword();
        backupPasswordStatus.setText(has ? R.string.backup_password_set : R.string.backup_password_not_set);
        backupPasswordButton.setText(has ? R.string.change_backup_password : R.string.set_backup_password);
    }

    private void showBackupPasswordDialog(Runnable onSaved) {
        final boolean hasCurrent = preferencesManager.hasBackupPassword();
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = getResources().getDimensionPixelSize(R.dimen.margin_large);
        layout.setPadding(pad, pad, pad, pad);

        final TextInputLayout[] curLayout = {null};
        final EditText[] cur = {null};
        if (hasCurrent) {
            curLayout[0] = new TextInputLayout(requireContext());
            curLayout[0].setHint(getString(R.string.current_passphrase));
            curLayout[0].setPasswordVisibilityToggleEnabled(true);
            cur[0] = new EditText(requireContext());
            cur[0].setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            curLayout[0].addView(cur[0]);
            layout.addView(curLayout[0]);

            TextView hint = new TextView(requireContext());
            hint.setText(R.string.backup_passphrase_change_hint);
            hint.setTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(),
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)));
            hint.setPadding(0, 0, 0, dpToPx(4));
            layout.addView(hint);
        }

        TextInputLayout ppLayout = new TextInputLayout(requireContext());
        ppLayout.setHint(getString(R.string.passphrase));
        ppLayout.setPasswordVisibilityToggleEnabled(true);
        EditText pp = new EditText(requireContext());
        pp.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        ppLayout.addView(pp);

        TextInputLayout cpLayout = new TextInputLayout(requireContext());
        cpLayout.setHint(getString(R.string.confirm_passphrase));
        cpLayout.setPasswordVisibilityToggleEnabled(true);
        EditText cp = new EditText(requireContext());
        cp.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        cpLayout.addView(cp);

        TextView strength = new TextView(requireContext());
        strength.setPadding(0, dpToPx(4), 0, 0);

        MaterialButton generate = new MaterialButton(requireContext());
        generate.setText(R.string.passphrase_generate);
        generate.setOnClickListener(v -> {
            String g = PassphraseUtils.generate();
            pp.setText(g);
            cp.setText(g);
        });

        MaterialButton copyBtn = new MaterialButton(requireContext());
        copyBtn.setText(R.string.copy);
        copyBtn.setOnClickListener(v -> {
            String value = pp.getText() == null ? "" : pp.getText().toString().trim();
            if (!value.isEmpty()) {
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("passphrase", value));
                Snackbar.make(rootView, R.string.copied, Snackbar.LENGTH_SHORT).show();
            }
        });

        TextView saveHint = new TextView(requireContext());
        saveHint.setText(R.string.passphrase_save_hint);
        saveHint.setTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(),
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                ContextCompat.getColor(requireContext(), R.color.text_secondary)));
        saveHint.setPadding(0, dpToPx(12), 0, 0);

        MaterialCheckBox saved = new MaterialCheckBox(requireContext());
        saved.setText(R.string.passphrase_saved_confirm);

        MaterialButton save = new MaterialButton(requireContext());
        save.setText(R.string.save);

        pp.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                ppLayout.setError(null);
                int st = PassphraseUtils.strength(pp.getText().toString().trim());
                int colorRes;
                String label;
                if (st == 0) { label = getString(R.string.strength_weak); colorRes = R.color.error; }
                else if (st == 1) { label = getString(R.string.strength_okay); colorRes = R.color.text_secondary; }
                else { label = st == 2 ? getString(R.string.strength_strong) : getString(R.string.strength_very_strong); colorRes = R.color.success; }
                strength.setText(label);
                strength.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
            }
        });

        cp.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { cpLayout.setError(null); }
        });

        if (hasCurrent) {
            cur[0].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) { curLayout[0].setError(null); }
            });
        }

        layout.addView(ppLayout);
        layout.addView(cpLayout);
        layout.addView(strength);
        layout.addView(generate);
        layout.addView(copyBtn);
        layout.addView(saveHint);
        layout.addView(saved);
        layout.addView(save);

        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.backup_password)
                .setView(layout)
                .setNegativeButton(R.string.cancel, null)
                .create();
        save.setOnClickListener(v -> {
            if (curLayout[0] != null) curLayout[0].setError(null);
            ppLayout.setError(null);
            cpLayout.setError(null);
            if (hasCurrent) {
                String enteredCurrent = cur[0].getText() == null ? "" : cur[0].getText().toString().trim();
                String stored = preferencesManager.getBackupPassword();
                if (stored == null || !stored.equals(enteredCurrent)) {
                    curLayout[0].setError(getString(R.string.passphrase_incorrect));
                    return;
                }
            }
            String p = pp.getText().toString().trim();
            String cpText = cp.getText().toString().trim();
            if (!PassphraseUtils.meetsMinimum(p)) {
                ppLayout.setError(getString(R.string.passphrase_too_weak));
                return;
            }
            if (!p.equals(cpText)) {
                cpLayout.setError(getString(R.string.passphrase_mismatch));
                return;
            }
            if (!saved.isChecked()) {
                android.widget.Toast.makeText(requireContext(),
                        R.string.passphrase_saved_confirm, android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            String oldPassphrase = preferencesManager.getBackupPassword();
            preferencesManager.setBackupPassword(p);
            updateBackupPasswordUi();
            dialog.dismiss();
            com.medcare.app.utils.BackupManager.reencryptAll(
                    requireContext(), oldPassphrase, p, count -> {
                        if (isAdded() && rootView != null && count > 0) {
                            Snackbar.make(rootView, R.string.backup_passphrase_reencrypted,
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    });
            if (onSaved != null) {
                onSaved.run();
            }
        });
        dialog.show();
    }

    private Runnable pendingBackupRun;

    private void ensureBackupStoragePermission(Runnable run) {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 28
                && ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingBackupRun = run;
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 900);
            return;
        }
        run.run();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 501 && clinicAutocomplete != null) {
            clinicAutocomplete.onLocationPermissionResult(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        } else if (requestCode == 900) {
            if (pendingBackupRun != null) {
                Runnable run = pendingBackupRun;
                pendingBackupRun = null;
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    run.run();
                }
            }
        }
    }

    private void saveClinic(String value, Double lat, Double lng) {
        long ownerId = preferencesManager.getLoggedInUserId();
        UserRepository repo = new UserRepository(requireContext());
        repo.getUserById(ownerId, user -> {
            if (user != null) {
                String trimmed = value == null ? null : value.trim();
                if (trimmed != null && trimmed.isEmpty()) trimmed = null;
                user.setClinic(trimmed);
                user.setClinicLat(trimmed == null ? null : lat);
                user.setClinicLng(trimmed == null ? null : lng);
                repo.update(user, result -> {});
            }
        });
    }

    private void refreshBackgroundWorkUi() {
        if (backgroundWorkButton == null || backgroundWorkStatus == null) return;
        boolean enabled = preferencesManager.isBackgroundWorkEnabled();
        boolean exempt = com.medcare.app.background.BackgroundScheduler.isExempt(requireContext());
        if (!enabled) {
            backgroundWorkStatus.setText(R.string.background_work_off_status);
            backgroundWorkButton.setText(R.string.background_work_turn_on);
        } else if (exempt) {
            backgroundWorkStatus.setText(R.string.background_work_on_status);
            backgroundWorkButton.setText(R.string.open_settings);
        } else {
            backgroundWorkStatus.setText(R.string.background_work_needs_permission_status);
            backgroundWorkButton.setText(R.string.open_settings);
        }
    }

    private void onBackgroundWorkAction() {
        boolean enabled = preferencesManager.isBackgroundWorkEnabled();
        boolean exempt = com.medcare.app.background.BackgroundScheduler.isExempt(requireContext());
        if (!enabled) {
            preferencesManager.setBackgroundWorkEnabled(true);
            com.medcare.app.background.BackgroundScheduler.rescheduleAll(requireContext());
            if (exempt) {
                refreshBackgroundWorkUi();
            } else {
                openBatterySettings();
            }
        } else if (!exempt) {
            openBatterySettings();
        } else {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.background_work_off_title)
                    .setMessage(R.string.background_work_off_confirm)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        preferencesManager.setBackgroundWorkEnabled(false);
                        com.medcare.app.background.BackgroundScheduler.cancel(requireContext());
                        refreshBackgroundWorkUi();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    private void openBatterySettings() {
        android.content.Context ctx = requireContext();
        if (com.medcare.app.background.BackgroundScheduler.isExempt(ctx)) {
            refreshBackgroundWorkUi();
            return;
        }
        android.net.Uri packageUri = android.net.Uri.parse("package:" + ctx.getPackageName());
        try {
            startActivity(new android.content.Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri));
            return;
        } catch (android.content.ActivityNotFoundException ignored) {}
        try {
            startActivity(new android.content.Intent(
                    android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            return;
        } catch (android.content.ActivityNotFoundException ignored) {}
        try {
            startActivity(new android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri));
        } catch (android.content.ActivityNotFoundException ignored) {
            Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (backgroundWorkButton != null && backgroundWorkStatus != null) {
            checkBackgroundWorkWithRetry();
        }
    }

    private void loadBackupsList() {
        final android.content.Context context = requireContext();
        AppDatabase.getExecutor().execute(() -> {
            List<BackupStorage.BackupFile> backups = BackupManager.list(context);
            BackupManager.withAccountEmails(context, backups);
            AppDatabase.runOnMainThread(() -> {
                if (!isAdded()) return;
                backupsListContainer.removeAllViews();
                if (backups.isEmpty()) {
                    backupsListContainer.setVisibility(View.GONE);
                    deleteAllBackupsButton.setVisibility(View.GONE);
                } else {
                    backupsListContainer.setVisibility(View.VISIBLE);
                    deleteAllBackupsButton.setVisibility(View.VISIBLE);
                    SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    for (BackupStorage.BackupFile backup : backups) {
                        backupsListContainer.addView(createBackupRow(backup, fmt));
                    }
                }
            });
        });
    }

    private View createBackupRow(BackupStorage.BackupFile backup, SimpleDateFormat fmt) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_backup, backupsListContainer, false);
        ((TextView) row.findViewById(R.id.backup_item_name)).setText(backup.name);
        String subtitle = fmt.format(new Date(backup.dateMillis)) + "  \u00B7  " + formatSize(backup.size);
        if (backup.email != null && !backup.email.isEmpty()) {
            subtitle += "  \u00B7  " + backup.email;
        }
        ((TextView) row.findViewById(R.id.backup_item_date)).setText(subtitle);
        row.findViewById(R.id.backup_item_restore).setOnClickListener(v -> confirmRestoreBackup(backup));
        row.findViewById(R.id.backup_item_delete).setOnClickListener(v -> confirmDeleteBackup(backup));
        return row;
    }

    private String formatSize(long bytes) {
        if (bytes >= 1048576) {
            return String.format(Locale.getDefault(), "%.1f MB", bytes / 1048576.0);
        }
        if (bytes >= 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void confirmDeleteBackup(BackupStorage.BackupFile backup) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_backup_confirm)
                .setPositiveButton(R.string.confirm, (dialog, which) ->
                        verifyAccountPassword(() -> doDeleteBackup(backup)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void doDeleteBackup(BackupStorage.BackupFile backup) {
        BackupManager.delete(requireContext(), backup);
        Snackbar.make(rootView, R.string.backup_deleted, Snackbar.LENGTH_SHORT).show();
        loadBackupsList();
    }

    private void confirmRestoreBackup(BackupStorage.BackupFile backup) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.restore_from_backup)
                .setMessage(R.string.backup_restore_confirm)
                .setPositiveButton(R.string.confirm, (dialog, which) ->
                        verifyAccountPassword(() -> restoreWithPassphrase(backup)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void verifyAccountPassword(Runnable onSuccess) {
        EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(getString(R.string.password));
        int pad = getResources().getDimensionPixelSize(R.dimen.margin_large);
        input.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.password_required)
                .setMessage(R.string.enter_password_to_continue)
                .setView(input)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String entered = input.getText().toString();
                    new UserRepository(requireContext()).getUserById(
                            preferencesManager.getLoggedInUserId(), user -> {
                        if (user != null && com.medcare.app.utils.PasswordUtils.verify(
                                entered, user.getEmail(), user.getPassword())) {
                            onSuccess.run();
                        } else if (isAdded()) {
                            Snackbar.make(rootView, R.string.password_incorrect, Snackbar.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void restoreWithPassphrase(BackupStorage.BackupFile backup) {
        BackupManager.restore(requireContext(), backup, preferencesManager.getBackupPassword(), result -> {
            if (!isAdded()) return;
            if (result != null && result.userId != -1) {
                Snackbar.make(rootView, R.string.backup_restored, Snackbar.LENGTH_SHORT).show();
                final Activity activity = requireActivity();
                Navigation.findNavController(rootView)
                        .navigate(R.id.action_settings_to_dashboard);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        activity.recreate();
                    }
                });
            } else {
                promptBackupPassphrase(backup);
            }
        });
    }

    private void promptBackupPassphrase(BackupStorage.BackupFile backup) {
        EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(getString(R.string.passphrase));
        int pad = getResources().getDimensionPixelSize(R.dimen.margin_large);
        input.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.passphrase)
                .setMessage(R.string.passphrase_restore_hint)
                .setView(input)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String p = input.getText().toString().trim();
                    BackupManager.restore(requireContext(), backup, p, result -> {
                        if (!isAdded()) return;
                        if (result != null && result.userId != -1) {
                            Snackbar.make(rootView, R.string.backup_restored, Snackbar.LENGTH_SHORT).show();
                            final Activity activity = requireActivity();
                            Navigation.findNavController(rootView)
                                    .navigate(R.id.action_settings_to_dashboard);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (!activity.isFinishing() && !activity.isDestroyed()) {
                                    activity.recreate();
                                }
                            });
                        } else {
                            Snackbar.make(rootView, R.string.passphrase_wrong, Snackbar.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDeleteAllBackups() {
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
                .setTitle(R.string.delete_all_backups)
                .setMessage(message)
                .setView(layout)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    if (confirmWord.equals(confirmInput.getText().toString())) {
                        verifyAccountPassword(this::doDeleteAllBackups);
                    } else {
                        Snackbar.make(rootView, R.string.confirmation_mismatch, Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void doDeleteAllBackups() {
        BackupManager.deleteAll(requireContext());
        Snackbar.make(rootView, R.string.backup_deleted, Snackbar.LENGTH_SHORT).show();
        loadBackupsList();
    }

    private Integer chipToLead(int chipId) {
        if (chipId == R.id.reminder_15min_chip) return 15;
        if (chipId == R.id.reminder_1hour_chip) return 60;
        if (chipId == R.id.reminder_1day_chip) return 1440;
        return null;
    }

    private int leadToChipRes(int lead) {
        if (lead == 15) return R.id.reminder_15min_chip;
        if (lead == 60) return R.id.reminder_1hour_chip;
        if (lead == 1440) return R.id.reminder_1day_chip;
        return R.id.reminder_custom_chip;
    }

    private void updateReminderCustomVisibility() {
        boolean custom = reminderLeadOptions.getCheckedChipId() == R.id.reminder_custom_chip;
        reminderCustomLayout.setVisibility(custom ? View.VISIBLE : View.GONE);
        if (custom) prefillReminderCustom();
    }

    private void prefillReminderCustom() {
        int lead = preferencesManager.getReminderLeadMinutes();
        if (lead == 15 || lead == 60 || lead == 1440) lead = 90;
        int unitRes;
        int value;
        if (lead % 1440 == 0) {
            unitRes = R.id.reminder_unit_days;
            value = lead / 1440;
        } else if (lead % 60 == 0) {
            unitRes = R.id.reminder_unit_hours;
            value = lead / 60;
        } else {
            unitRes = R.id.reminder_unit_minutes;
            value = lead;
        }
        reminderCustomInput.setText(String.valueOf(value));
        reminderCustomUnit.check(unitRes);
    }

    private int unitFactor(int unitChipId) {
        if (unitChipId == R.id.reminder_unit_hours) return 60;
        if (unitChipId == R.id.reminder_unit_days) return 1440;
        return 1;
    }

    private void saveReminderCustom() {
        try {
            int value = Integer.parseInt(reminderCustomInput.getText().toString().trim());
            if (value < 1) return;
            int minutes = value * unitFactor(reminderCustomUnit.getCheckedChipId());
            if (minutes > 10080) minutes = 10080;
            preferencesManager.setReminderLeadMinutes(minutes);
            if (leadToChipRes(minutes) != R.id.reminder_custom_chip) {
                reminderLeadOptions.check(leadToChipRes(minutes));
            }
        } catch (NumberFormatException ignored) {}
    }

    private void showActivityLog() {
        final android.content.Context context = requireContext();
        final long ownerId = preferencesManager.getLoggedInUserId();
        AppDatabase.getExecutor().execute(() -> {
            List<com.medcare.app.data.entity.LogEntry> all =
                    AppDatabase.getInstance(context).logDao().getByOwner(ownerId);
            List<com.medcare.app.data.entity.LogEntry> logs =
                    all.size() > 200 ? all.subList(0, 200) : all;
            AppDatabase.runOnMainThread(() -> {
                if (!isAdded()) return;
                if (logs.isEmpty()) {
                    Snackbar.make(rootView, R.string.no_logs, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                StringBuilder sb = new StringBuilder();
                SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault());
                for (com.medcare.app.data.entity.LogEntry entry : logs) {
                    sb.append(fmt.format(new Date(entry.getTimestamp())))
                            .append("  ").append(entry.getAction())
                            .append("  ").append(entry.getEntityType())
                            .append(" #").append(entry.getEntityId() == null ? "-" : entry.getEntityId())
                            .append("\n");
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.activity_log)
                        .setMessage(sb.toString())
                        .setPositiveButton(R.string.cancel, null)
                        .show();
            });
        });
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

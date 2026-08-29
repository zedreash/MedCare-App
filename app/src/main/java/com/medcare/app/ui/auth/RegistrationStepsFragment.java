package com.medcare.app.ui.auth;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.medcare.app.R;
import com.medcare.app.data.entity.User;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.PassphraseUtils;
import com.medcare.app.utils.PreferencesManager;

import java.util.concurrent.Executor;

public class RegistrationStepsFragment extends Fragment {
    private static final int STEP_PASSPHRASE = 0;
    private static final int STEP_THEME = 1;
    private static final int STEP_BIOMETRIC = 2;
    private static final int STEP_BACKUP = 3;
    private static final int STEP_BACKGROUND = 4;

    private PreferencesManager preferencesManager;
    private View rootView;
    private ViewFlipper flipper;
    private TextView stepTitle;
    private TextView stepIndicator;
    private View backButton;
    private int currentStep = STEP_PASSPHRASE;
    private static final String KEY_CURRENT_STEP = "current_step";

    private TextInputLayout passphraseLayout;
    private TextInputLayout confirmPassphraseLayout;
    private TextInputEditText passphraseInput;
    private TextInputEditText confirmPassphraseInput;
    private TextView strengthText;
    private TextView savedValue;
    private MaterialCheckBox savedCheckbox;
    private MaterialButton passphraseNextButton;

    private ImageView themeBlue, themeGreen, themePurple, themeOrange;
    private String selectedStyle = "blue";

    private BiometricPrompt biometricPrompt;
    private boolean biometricStepAdvancing = false;
    private boolean accountCreated = false;
    private boolean themeRecreating = false;

    private UserRepository userRepository;
    private String pendingTz;
    private String pendingName;
    private String pendingEmail;
    private String pendingPhone;
    private String pendingDob;
    private String pendingHashedPassword;
    private String pendingClinic;
    private Double pendingClinicLat;
    private Double pendingClinicLng;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registration_steps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        preferencesManager = new PreferencesManager(requireContext());
        userRepository = new UserRepository(requireContext());
        String pending = preferencesManager.getPendingThemeStyle();
        selectedStyle = (pending != null && !pending.isEmpty()) ? pending : "blue";
        readPendingAccount(getArguments());

        flipper = view.findViewById(R.id.steps_flipper);
        stepTitle = view.findViewById(R.id.step_title);
        stepIndicator = view.findViewById(R.id.step_indicator);
        backButton = view.findViewById(R.id.back_button);

        passphraseLayout = view.findViewById(R.id.passphrase_layout);
        confirmPassphraseLayout = view.findViewById(R.id.confirm_passphrase_layout);
        passphraseInput = view.findViewById(R.id.passphrase_input);
        confirmPassphraseInput = view.findViewById(R.id.confirm_passphrase_input);
        strengthText = view.findViewById(R.id.strength_text);
        savedValue = view.findViewById(R.id.saved_value);
        savedCheckbox = view.findViewById(R.id.saved_checkbox);
        passphraseNextButton = view.findViewById(R.id.passphrase_next_button);

        themeBlue = view.findViewById(R.id.step_theme_blue);
        themeGreen = view.findViewById(R.id.step_theme_green);
        themePurple = view.findViewById(R.id.step_theme_purple);
        themeOrange = view.findViewById(R.id.step_theme_orange);

        setupPassphraseStep();
        setupThemeStep();
        setupBiometricStep();
        setupBackupStep();
        setupBackgroundStep();

        backButton.setOnClickListener(v -> onBack());
        showStep(savedInstanceState == null
                ? STEP_PASSPHRASE : savedInstanceState.getInt(KEY_CURRENT_STEP, STEP_PASSPHRASE));

        rootView.getViewTreeObserver().addOnWindowFocusChangeListener(hasFocus -> {
            if (hasFocus && isAdded() && flipper != null
                    && flipper.getDisplayedChild() == STEP_BACKGROUND) {
                checkBackgroundExemptionWithRetry();
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_STEP, currentStep);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (rootView != null && flipper != null
                && flipper.getDisplayedChild() == STEP_BACKGROUND) {
            checkBackgroundExemptionWithRetry();
        }
    }

    private void checkBackgroundExemptionWithRetry() {
        final int[] attempts = {0};
        final Runnable[] poll = new Runnable[1];
        poll[0] = () -> {
            if (!isAdded() || flipper == null
                    || flipper.getDisplayedChild() != STEP_BACKGROUND) return;
            boolean exempt = com.medcare.app.background.BackgroundScheduler
                    .isExempt(requireContext());
            refreshBackgroundStatus();
            if (!exempt && attempts[0] < 10) {
                attempts[0]++;
                new Handler(Looper.getMainLooper()).postDelayed(poll[0], 300);
            }
        };
        new Handler(Looper.getMainLooper()).post(poll[0]);
    }

    private void readPendingAccount(@Nullable Bundle args) {
        if (args == null) return;
        pendingTz = args.getString("tz");
        pendingName = args.getString("name");
        pendingEmail = args.getString("email");
        pendingPhone = args.getString("phone");
        pendingDob = args.getString("dob");
        pendingHashedPassword = args.getString("hashedPassword");
        pendingClinic = args.getString("clinic");
        try {
            String lat = args.getString("clinicLat");
            String lng = args.getString("clinicLng");
            pendingClinicLat = lat != null ? Double.parseDouble(lat) : null;
            pendingClinicLng = lng != null ? Double.parseDouble(lng) : null;
        } catch (NumberFormatException ignored) {}
    }

    private void showStep(int step) {
        currentStep = step;
        flipper.setDisplayedChild(step);
        stepIndicator.setText(getString(R.string.step_of_format, step + 2));
        switch (step) {
            case STEP_PASSPHRASE: stepTitle.setText(R.string.passphrase_step_title); break;
            case STEP_THEME: stepTitle.setText(R.string.theme_step_title); break;
            case STEP_BIOMETRIC:
                stepTitle.setText(R.string.biometric_step_title);
                biometricStepAdvancing = false;
                break;
            case STEP_BACKGROUND:
                stepTitle.setText(R.string.background_step_title);
                refreshBackgroundStatus();
                break;
            default: stepTitle.setText(R.string.backup_step_title); break;
        }
    }

    private void onBack() {
        int step = flipper.getDisplayedChild();
        if (step == STEP_PASSPHRASE) {
            Navigation.findNavController(requireView()).navigateUp();
        } else {
            showStep(step - 1);
        }
    }

    private void setupPassphraseStep() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updatePassphraseState(); }
        };
        passphraseInput.addTextChangedListener(watcher);
        confirmPassphraseInput.addTextChangedListener(watcher);
        savedCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updatePassphraseState());

        rootView.findViewById(R.id.generate_button).setOnClickListener(v -> {
            String generated = PassphraseUtils.generate();
            passphraseInput.setText(generated);
            confirmPassphraseInput.setText(generated);
            savedValue.setText(generated);
        });

        rootView.findViewById(R.id.copy_passphrase_button).setOnClickListener(v -> {
            String value = savedValue.getText() == null ? "" : savedValue.getText().toString();
            if (!value.isEmpty()) {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) requireContext()
                        .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("passphrase", value));
                Snackbar.make(rootView, R.string.copied, Snackbar.LENGTH_SHORT).show();
            }
        });

        passphraseNextButton.setOnClickListener(v -> {
            String passphrase = passphraseInput.getText().toString().trim();
            preferencesManager.setBackupPassword(passphrase);
            showStep(STEP_THEME);
        });
        updatePassphraseState();
    }

    private void updatePassphraseState() {
        String passphrase = passphraseInput.getText() == null ? "" : passphraseInput.getText().toString().trim();
        String confirm = confirmPassphraseInput.getText() == null ? "" : confirmPassphraseInput.getText().toString().trim();
        savedValue.setText(passphrase.isEmpty() ? "" : passphrase);

        int strength = PassphraseUtils.strength(passphrase);
        int colorRes;
        String label;
        switch (strength) {
            case 0:
                label = getString(R.string.strength_weak);
                colorRes = R.color.error;
                break;
            case 1:
                label = getString(R.string.strength_okay);
                colorRes = R.color.text_secondary;
                break;
            default:
                label = strength == 2 ? getString(R.string.strength_strong)
                        : getString(R.string.strength_very_strong);
                colorRes = R.color.success;
                break;
        }
        strengthText.setText(label);
        strengthText.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), colorRes));

        if (passphrase.isEmpty()) {
            passphraseLayout.setError(null);
        } else if (!PassphraseUtils.meetsMinimum(passphrase)) {
            passphraseLayout.setError(getString(R.string.passphrase_too_weak));
        } else {
            passphraseLayout.setError(null);
        }
        confirmPassphraseLayout.setError(!passphrase.equals(confirm)
                ? getString(R.string.passphrase_mismatch) : null);

        boolean valid = PassphraseUtils.meetsMinimum(passphrase)
                && passphrase.equals(confirm)
                && savedCheckbox.isChecked();
        passphraseNextButton.setEnabled(valid);
    }

    private void setupThemeStep() {
        themeBlue.setOnClickListener(v -> selectTheme("blue"));
        themeGreen.setOnClickListener(v -> selectTheme("green"));
        themePurple.setOnClickListener(v -> selectTheme("purple"));
        themeOrange.setOnClickListener(v -> selectTheme("orange"));
        updateThemeSelection();
        rootView.findViewById(R.id.theme_next_button).setOnClickListener(v -> showStep(STEP_BIOMETRIC));
    }

    private void selectTheme(String style) {
        selectedStyle = style;
        preferencesManager.setPendingThemeStyle(style);
        updateThemeSelection();
        if (isAdded()) {
            themeRecreating = true;
            requireActivity().recreate();
        }
    }

    private void updateThemeSelection() {
        applySwatch(themeBlue, "blue".equals(selectedStyle));
        applySwatch(themeGreen, "green".equals(selectedStyle));
        applySwatch(themePurple, "purple".equals(selectedStyle));
        applySwatch(themeOrange, "orange".equals(selectedStyle));
    }

    private void applySwatch(ImageView swatch, boolean selected) {
        swatch.setImageResource(selected ? R.drawable.ic_check : 0);
    }

    private void setupBiometricStep() {
        rootView.findViewById(R.id.enable_biometric_button).setOnClickListener(v -> enableBiometrics());
        rootView.findViewById(R.id.skip_biometric_button).setOnClickListener(v -> showStep(STEP_BACKUP));
    }

    private void enableBiometrics() {
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        if (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                != BiometricManager.BIOMETRIC_SUCCESS) {
            showStep(STEP_BACKUP);
            return;
        }
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        preferencesManager.setBiometricEnabled(true);
                        preferencesManager.setBiometricTimeout("immediate");
                        advanceFromBiometric();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        biometricStepAdvancing = false;
                    }

                    @Override
                    public void onAuthenticationFailed() {
                    }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.enable_biometric_title))
                .setSubtitle(getString(R.string.enable_biometric_subtitle))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                try {
                    biometricPrompt.authenticate(info);
                } catch (Exception ignored) {
                    advanceFromBiometric();
                }
            }
        }, 350);
    }

    private void advanceFromBiometric() {
        if (biometricStepAdvancing) return;
        biometricStepAdvancing = true;
        showStep(STEP_BACKUP);
    }

    private void setupBackupStep() {
        RadioGroup group = rootView.findViewById(R.id.backup_freq_group);
        String current = preferencesManager.getBackupFrequency();
        group.check(frequencyToRadio(current));
        rootView.findViewById(R.id.backup_next_button).setOnClickListener(v -> {
            int checked = group.getCheckedRadioButtonId();
            preferencesManager.setBackupFrequency(radioToFrequency(checked));
            showStep(STEP_BACKGROUND);
        });
    }

    private void setupBackgroundStep() {
        rootView.findViewById(R.id.open_settings_button).setOnClickListener(v -> openBatterySettings());
        rootView.findViewById(R.id.open_exact_alarm_button).setOnClickListener(v -> openExactAlarmSettings());
        rootView.findViewById(R.id.background_continue_button).setOnClickListener(v -> {
            preferencesManager.setBackgroundWorkEnabled(true);
            createAccountAndFinish();
        });
        rootView.findViewById(R.id.background_skip_button).setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.background_skip_title)
                    .setMessage(R.string.background_skip_message)
                    .setPositiveButton(R.string.skip, (dialog, which) -> {
                        preferencesManager.setBackgroundWorkEnabled(false);
                        createAccountAndFinish();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void refreshBackgroundStatus() {
        TextView status = rootView.findViewById(R.id.background_status_text);
        MaterialButton openSettings = rootView.findViewById(R.id.open_settings_button);
        MaterialButton exactAlarm = rootView.findViewById(R.id.open_exact_alarm_button);
        MaterialButton continueBtn = rootView.findViewById(R.id.background_continue_button);
        if (status == null || openSettings == null) return;

        boolean exempt = com.medcare.app.background.BackgroundScheduler.isExempt(requireContext());
        boolean exact = com.medcare.app.background.BackgroundScheduler.canScheduleExact(requireContext());
        boolean ready = exempt && exact;

        if (!exempt) {
            status.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.error));
            status.setText(R.string.background_status_disabled);
        } else if (!exact) {
            status.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.error));
            status.setText(R.string.background_status_exact_needed);
        } else {
            status.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success));
            status.setText(R.string.background_status_enabled);
        }

        openSettings.setVisibility(exempt ? View.GONE : View.VISIBLE);
        openSettings.setText(getString(R.string.open_settings));

        if (Build.VERSION.SDK_INT >= 31 && !exact) {
            exactAlarm.setVisibility(View.VISIBLE);
        } else {
            exactAlarm.setVisibility(View.GONE);
        }

        continueBtn.setEnabled(ready);
    }

    private void openBatterySettings() {
        android.content.Context ctx = requireContext();
        if (com.medcare.app.background.BackgroundScheduler.isExempt(ctx)) {
            refreshBackgroundStatus();
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

    private void openExactAlarmSettings() {
        android.content.Context ctx = requireContext();
        android.net.Uri packageUri = android.net.Uri.parse("package:" + ctx.getPackageName());
        try {
            startActivity(new android.content.Intent(
                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri));
        } catch (android.content.ActivityNotFoundException ignored) {
            try {
                startActivity(new android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri));
            } catch (android.content.ActivityNotFoundException e) {
                Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private String radioToFrequency(int id) {
        if (id == R.id.freq_hourly) return "hourly";
        if (id == R.id.freq_daily) return "daily";
        if (id == R.id.freq_weekly) return "weekly";
        if (id == R.id.freq_monthly) return "monthly";
        if (id == R.id.freq_yearly) return "yearly";
        return "off";
    }

    private int frequencyToRadio(String frequency) {
        if ("hourly".equals(frequency)) return R.id.freq_hourly;
        if ("daily".equals(frequency)) return R.id.freq_daily;
        if ("weekly".equals(frequency)) return R.id.freq_weekly;
        if ("monthly".equals(frequency)) return R.id.freq_monthly;
        if ("yearly".equals(frequency)) return R.id.freq_yearly;
        return R.id.freq_off;
    }

    private void createAccountAndFinish() {
        User user = new User(pendingTz, pendingName, pendingEmail, pendingPhone,
                pendingDob, pendingHashedPassword);
        user.setClinic(pendingClinic);
        user.setClinicLat(pendingClinicLat);
        user.setClinicLng(pendingClinicLng);
        userRepository.insert(user, new UserRepository.Callback<Long>() {
            @Override
            public void onResult(Long userId) {
                if (!isAdded()) return;
                if (userId == -1) {
                    Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
                    return;
                }
                accountCreated = true;
                preferencesManager.setLoggedInUserId(userId);
                preferencesManager.setLanguageForUser(userId, preferencesManager.getLanguage());
                String pendingStyle = preferencesManager.getPendingThemeStyle();
                if (pendingStyle != null && !pendingStyle.isEmpty()) {
                    preferencesManager.setThemeStyle(pendingStyle);
                }
                preferencesManager.clearPendingThemeStyle();
                com.medcare.app.background.BackgroundScheduler.rescheduleAll(requireContext());
                requestNotificationPermissionOrFinish();
            }
        });
    }

    private void requestNotificationPermissionOrFinish() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 400);
        } else {
            goToDashboard();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 400) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            preferencesManager.setRemindersEnabled(granted);
            goToDashboard();
        }
    }

    private void goToDashboard() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_registrationSteps_to_dashboard);
        final androidx.activity.ComponentActivity activity = requireActivity();
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                activity.recreate();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (biometricPrompt != null) {
            try {
                biometricPrompt.cancelAuthentication();
            } catch (Exception ignored) {
            }
            biometricPrompt = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (accountCreated || themeRecreating) return;
        if (preferencesManager == null) return;
        boolean hadPending = preferencesManager.getPendingThemeStyle() != null;
        if (hadPending) {
            preferencesManager.clearPendingThemeStyle();
            final android.app.Activity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                new Handler(Looper.getMainLooper()).post(activity::recreate);
            }
        }
    }
}
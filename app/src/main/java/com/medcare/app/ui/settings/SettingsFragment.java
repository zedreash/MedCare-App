package com.medcare.app.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import com.medcare.app.R;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.PreferencesManager;

public class SettingsFragment extends Fragment {

    private PreferencesManager preferencesManager;
    private View rootView;

    private RadioGroup themeGroup;
    private RadioGroup languageGroup;
    private TextInputLayout durationLayout;
    private EditText durationInput;
    private TextView patientSortValue;
    private TextView appointmentSortValue;

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
        languageGroup = view.findViewById(R.id.language_group);
        durationLayout = view.findViewById(R.id.duration_layout);
        durationInput = view.findViewById(R.id.duration_input);
        patientSortValue = view.findViewById(R.id.patient_sort_value);
        appointmentSortValue = view.findViewById(R.id.appointment_sort_value);

        view.findViewById(R.id.back_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.clear_data_button).setOnClickListener(v -> onClearDataClicked());

        view.findViewById(R.id.patient_sort_card).setOnClickListener(v ->
                showSortDialog(true));
        view.findViewById(R.id.appointment_sort_card).setOnClickListener(v ->
                showSortDialog(false));
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
        passwordInput.setHint(R.string.clear_data_password_hint);
        layout.addView(passwordInput);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_data_password_title)
                .setView(layout)
                .setPositiveButton(R.string.next, (dialog, which) -> {
                    String entered = passwordInput.getText().toString();
                    UserRepository userRepo = new UserRepository(requireContext());
                    long userId = preferencesManager.getLoggedInUserId();
                    com.medcare.app.data.entity.User user = userRepo.getUserById(userId);
                    if (user != null && user.getPassword().equals(entered)) {
                        showConfirmationDialog();
                    } else {
                        Snackbar.make(rootView, R.string.password_incorrect, Snackbar.LENGTH_SHORT).show();
                    }
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
                        requireContext().deleteDatabase("medcare_database");
                        preferencesManager.clearSession();
                        Snackbar.make(rootView, R.string.data_cleared, Snackbar.LENGTH_SHORT).show();
                        Navigation.findNavController(rootView)
                                .navigate(R.id.action_settings_to_login);
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
}

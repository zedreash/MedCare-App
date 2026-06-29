package com.medcare.app.ui.profile;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
import com.medcare.app.utils.PasswordUtils;
import com.medcare.app.utils.PreferencesManager;
import com.medcare.app.utils.ValidationUtils;
import java.util.Calendar;
import java.util.Locale;
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
        view.findViewById(R.id.save_button).setOnClickListener(v -> onSaveClicked());
        view.findViewById(R.id.change_password_button).setOnClickListener(v -> onChangePasswordClicked());
        view.findViewById(R.id.logout_button).setOnClickListener(v -> onLogoutClicked());
        view.findViewById(R.id.delete_account_button).setOnClickListener(v -> onDeleteAccountClicked());
        view.findViewById(R.id.settings_button).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profile_to_settings));
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
        nameInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) nameLayout.setError(null); });
        emailInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) emailLayout.setError(null); });
        phoneInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) phoneLayout.setError(null); });
        dobInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) dobLayout.setError(null); });
    }
    private void loadUserProfile() {
        long userId = preferencesManager.getLoggedInUserId();
        if (userId == -1) {
            preferencesManager.clearSession();
            navigateToLogin();
            return;
        }
        currentUser = userRepository.getUserById(userId);
        if (currentUser == null) {
            preferencesManager.clearSession();
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
        hideKeyboard();
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

    private void onChangePasswordClicked() {
        android.view.View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);

        com.google.android.material.textfield.TextInputLayout currentPwdLayout =
                dialogView.findViewById(R.id.current_password_layout);
        com.google.android.material.textfield.TextInputLayout newPwdLayout =
                dialogView.findViewById(R.id.new_password_layout);
        com.google.android.material.textfield.TextInputLayout confirmPwdLayout =
                dialogView.findViewById(R.id.confirm_password_layout);
        EditText currentPwdInput = dialogView.findViewById(R.id.current_password_input);
        EditText newPwdInput = dialogView.findViewById(R.id.new_password_input);
        EditText confirmPwdInput = dialogView.findViewById(R.id.confirm_password_input);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_password)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, which) -> {})
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPwd = currentPwdInput.getText().toString();
            String newPwd = newPwdInput.getText().toString();
            String confirmPwd = confirmPwdInput.getText().toString();

            boolean valid = true;

            if (TextUtils.isEmpty(currentPwd)) {
                currentPwdLayout.setError(getString(R.string.field_required));
                valid = false;
            } else if (!PasswordUtils.verify(currentPwd, currentUser.getEmail(), currentUser.getPassword())) {
                currentPwdLayout.setError(getString(R.string.password_incorrect));
                valid = false;
            } else {
                currentPwdLayout.setError(null);
            }

            if (TextUtils.isEmpty(newPwd)) {
                newPwdLayout.setError(getString(R.string.field_required));
                valid = false;
            } else if (!ValidationUtils.isValidPassword(newPwd)) {
                newPwdLayout.setError(getString(R.string.password_too_short));
                valid = false;
            } else {
                newPwdLayout.setError(null);
            }

            if (TextUtils.isEmpty(confirmPwd)) {
                confirmPwdLayout.setError(getString(R.string.field_required));
                valid = false;
            } else if (!newPwd.equals(confirmPwd)) {
                confirmPwdLayout.setError(getString(R.string.password_mismatch));
                valid = false;
            } else {
                confirmPwdLayout.setError(null);
            }

            if (valid) {
                String hashed = PasswordUtils.hash(newPwd, currentUser.getEmail());
                currentUser.setPassword(hashed);
                userRepository.update(currentUser);
                dialog.dismiss();
                Snackbar.make(rootView, R.string.password_changed, Snackbar.LENGTH_SHORT).show();
            }
        });
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
    private void hideKeyboard() {
        View focused = getActivity().getCurrentFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            focused.clearFocus();
        }
    }

    private void onDeleteAccountClicked() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    userRepository.delete(currentUser);
                    preferencesManager.clearSession();
                    Snackbar.make(rootView, R.string.success_deleted, Snackbar.LENGTH_SHORT).show();
                    navigateToLogin();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void onLogoutClicked() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    preferencesManager.clearSession();
                    navigateToLogin();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void navigateToLogin() {
        Navigation.findNavController(rootView)
                .navigate(R.id.action_profile_to_login);
    }
}

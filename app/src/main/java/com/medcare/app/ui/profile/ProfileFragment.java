package com.medcare.app.ui.profile;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.medcare.app.R;
import com.medcare.app.data.entity.User;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.PasswordUtils;
import com.medcare.app.utils.AvatarUtils;
import com.medcare.app.utils.FieldHint;
import com.medcare.app.utils.PreferencesManager;
import com.medcare.app.utils.ValidationUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Locale;
public class ProfileFragment extends Fragment {
    private UserRepository userRepository;
    private PreferencesManager preferencesManager;
    private User currentUser;
    private TextView profileAvatar;
    private ImageView profileAvatarImage;
    private ActivityResultLauncher<String> pickImageLauncher;
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
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        saveProfilePhoto(uri);
                    }
                });
        View avatarContainer = view.findViewById(R.id.profile_avatar_container);
        avatarContainer.setOnClickListener(v -> showAvatarOptions());
        avatarContainer.setOnLongClickListener(v -> {
            showAvatarOptions();
            return true;
        });
        view.findViewById(R.id.save_button).setOnClickListener(v -> onSaveClicked());
        view.findViewById(R.id.change_password_button).setOnClickListener(v -> onChangePasswordClicked());
        view.findViewById(R.id.logout_button).setOnClickListener(v -> onLogoutClicked());
        view.findViewById(R.id.delete_account_button).setOnClickListener(v -> onDeleteAccountClicked());
        view.findViewById(R.id.settings_button).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profile_to_settings));
    }
    private void initViews(View view) {
        profileAvatar = view.findViewById(R.id.profile_avatar);
        profileAvatarImage = view.findViewById(R.id.profile_avatar_image);
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
        FieldHint.required(nameLayout, R.string.full_name);
        FieldHint.required(emailLayout, R.string.email);
        FieldHint.required(phoneLayout, R.string.phone);
        FieldHint.required(dobLayout, R.string.date_of_birth);
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
        userRepository.getUserById(userId, new UserRepository.Callback<User>() {
            @Override
            public void onResult(User user) {
                currentUser = user;
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
                updateAvatar();
                loadProfilePhoto();
            }
        });
    }
    private void updateAvatar() {
        if (profileAvatar == null || currentUser == null) return;
        String name = currentUser.getFullName();
        profileAvatar.setText(AvatarUtils.getInitials(name));
    }
    private void loadProfilePhoto() {
        if (profileAvatarImage == null) return;
        String path = preferencesManager.getProfilePhotoPath();
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                Bitmap bitmap = decodeSampledBitmap(path, 240, 240);
                if (bitmap != null) {
                    profileAvatarImage.setImageBitmap(bitmap);
                    profileAvatarImage.setVisibility(View.VISIBLE);
                    profileAvatar.setVisibility(View.INVISIBLE);
                    return;
                }
            }
        }
        profileAvatarImage.setVisibility(View.GONE);
        profileAvatar.setVisibility(View.VISIBLE);
        updateAvatar();
    }
    private void showAvatarOptions() {
        final boolean hasPhoto = preferencesManager.getProfilePhotoPath() != null;
        final String[] options = hasPhoto
                ? new String[]{getString(R.string.change_profile_photo), getString(R.string.remove_profile_photo)}
                : new String[]{getString(R.string.change_profile_photo)};
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_photo)
                .setItems(options, (dialog, which) -> {
                    if (hasPhoto && which == 1) {
                        removeProfilePhoto();
                    } else {
                        pickImageLauncher.launch("image/*");
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void saveProfilePhoto(Uri uri) {
        try {
            File dest = new File(requireContext().getFilesDir(), "profile_avatar.jpg");
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
            }
            preferencesManager.setProfilePhotoPath(dest.getAbsolutePath());
            loadProfilePhoto();
            Snackbar.make(rootView, R.string.profile_picture_updated, Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
        }
    }
    private void removeProfilePhoto() {
        String path = preferencesManager.getProfilePhotoPath();
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
            preferencesManager.clearProfilePhotoPath();
        }
        loadProfilePhoto();
        Snackbar.make(rootView, R.string.profile_picture_removed, Snackbar.LENGTH_SHORT).show();
    }
    private Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
        int sampleSize = 1;
        while (opts.outWidth / sampleSize > reqWidth || opts.outHeight / sampleSize > reqHeight) {
            sampleSize *= 2;
        }
        opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(path, opts);
    }
    private void onSaveClicked() {
        hideKeyboard();
        if (!validateInputs() || currentUser == null) {
            return;
        }
        String name = nameInput.getText().toString().trim();
        final String newEmail = emailInput.getText().toString().trim().toLowerCase();
        String phone = phoneInput.getText().toString().trim();
        String dob = dobInput.getText().toString().trim();
        String oldEmail = currentUser.getEmail();
        if (oldEmail != null && oldEmail.equalsIgnoreCase(newEmail)) {
            applyProfileUpdate(name, newEmail, phone, dob);
        } else {
            userRepository.getUserByEmail(newEmail, new UserRepository.Callback<User>() {
                @Override
                public void onResult(User existing) {
                    if (existing != null && existing.getId() != currentUser.getId()) {
                        emailInput.requestFocus();
                        emailLayout.setError(getString(R.string.email_already_registered));
                        return;
                    }
                    promptEmailChangePassword(name, oldEmail, newEmail, phone, dob);
                }
            });
        }
    }
    private void applyProfileUpdate(String name, String email, String phone, String dob) {
        currentUser.setFullName(name);
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        currentUser.setDateOfBirth(dob);
        updateAvatar();
        userRepository.update(currentUser, new UserRepository.Callback<Void>() {
            @Override
            public void onResult(Void result) {
                Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
            }
        });
    }
    private void promptEmailChangePassword(String name, String oldEmail, String newEmail, String phone, String dob) {
        EditText passwordInput = new EditText(requireContext());
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint(getString(R.string.password) + " *");
        int padding = getResources().getDimensionPixelSize(R.dimen.margin_large);
        passwordInput.setPadding(padding, padding, padding, padding);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_password)
                .setMessage(R.string.email_change_password_message)
                .setView(passwordInput)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String entered = passwordInput.getText().toString();
                    if (PasswordUtils.verify(entered, oldEmail, currentUser.getPassword())) {
                        currentUser.setPassword(PasswordUtils.hash(entered, newEmail));
                        applyProfileUpdate(name, newEmail, phone, dob);
                    } else {
                        Snackbar.make(rootView, R.string.password_incorrect, Snackbar.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void onChangePasswordClicked() {
        if (currentUser == null) return;
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

        FieldHint.required(currentPwdLayout, R.string.current_password);
        FieldHint.required(newPwdLayout, R.string.new_password);
        FieldHint.required(confirmPwdLayout, R.string.confirm_password);

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
                userRepository.update(currentUser, new UserRepository.Callback<Void>() {
                    @Override
                    public void onResult(Void result) {
                        dialog.dismiss();
                        Snackbar.make(rootView, R.string.password_changed, Snackbar.LENGTH_SHORT).show();
                    }
                });
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
                    long ownerId = preferencesManager.getLoggedInUserId();
                    PatientRepository patientRepo = new PatientRepository(requireContext());
                    AppointmentRepository appointmentRepo = new AppointmentRepository(requireContext());
                    appointmentRepo.deleteAllByOwner(ownerId, new AppointmentRepository.Callback<Void>() {
                        @Override
                        public void onResult(Void result) {
                            patientRepo.deleteAllByOwner(ownerId, new PatientRepository.Callback<Void>() {
                                @Override
                                public void onResult(Void result) {
                                    userRepository.delete(currentUser, new UserRepository.Callback<Void>() {
                                        @Override
                                        public void onResult(Void result) {
                                            preferencesManager.clearSession();
                                            Snackbar.make(rootView, R.string.success_deleted, Snackbar.LENGTH_SHORT).show();
                                            navigateToLogin();
                                        }
                                    });
                                }
                            });
                        }
                    });
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
        final Activity activity = requireActivity();
        Navigation.findNavController(rootView)
                .navigate(R.id.action_profile_to_login);
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                activity.recreate();
            }
        });
    }
}

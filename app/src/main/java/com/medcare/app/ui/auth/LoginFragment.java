package com.medcare.app.ui.auth;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.medcare.app.R;
import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.entity.User;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.BackupManager;
import com.medcare.app.utils.BackupStorage;
import com.medcare.app.utils.DataTransfer;
import com.medcare.app.utils.FieldHint;
import com.medcare.app.utils.PreferencesManager;
import com.medcare.app.utils.ValidationUtils;
import com.medcare.app.transfer.TransferManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class LoginFragment extends Fragment {
    private UserRepository userRepository;
    private PreferencesManager preferencesManager;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private EditText emailInput;
    private EditText passwordInput;
    private View rootView;
    private ActivityResultLauncher<String[]> importLauncher;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        userRepository = new UserRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());
        initViews(view);
        setupErrorClearListeners();
        if (preferencesManager.isLoggedIn()) {
            navigateToDashboard();
            return;
        }
        view.findViewById(R.id.login_button).setOnClickListener(v -> onLoginClicked());
        view.findViewById(R.id.register_link).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_register));
        view.findViewById(R.id.login_language_button).setOnClickListener(v -> showLanguageDialog());
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri == null) return;
                    String name = queryDisplayName(uri);
                    if (name == null || !name.toLowerCase(Locale.getDefault())
                            .endsWith(DataTransfer.FILE_EXTENSION)) {
                        if (isAdded()) {
                            Snackbar.make(rootView, R.string.import_failed, Snackbar.LENGTH_LONG).show();
                        }
                        return;
                    }
                    ImportFlow.handleImport(this, uri);
                });
        view.findViewById(R.id.import_button).setOnClickListener(v ->
                importLauncher.launch(new String[]{"*/*"}));
        view.findViewById(R.id.restore_button).setOnClickListener(v -> showBackupRestorePicker());
        view.findViewById(R.id.transfer_button).setOnClickListener(v -> onTransferReceiveClicked());
        maybePromptBackupRestore();
        requestNotificationPermissionIfNeeded();
    }

    private void onTransferReceiveClicked() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.transfer_receive_title)
                .setMessage(R.string.transfer_receive_instructions)
                .setPositiveButton(R.string.confirm, (d, w) -> startTransferReceive())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 400);
        }
    }

    private void startTransferReceive() {
        TransferManager.startReceiving(this, new TransferManager.Listener() {
            @Override
            public void onStatus(String message) {
                if (isAdded()) {
                    Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onTransferDone(DataTransfer.ImportResult result) {
                if (!isAdded()) return;
                ImportFlow.handleRestoreResult(LoginFragment.this, result);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 700 && allGranted(grantResults)) {
            startTransferReceive();
        }
    }

    private boolean allGranted(int[] grantResults) {
        for (int r : grantResults) {
            if (r != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == com.medcare.app.transfer.TransferManager.REQUEST_ENABLE_BT) {
            com.medcare.app.transfer.TransferManager.onBluetoothEnableResult(this,
                    resultCode == Activity.RESULT_OK);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        TransferManager.stop();
    }

    private String queryDisplayName(Uri uri) {
        try (Cursor cursor = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    return cursor.getString(idx);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void maybePromptBackupRestore() {
        final android.content.Context context = requireContext();
        AppDatabase.getExecutor().execute(() -> {
            boolean hasUsers = !AppDatabase.getInstance(context).userDao().getAllUsers().isEmpty();
            if (hasUsers) return;
            List<BackupStorage.BackupFile> backups = BackupManager.list(context);
            if (backups.isEmpty()) return;
            BackupManager.withAccountEmails(context, backups);
            boolean anyReadable = false;
            for (BackupStorage.BackupFile b : backups) {
                if (b.email != null) {
                    anyReadable = true;
                    break;
                }
            }
            if (!anyReadable) return; // backups can't be decrypted on this device (e.g. fresh phone)
            final List<BackupStorage.BackupFile> latestPerAccount = latestPerAccount(backups);
            AppDatabase.runOnMainThread(() -> {
                if (!isAdded()) return;

                LinearLayout list = new LinearLayout(requireContext());
                list.setOrientation(LinearLayout.VERTICAL);
                int pad = getResources().getDimensionPixelSize(R.dimen.margin_large);
                list.setPadding(pad, pad, pad, pad);
                SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                for (BackupStorage.BackupFile backup : latestPerAccount) {
                    list.addView(createBackupRow(backup, fmt));
                }

                final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.restore_from_backup)
                        .setView(list)
                        .setNegativeButton(R.string.cancel, null)
                        .create();

                if (backups.size() > latestPerAccount.size()) {
                    com.google.android.material.button.MaterialButton more =
                            new com.google.android.material.button.MaterialButton(requireContext());
                    int extra = backups.size() - latestPerAccount.size();
                    more.setText(getResources().getQuantityString(
                            R.plurals.show_more_backups, extra, extra));
                    more.setIconResource(R.drawable.ic_expand_more);
                    more.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    more.setOnClickListener(v -> {
                        dialog.dismiss();
                        showBackupRestorePicker();
                    });
                    list.addView(more);
                }

                dialog.show();
            });
        });
    }

    private List<BackupStorage.BackupFile> latestPerAccount(List<BackupStorage.BackupFile> backups) {
        Map<String, BackupStorage.BackupFile> latest = new LinkedHashMap<>();
        for (BackupStorage.BackupFile b : backups) {
            String key = b.email != null ? b.email : "";
            BackupStorage.BackupFile existing = latest.get(key);
            if (existing == null || b.dateMillis > existing.dateMillis) {
                latest.put(key, b);
            }
        }
        List<BackupStorage.BackupFile> result = new ArrayList<>(latest.values());
        result.sort((a, b) -> Long.compare(b.dateMillis, a.dateMillis));
        return result;
    }

    private void showBackupRestorePicker() {
        final android.content.Context context = requireContext();
        AppDatabase.getExecutor().execute(() -> {
            List<BackupStorage.BackupFile> backups = BackupManager.list(context);
            BackupManager.withAccountEmails(context, backups);
            AppDatabase.runOnMainThread(() -> {
                if (!isAdded()) return;
                if (backups.isEmpty()) {
                    Snackbar.make(rootView, R.string.no_backups, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                LinearLayout list = new LinearLayout(requireContext());
                list.setOrientation(LinearLayout.VERTICAL);
                int pad = getResources().getDimensionPixelSize(R.dimen.margin_large);
                list.setPadding(pad, pad, pad, pad);
                SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                for (BackupStorage.BackupFile backup : backups) {
                    list.addView(createBackupRow(backup, fmt));
                }
                ScrollView scroll = new ScrollView(requireContext());
                scroll.addView(list);
                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.restore_from_backup)
                        .setView(scroll)
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        });
    }

    private View createBackupRow(BackupStorage.BackupFile backup, SimpleDateFormat fmt) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_backup_select, null, false);
        ((TextView) row.findViewById(R.id.backup_item_name)).setText(backup.name);
        String subtitle = fmt.format(new Date(backup.dateMillis)) + "  \u00B7  " + formatSize(backup.size);
        if (backup.email != null && !backup.email.isEmpty()) {
            subtitle += "  \u00B7  " + backup.email;
        }
        ((TextView) row.findViewById(R.id.backup_item_date)).setText(subtitle);
        row.setOnClickListener(v -> confirmRestoreBackup(backup));
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

    private void confirmRestoreBackup(BackupStorage.BackupFile backup) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.restore_from_backup)
                .setMessage(R.string.backup_restore_confirm)
                .setPositiveButton(R.string.confirm, (dialog, which) -> restoreWithPassphrase(backup))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void restoreWithPassphrase(BackupStorage.BackupFile backup) {
        BackupManager.restore(requireContext(), backup, preferencesManager.getBackupPassword(), result -> {
            if (!isAdded()) return;
            if (result != null) {
                ImportFlow.handleRestoreResult(this, result);
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
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.passphrase)
                .setMessage(R.string.passphrase_restore_hint)
                .setView(input)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String p = input.getText().toString().trim();
                    BackupManager.restore(requireContext(), backup, p, result -> {
                        if (!isAdded()) return;
                        if (result != null) {
                            ImportFlow.handleRestoreResult(this, result);
                        } else {
                            Snackbar.make(rootView, R.string.passphrase_wrong, Snackbar.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void initViews(View view) {
        emailLayout = view.findViewById(R.id.email_layout);
        passwordLayout = view.findViewById(R.id.password_layout);
        emailInput = view.findViewById(R.id.email_input);
        passwordInput = view.findViewById(R.id.password_input);
        FieldHint.required(emailLayout, R.string.email);
        FieldHint.required(passwordLayout, R.string.password);
    }
    private void setupErrorClearListeners() {
        emailInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) emailLayout.setError(null);
        });
        passwordInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) passwordLayout.setError(null);
        });
    }
    private void onLoginClicked() {
        hideKeyboard();
        if (!validateInputs()) {
            return;
        }
        String email = emailInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString();
        userRepository.login(email, password, new UserRepository.Callback<User>() {
            @Override
            public void onResult(User user) {
                if (!isAdded()) return;
                if (user != null) {
                    preferencesManager.setLoggedInUserId(user.getId());
                    boolean themeChanged = applyUserTheme(user.getId());
                    Snackbar.make(rootView, R.string.login_success, Snackbar.LENGTH_SHORT).show();
                    final Activity activity = requireActivity();
                    navigateToDashboard();
                    if (themeChanged) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                activity.recreate();
                            }
                        });
                    }
                } else {
                    Snackbar.make(rootView, R.string.login_invalid_credentials, Snackbar.LENGTH_LONG).show();
                    passwordLayout.setError(getString(R.string.login_invalid_credentials));
                }
            }
        });
    }
    private boolean applyUserTheme(long userId) {
        String mode = preferencesManager.getThemeModeForUser(userId);
        String style = preferencesManager.getThemeStyleForUser(userId);
        boolean changed = false;
        if (mode != null && !mode.isEmpty() && !mode.equals(preferencesManager.getThemeMode())) {
            preferencesManager.setThemeMode(mode);
            changed = true;
        }
        if (style != null && !style.isEmpty() && !style.equals(preferencesManager.getThemeStyle())) {
            preferencesManager.setThemeStyle(style);
            changed = true;
        }
        String lang = preferencesManager.getLanguageForUser(userId);
        if (lang != null && !lang.isEmpty() && !lang.equals(preferencesManager.getLanguage())) {
            preferencesManager.setLanguage(lang);
            changed = true;
        }
        return changed;
    }

    private void showLanguageDialog() {
        final String[] options = {
                getString(R.string.lang_system),
                getString(R.string.lang_english),
                getString(R.string.lang_arabic),
                getString(R.string.lang_hebrew)
        };
        final String[] values = {"system", "en", "ar", "he"};
        String current = preferencesManager.getLanguage();
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                checked = i;
                break;
            }
        }
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.language_section)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    preferencesManager.setLanguage(values[which]);
                    dialog.dismiss();
                    requireActivity().recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private boolean validateInputs() {
        boolean valid = true;
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.field_required));
            valid = false;
        } else if (!ValidationUtils.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.invalid_email));
            valid = false;
        } else {
            emailLayout.setError(null);
        }
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.field_required));
            valid = false;
        } else {
            passwordLayout.setError(null);
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

    private void navigateToDashboard() {
        Navigation.findNavController(rootView)
                .navigate(R.id.action_login_to_dashboard);
    }
}

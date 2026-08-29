package com.medcare.app.ui.auth;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.medcare.app.R;
import com.medcare.app.data.entity.User;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.DataTransfer;
import com.medcare.app.utils.PasswordUtils;
import com.medcare.app.utils.PreferencesManager;

import java.util.concurrent.Executor;

public final class ImportFlow {
    private ImportFlow() {}

    public static void handleImport(Fragment fragment, Uri uri) {
        if (fragment == null || fragment.getContext() == null) return;
        new AlertDialog.Builder(fragment.requireContext())
                .setTitle(R.string.import_data)
                .setMessage(R.string.import_warning)
                .setPositiveButton(R.string.confirm, (dialog, which) -> promptPassword(fragment, uri))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void promptPassword(Fragment fragment, Uri uri) {
        if (fragment == null || fragment.getContext() == null) return;
        EditText input = new EditText(fragment.requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(fragment.getString(R.string.passphrase));
        int pad = fragment.getResources().getDimensionPixelSize(R.dimen.margin_large);
        input.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(fragment.requireContext())
                .setTitle(R.string.import_data)
                .setMessage(R.string.import_password_message)
                .setView(input)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String password = input.getText().toString();
                    runImport(fragment, uri, password);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void runImport(Fragment fragment, Uri uri, String password) {
        DataTransfer.importData(fragment.requireContext(), uri, password, result -> {
            handleRestoreResult(fragment, result);
        });
    }

    public static void handleRestoreResult(Fragment fragment, DataTransfer.ImportResult result) {
        if (fragment == null || !fragment.isAdded()) return;
        if (result == null || result.userId == -1) {
            Snackbar.make(fragment.requireView(), R.string.import_failed, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (result.needsPassword) {
            promptSetPassword(fragment, result.userId);
            return;
        }
        if (new PreferencesManager(fragment.requireContext()).isLoggedIn()) {
            Snackbar.make(fragment.requireView(), R.string.backup_restored, Snackbar.LENGTH_SHORT).show();
            return;
        }
        Snackbar.make(fragment.requireView(), R.string.restore_login_required, Snackbar.LENGTH_LONG).show();
    }

    private static void promptSetPassword(Fragment fragment, long userId) {
        LinearLayout layout = new LinearLayout(fragment.requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = fragment.getResources().getDimensionPixelSize(R.dimen.margin_large);
        layout.setPadding(pad, pad, pad, pad);
        EditText pw = new EditText(fragment.requireContext());
        pw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pw.setHint(fragment.getString(R.string.password));
        EditText confirm = new EditText(fragment.requireContext());
        confirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirm.setHint(fragment.getString(R.string.confirm_password));
        layout.addView(pw);
        layout.addView(confirm);
        MaterialButton save = new MaterialButton(fragment.requireContext());
        save.setText(R.string.save);
        layout.addView(save);
        final AlertDialog dialog = new AlertDialog.Builder(fragment.requireContext())
                .setTitle(R.string.set_password_title)
                .setMessage(R.string.set_password_prompt)
                .setView(layout)
                .setNegativeButton(R.string.cancel, null)
                .create();
        save.setOnClickListener(v -> {
            String a = pw.getText().toString();
            String b = confirm.getText().toString();
            if (a.isEmpty() || !a.equals(b)) {
                if (fragment.isAdded()) {
                    Snackbar.make(fragment.requireView(), R.string.password_mismatch,
                            Snackbar.LENGTH_SHORT).show();
                }
                return;
            }
            dialog.dismiss();
            applyPassword(fragment, userId, a);
        });
        dialog.show();
    }

    private static void applyPassword(Fragment fragment, long userId, String password) {
        UserRepository repo = new UserRepository(fragment.requireContext());
        repo.getUserById(userId, new UserRepository.Callback<User>() {
            @Override
            public void onResult(User user) {
                if (user == null || !fragment.isAdded()) {
                    if (fragment.isAdded()) {
                        Snackbar.make(fragment.requireView(), R.string.error_generic, Snackbar.LENGTH_SHORT).show();
                    }
                    return;
                }
                user.setPassword(PasswordUtils.hash(password, user.getEmail()));
                repo.update(user, new UserRepository.Callback<Void>() {
                    @Override
                    public void onResult(Void result) {
                        if (!fragment.isAdded()) return;
                        new PreferencesManager(fragment.requireContext()).setLoggedInUserId(userId);
                        postImport(fragment, false);
                    }
                });
            }
        });
    }

    public static void postImport(Fragment fragment, boolean biometricEnabled) {
        if (biometricEnabled) {
            promptReEnroll(fragment, () -> finishImport(fragment));
        } else {
            finishImport(fragment);
        }
    }

    private static void finishImport(Fragment fragment) {
        if (!fragment.isAdded()) return;
        final Activity activity = fragment.requireActivity();
        if (fragment instanceof RegisterFragment) {
            Navigation.findNavController(fragment.requireView())
                    .navigate(R.id.action_register_to_dashboard);
        } else {
            Navigation.findNavController(fragment.requireView())
                    .navigate(R.id.action_login_to_dashboard);
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                activity.recreate();
            }
        });
    }

    private static void promptReEnroll(Fragment fragment, Runnable onDone) {
        if (!fragment.isAdded()) {
            onDone.run();
            return;
        }
        BiometricManager bm = BiometricManager.from(fragment.requireContext());
        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) {
            onDone.run();
            return;
        }
        Executor executor = ContextCompat.getMainExecutor(fragment.requireContext());
        BiometricPrompt prompt = new BiometricPrompt(fragment, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        PreferencesManager prefs = new PreferencesManager(fragment.requireContext());
                        prefs.setBiometricEnabled(true);
                        prefs.setBiometricTimeout("immediate");
                        onDone.run();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        onDone.run();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        onDone.run();
                    }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(fragment.getString(R.string.enable_biometric_title))
                .setSubtitle(fragment.getString(R.string.enable_biometric_subtitle))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        prompt.authenticate(info);
    }
}
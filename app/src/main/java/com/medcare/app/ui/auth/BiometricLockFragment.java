package com.medcare.app.ui.auth;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.medcare.app.R;
import com.medcare.app.data.entity.User;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.PreferencesManager;
import java.util.concurrent.Executor;
public class BiometricLockFragment extends Fragment {
    private BiometricPrompt biometricPrompt;
    private TextView statusText;
    private MaterialButton authenticateButton;
    private MaterialButton passwordFallbackButton;
    private View passwordLayout;
    private EditText passwordInput;
    private TextView passwordError;
    private PreferencesManager preferencesManager;
    private UserRepository userRepository;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_biometric_lock, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferencesManager = new PreferencesManager(requireContext());
        userRepository = new UserRepository(requireContext());
        statusText = view.findViewById(R.id.status_text);
        authenticateButton = view.findViewById(R.id.authenticate_button);
        passwordFallbackButton = view.findViewById(R.id.password_fallback_button);
        passwordLayout = view.findViewById(R.id.password_layout);
        passwordInput = view.findViewById(R.id.password_input);
        passwordError = view.findViewById(R.id.password_error);
        setupBiometricPrompt();
        authenticateButton.setOnClickListener(v -> showBiometricPrompt());
        passwordFallbackButton.setOnClickListener(v -> {
            passwordFallbackButton.setVisibility(View.GONE);
            passwordLayout.setVisibility(View.VISIBLE);
        });
        passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            checkPassword();
            return true;
        });
        showBiometricPrompt();
    }
    private void setupBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                onUnlocked();
            }
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                statusText.setText(errString);
                passwordFallbackButton.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAuthenticationFailed() {
                statusText.setText(R.string.biometric_failed);
            }
        });
    }
    private void showBiometricPrompt() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_unlock))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        biometricPrompt.authenticate(promptInfo);
    }
    private void checkPassword() {
        String password = passwordInput.getText().toString().trim();
        if (password.isEmpty()) {
            passwordError.setText(R.string.field_required);
            passwordError.setVisibility(View.VISIBLE);
            return;
        }
        long userId = preferencesManager.getLoggedInUserId();
        User user = userRepository.getUserById(userId);
        if (user != null && user.getPassword().equals(password)) {
            onUnlocked();
        } else {
            passwordError.setText(R.string.password_incorrect);
            passwordError.setVisibility(View.VISIBLE);
        }
    }
    private void onUnlocked() {
        preferencesManager.setLastUnlockTime(System.currentTimeMillis());
        Navigation.findNavController(requireView()).navigateUp();
    }
}

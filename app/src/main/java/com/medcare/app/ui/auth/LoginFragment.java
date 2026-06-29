package com.medcare.app.ui.auth;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.medcare.app.utils.PreferencesManager;
import com.medcare.app.utils.ValidationUtils;
public class LoginFragment extends Fragment {
    private UserRepository userRepository;
    private PreferencesManager preferencesManager;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private EditText emailInput;
    private EditText passwordInput;
    private View rootView;
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
    }
    private void initViews(View view) {
        emailLayout = view.findViewById(R.id.email_layout);
        passwordLayout = view.findViewById(R.id.password_layout);
        emailInput = view.findViewById(R.id.email_input);
        passwordInput = view.findViewById(R.id.password_input);
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
        if (!validateInputs()) {
            return;
        }
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        User user = userRepository.login(email, password);
        if (user != null) {
            preferencesManager.setLoggedInUserId(user.getId());
            Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
            navigateToDashboard();
        } else {
            Snackbar.make(rootView, R.string.login_invalid_credentials, Snackbar.LENGTH_LONG).show();
            passwordLayout.setError(getString(R.string.login_invalid_credentials));
        }
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
        } else if (!ValidationUtils.isValidPassword(password)) {
            passwordLayout.setError(getString(R.string.password_too_short));
            valid = false;
        } else {
            passwordLayout.setError(null);
        }
        return valid;
    }
    private void navigateToDashboard() {
        Navigation.findNavController(rootView)
                .navigate(R.id.action_login_to_dashboard);
    }
}

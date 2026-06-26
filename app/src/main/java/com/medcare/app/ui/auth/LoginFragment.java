package com.medcare.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.medcare.app.R;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.PreferencesManager;
import com.medcare.app.utils.ValidationUtils;

public class LoginFragment extends Fragment {

    private UserRepository userRepository;
    private PreferencesManager preferencesManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = new UserRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());

        if (preferencesManager.isLoggedIn()) {
            navigateToPatients();
        }

        view.findViewById(R.id.login_button).setOnClickListener(v -> onLoginClicked());
        view.findViewById(R.id.register_link).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_register));
    }

    private void onLoginClicked() {
    }

    private void navigateToPatients() {
    }
}

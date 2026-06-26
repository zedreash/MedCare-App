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

public class RegisterFragment extends Fragment {

    private UserRepository userRepository;
    private PreferencesManager preferencesManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = new UserRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());

        view.findViewById(R.id.register_button).setOnClickListener(v -> onRegisterClicked());
        view.findViewById(R.id.login_link).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_register_to_login));
    }

    private void onRegisterClicked() {
    }
}

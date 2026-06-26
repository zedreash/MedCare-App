package com.medcare.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.medcare.app.R;
import com.medcare.app.data.entity.User;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.PreferencesManager;

public class ProfileFragment extends Fragment {

    private UserRepository userRepository;
    private PreferencesManager preferencesManager;
    private User currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = new UserRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());

        loadUserProfile();

        view.findViewById(R.id.save_button).setOnClickListener(v -> onSaveClicked());
        view.findViewById(R.id.logout_button).setOnClickListener(v -> onLogoutClicked());
    }

    private void loadUserProfile() {
    }

    private void onSaveClicked() {
    }

    private void onLogoutClicked() {
        preferencesManager.clearSession();
        Navigation.findNavController(requireView())
                .navigate(R.id.action_profile_to_login);
    }
}

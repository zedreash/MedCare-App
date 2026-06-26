package com.medcare.app.ui.patients;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.medcare.app.R;
import com.medcare.app.data.repository.PatientRepository;

public class PatientFormFragment extends Fragment {

    private PatientRepository patientRepository;
    private long patientId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientId = getArguments().getInt("patientId", -1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        patientRepository = new PatientRepository(requireContext());

        if (patientId != -1) {
            loadPatient();
        }

        view.findViewById(R.id.save_button).setOnClickListener(v -> onSaveClicked());
    }

    private void loadPatient() {
    }

    private void onSaveClicked() {
    }
}

package com.medcare.app.ui.patients;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.medcare.app.R;
import com.medcare.app.adapter.PatientAdapter;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.PatientRepository;

import java.util.List;

public class PatientListFragment extends Fragment {

    private PatientRepository patientRepository;
    private PatientAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyStateText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        patientRepository = new PatientRepository(requireContext());

        recyclerView = view.findViewById(R.id.patient_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new PatientAdapter(this::onPatientClicked);
        recyclerView.setAdapter(adapter);

        loadPatients();

        view.findViewById(R.id.add_patient_button).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("patientId", -1);
            Navigation.findNavController(view).navigate(R.id.action_patientList_to_patientForm, args);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPatients();
    }

    private void loadPatients() {
        List<Patient> patients = patientRepository.getAllPatients();
        adapter.setPatients(patients);

        if (patients.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void onPatientClicked(Patient patient) {
        Bundle args = new Bundle();
        args.putInt("patientId", (int) patient.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_patientList_to_patientForm, args);
    }
}

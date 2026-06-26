package com.medcare.app.ui.patients;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    private List<Patient> patientList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        patientRepository = new PatientRepository(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.patient_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new PatientAdapter(this::onPatientClicked);
        recyclerView.setAdapter(adapter);

        loadPatients();

        view.findViewById(R.id.add_patient_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_patientList_to_patientForm));

        view.findViewById(R.id.profile_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_patientList_to_profile));
    }

    private void loadPatients() {
    }

    private void onPatientClicked(Patient patient) {
    }
}

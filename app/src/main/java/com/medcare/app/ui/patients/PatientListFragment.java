package com.medcare.app.ui.patients;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.List;

public class PatientListFragment extends Fragment {

    private PatientRepository patientRepository;
    private PatientAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private EditText searchEditText;
    private List<Patient> allPatients = new ArrayList<>();

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
        searchEditText = view.findViewById(R.id.search_edit_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new PatientAdapter(this::onPatientClicked);
        recyclerView.setAdapter(adapter);

        loadPatients();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPatients(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

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
        allPatients = patientRepository.getAllPatients();
        filterPatients(searchEditText.getText().toString());
    }

    private void filterPatients(String query) {
        if (query == null) query = "";
        query = query.trim().toLowerCase();

        List<Patient> filtered;
        if (query.isEmpty()) {
            filtered = allPatients;
        } else {
            filtered = new ArrayList<>();
            for (Patient p : allPatients) {
                if (p.getFullName().toLowerCase().contains(query) ||
                    (p.getPhone() != null && p.getPhone().toLowerCase().contains(query)) ||
                    (p.getDiagnosis() != null && p.getDiagnosis().toLowerCase().contains(query))) {
                    filtered.add(p);
                }
            }
        }

        adapter.setPatients(filtered);

        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
            if (allPatients.isEmpty()) {
                emptyStateText.setText(R.string.no_patients);
            } else {
                emptyStateText.setText(R.string.no_search_results);
            }
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

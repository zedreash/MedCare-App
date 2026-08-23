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
import com.google.android.material.snackbar.Snackbar;
import com.medcare.app.R;
import com.medcare.app.adapter.PatientAdapter;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.utils.PreferencesManager;
import java.util.ArrayList;
import java.util.List;
public class PatientListFragment extends Fragment {
    private static final int SORT_NEWEST = 0;
    private static final int SORT_OLDEST = 1;
    private static final int SORT_NAME_AZ = 2;
    private static final int SORT_NAME_ZA = 3;
    private PatientRepository patientRepository;
    private AppointmentRepository appointmentRepository;
    private PatientAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private View emptyStateContainer;
    private EditText searchEditText;
    private List<Patient> allPatients = new ArrayList<>();
    private int currentSortMode = SORT_NEWEST;
    private PreferencesManager preferencesManager;
    private View rootView;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_list, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        patientRepository = new PatientRepository(requireContext());
        appointmentRepository = new AppointmentRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());
        currentSortMode = preferencesManager.getPatientSortMode(SORT_NEWEST);
        recyclerView = view.findViewById(R.id.patient_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        searchEditText = view.findViewById(R.id.search_edit_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PatientAdapter(this::onPatientClicked, this::onPatientDeleteClicked);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    adapter.closeRevealed();
                }
            }
        });
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
        view.findViewById(R.id.sort_button).setOnClickListener(v -> showSortDialog());
        view.findViewById(R.id.add_patient_button).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("patientId", -1);
            Navigation.findNavController(view).navigate(R.id.action_patientList_to_patientForm, args);
        });
        view.findViewById(R.id.empty_add_patient_button).setOnClickListener(v -> {
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
        patientRepository.getAllPatients(preferencesManager.getLoggedInUserId(), new PatientRepository.Callback<List<Patient>>() {
            @Override
            public void onResult(List<Patient> result) {
                allPatients = result;
                sortPatients();
                filterPatients(searchEditText.getText().toString());
            }
        });
    }
    private void showSortDialog() {
        String[] options = {
                getString(R.string.sort_newest_first),
                getString(R.string.sort_oldest_first),
                getString(R.string.sort_name_az),
                getString(R.string.sort_name_za)
        };
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.sort_by)
                .setSingleChoiceItems(options, currentSortMode, (dialog, which) -> {
                    currentSortMode = which;
                    preferencesManager.setPatientSortMode(currentSortMode);
                    sortPatients();
                    filterPatients(searchEditText.getText().toString());
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void sortPatients() {
        switch (currentSortMode) {
            case SORT_NEWEST:
                allPatients.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
            case SORT_OLDEST:
                allPatients.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                break;
            case SORT_NAME_AZ:
                allPatients.sort((a, b) -> compareNames(a.getFullName(), b.getFullName()));
                break;
            case SORT_NAME_ZA:
                allPatients.sort((a, b) -> compareNames(b.getFullName(), a.getFullName()));
                break;
        }
    }
    private int compareNames(String n1, String n2) {
        if (n1 == null && n2 == null) return 0;
        if (n1 == null) return -1;
        if (n2 == null) return 1;
        return n1.compareToIgnoreCase(n2);
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
                if ((p.getFullName() != null && p.getFullName().toLowerCase().contains(query)) ||
                    (p.getPhone() != null && p.getPhone().toLowerCase().contains(query)) ||
                    (p.getDiagnosis() != null && p.getDiagnosis().toLowerCase().contains(query)) ||
                    (p.getAddress() != null && p.getAddress().toLowerCase().contains(query))) {
                    filtered.add(p);
                }
            }
        }
        adapter.setPatients(filtered);
        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
            if (allPatients.isEmpty()) {
                emptyStateText.setText(R.string.no_patients);
            } else {
                emptyStateText.setText(R.string.no_search_results);
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }
    private void onPatientClicked(Patient patient) {
        Bundle args = new Bundle();
        args.putInt("patientId", (int) patient.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_patientList_to_patientDetail, args);
    }
    private void onPatientDeleteClicked(Patient patient, int position) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_patient_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    long ownerId = preferencesManager.getLoggedInUserId();
                    appointmentRepository.getAppointmentsByPatientId(patient.getId(), ownerId,
                            new AppointmentRepository.Callback<List<Appointment>>() {
                        @Override
                        public void onResult(List<Appointment> appointmentsToRestore) {
                            patientRepository.delete(patient, new PatientRepository.Callback<Void>() {
                                @Override
                                public void onResult(Void result) {
                                    loadPatients();
                                    Snackbar.make(rootView, R.string.deleted, Snackbar.LENGTH_LONG)
                                            .setAction(R.string.undo, v ->
                                                    restorePatient(patient, appointmentsToRestore))
                                            .show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void restorePatient(Patient patient, List<Appointment> appointments) {
        long ownerId = preferencesManager.getLoggedInUserId();
        patient.setOwnerId(ownerId);
        patientRepository.insert(patient, new PatientRepository.Callback<Long>() {
            @Override
            public void onResult(Long result) {
                if (appointments == null || appointments.isEmpty()) {
                    loadPatients();
                    return;
                }
                final int[] remaining = {appointments.size()};
                for (Appointment appointment : appointments) {
                    appointment.setOwnerId(ownerId);
                    appointmentRepository.insert(appointment, new AppointmentRepository.Callback<Long>() {
                        @Override
                        public void onResult(Long r) {
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                loadPatients();
                            }
                        }
                    });
                }
            }
        });
    }
}

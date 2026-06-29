package com.medcare.app.ui.appointments;
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
import com.medcare.app.adapter.AppointmentAdapter;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.utils.PreferencesManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class AppointmentListFragment extends Fragment {
    private static final int SORT_NEWEST = 0;
    private static final int SORT_OLDEST = 1;
    private static final int SORT_NAME_AZ = 2;
    private static final int SORT_NAME_ZA = 3;
    private static final int SORT_DATE_ASC = 4;
    private static final int SORT_DATE_DESC = 5;
    private static final int SORT_TIME_ASC = 6;
    private static final int SORT_TIME_DESC = 7;
    private AppointmentRepository appointmentRepository;
    private PatientRepository patientRepository;
    private AppointmentAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private EditText searchEditText;
    private List<Appointment> allAppointments = new ArrayList<>();
    private Map<Long, String> patientNames = new HashMap<>();
    private int currentSortMode = SORT_NEWEST;
    private PreferencesManager preferencesManager;
    private View rootView;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointment_list, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        appointmentRepository = new AppointmentRepository(requireContext());
        patientRepository = new PatientRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());
        currentSortMode = preferencesManager.getAppointmentSortMode(SORT_NEWEST);
        recyclerView = view.findViewById(R.id.appointment_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        searchEditText = view.findViewById(R.id.search_edit_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppointmentAdapter(this::onAppointmentClicked, this::onAppointmentDeleteClicked);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    adapter.closeRevealed();
                }
            }
        });
        loadAppointments();
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAppointments(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        view.findViewById(R.id.sort_button).setOnClickListener(v -> showSortDialog());
        view.findViewById(R.id.add_appointment_button).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("appointmentId", -1);
            Navigation.findNavController(view).navigate(
                    R.id.action_appointmentList_to_appointmentForm, args);
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        loadAppointments();
    }
    private void loadAppointments() {
        allAppointments = appointmentRepository.getAllAppointments();
        patientNames.clear();
        for (Appointment appointment : allAppointments) {
            if (!patientNames.containsKey(appointment.getPatientId())) {
                Patient patient = patientRepository.getPatientById(appointment.getPatientId());
                patientNames.put(appointment.getPatientId(),
                        patient != null ? patient.getFullName() : "Unknown");
            }
        }
        sortAppointments();
        filterAppointments(searchEditText.getText().toString());
    }
    private void showSortDialog() {
        String[] options = {
                getString(R.string.sort_newest_first),
                getString(R.string.sort_oldest_first),
                getString(R.string.sort_name_az),
                getString(R.string.sort_name_za),
                getString(R.string.sort_date_asc),
                getString(R.string.sort_date_desc),
                getString(R.string.sort_time_asc),
                getString(R.string.sort_time_desc)
        };
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.sort_by)
                .setSingleChoiceItems(options, currentSortMode, (dialog, which) -> {
                    currentSortMode = which;
                    preferencesManager.setAppointmentSortMode(currentSortMode);
                    sortAppointments();
                    filterAppointments(searchEditText.getText().toString());
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private long getDateSortKey(String date) {
        if (date == null) return 0;
        String[] parts = date.split("/");
        if (parts.length != 3) return 0;
        try {
            return Long.parseLong(parts[2]) * 10000 + Long.parseLong(parts[1]) * 100 + Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private void sortAppointments() {
        switch (currentSortMode) {
            case SORT_NEWEST:
                allAppointments.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
            case SORT_OLDEST:
                allAppointments.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                break;
            case SORT_NAME_AZ:
                allAppointments.sort((a, b) -> {
                    String na = patientNames.get(a.getPatientId());
                    String nb = patientNames.get(b.getPatientId());
                    return na.compareToIgnoreCase(nb);
                });
                break;
            case SORT_NAME_ZA:
                allAppointments.sort((a, b) -> {
                    String na = patientNames.get(a.getPatientId());
                    String nb = patientNames.get(b.getPatientId());
                    return nb.compareToIgnoreCase(na);
                });
                break;
            case SORT_DATE_ASC:
                allAppointments.sort((a, b) -> Long.compare(getDateSortKey(a.getDate()), getDateSortKey(b.getDate())));
                break;
            case SORT_DATE_DESC:
                allAppointments.sort((a, b) -> Long.compare(getDateSortKey(b.getDate()), getDateSortKey(a.getDate())));
                break;
            case SORT_TIME_ASC:
                allAppointments.sort((a, b) -> a.getTime().compareTo(b.getTime()));
                break;
            case SORT_TIME_DESC:
                allAppointments.sort((a, b) -> b.getTime().compareTo(a.getTime()));
                break;
        }
    }
    private void filterAppointments(String query) {
        if (query == null) query = "";
        query = query.trim().toLowerCase();
        List<Appointment> filtered;
        if (query.isEmpty()) {
            filtered = allAppointments;
        } else {
            filtered = new ArrayList<>();
            for (Appointment a : allAppointments) {
                String patientName = patientNames.get(a.getPatientId());
                if ((a.getName() != null && a.getName().toLowerCase().contains(query)) ||
                    (patientName != null && patientName.toLowerCase().contains(query)) ||
                    (a.getDate() != null && a.getDate().toLowerCase().contains(query)) ||
                    (a.getTime() != null && a.getTime().toLowerCase().contains(query)) ||
                    (a.getNotes() != null && a.getNotes().toLowerCase().contains(query))) {
                    filtered.add(a);
                }
            }
        }
        adapter.setAppointments(filtered, patientNames);
        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
            if (allAppointments.isEmpty()) {
                emptyStateText.setText(R.string.no_appointments);
            } else {
                emptyStateText.setText(R.string.no_search_results);
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }
    private void onAppointmentClicked(Appointment appointment) {
        Bundle args = new Bundle();
        args.putInt("appointmentId", (int) appointment.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_appointmentList_to_appointmentDetail, args);
    }
    private void onAppointmentDeleteClicked(Appointment appointment, int position) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_appointment_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    appointmentRepository.delete(appointment);
                    loadAppointments();
                    Snackbar.make(rootView, R.string.deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo, v -> {
                                appointmentRepository.insert(appointment);
                                loadAppointments();
                            })
                            .show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}

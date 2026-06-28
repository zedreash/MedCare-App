package com.medcare.app.ui.appointments;

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
import com.medcare.app.adapter.AppointmentAdapter;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentListFragment extends Fragment {

    private AppointmentRepository appointmentRepository;
    private PatientRepository patientRepository;
    private AppointmentAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyStateText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appointmentRepository = new AppointmentRepository(requireContext());
        patientRepository = new PatientRepository(requireContext());

        recyclerView = view.findViewById(R.id.appointment_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new AppointmentAdapter(this::onAppointmentClicked);
        recyclerView.setAdapter(adapter);

        loadAppointments();

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
        List<Appointment> appointments = appointmentRepository.getAllAppointments();

        Map<Long, String> patientNames = new HashMap<>();
        for (Appointment appointment : appointments) {
            if (!patientNames.containsKey(appointment.getPatientId())) {
                Patient patient = patientRepository.getPatientById(appointment.getPatientId());
                patientNames.put(appointment.getPatientId(),
                        patient != null ? patient.getFullName() : "Unknown");
            }
        }

        adapter.setAppointments(appointments, patientNames);

        if (appointments.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void onAppointmentClicked(Appointment appointment) {
        Bundle args = new Bundle();
        args.putInt("appointmentId", (int) appointment.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_appointmentList_to_appointmentForm, args);
    }
}

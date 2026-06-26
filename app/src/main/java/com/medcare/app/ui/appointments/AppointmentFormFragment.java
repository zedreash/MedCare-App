package com.medcare.app.ui.appointments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.medcare.app.R;
import com.medcare.app.data.repository.AppointmentRepository;

public class AppointmentFormFragment extends Fragment {

    private AppointmentRepository appointmentRepository;
    private long appointmentId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            appointmentId = getArguments().getInt("appointmentId", -1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointment_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appointmentRepository = new AppointmentRepository(requireContext());

        if (appointmentId != -1) {
            loadAppointment();
        }

        view.findViewById(R.id.save_button).setOnClickListener(v -> onSaveClicked());
    }

    private void loadAppointment() {
    }

    private void onSaveClicked() {
    }
}

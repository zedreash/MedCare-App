package com.medcare.app.ui.appointments;

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
import com.medcare.app.adapter.AppointmentAdapter;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.repository.AppointmentRepository;

import java.util.List;

public class AppointmentListFragment extends Fragment {

    private AppointmentRepository appointmentRepository;
    private AppointmentAdapter adapter;
    private List<Appointment> appointmentList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appointmentRepository = new AppointmentRepository(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.appointment_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new AppointmentAdapter(this::onAppointmentClicked);
        recyclerView.setAdapter(adapter);

        loadAppointments();

        view.findViewById(R.id.add_appointment_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_appointmentList_to_appointmentForm));
    }

    private void loadAppointments() {
    }

    private void onAppointmentClicked(Appointment appointment) {
    }
}

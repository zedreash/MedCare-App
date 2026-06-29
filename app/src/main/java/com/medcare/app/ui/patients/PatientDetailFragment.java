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
import com.medcare.app.adapter.PatientAppointmentAdapter;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import java.util.List;
public class PatientDetailFragment extends Fragment {
    private static final String ARG_PATIENT_ID = "patientId";
    private TextView nameText;
    private TextView phoneText;
    private TextView diagnosisText;
    private TextView addressText;
    private TextView notesText;
    private RecyclerView appointmentsRecycler;
    private PatientRepository patientRepository;
    private AppointmentRepository appointmentRepository;
    private PatientAppointmentAdapter adapter;
    private long patientId;
    private Patient patient;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientId = getArguments().getInt(ARG_PATIENT_ID, -1);
        }
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_detail, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        patientRepository = new PatientRepository(requireContext());
        appointmentRepository = new AppointmentRepository(requireContext());
        nameText = view.findViewById(R.id.patient_name_text);
        phoneText = view.findViewById(R.id.patient_phone_text);
        diagnosisText = view.findViewById(R.id.patient_diagnosis_text);
        addressText = view.findViewById(R.id.patient_address_text);
        notesText = view.findViewById(R.id.patient_notes_text);
        appointmentsRecycler = view.findViewById(R.id.appointments_recycler_view);
        appointmentsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        view.findViewById(R.id.back_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.edit_button).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("patientId", (int) patientId);
            Navigation.findNavController(view)
                    .navigate(R.id.action_patientDetail_to_patientForm, args);
        });
        loadPatient();
    }
    private void loadPatient() {
        if (patientId == -1) return;
        patient = patientRepository.getPatientById(patientId);
        if (patient == null) {
            Navigation.findNavController(requireView()).navigateUp();
            return;
        }
        nameText.setText(patient.getFullName());
        phoneText.setText(patient.getPhone());
        diagnosisText.setText(patient.getDiagnosis());
        String address = patient.getAddress();
        addressText.setText(address != null && !address.isEmpty() ? address : null);
        String notes = patient.getNotes();
        notesText.setText(notes != null && !notes.isEmpty() ? notes : null);
        List<Appointment> appointments = appointmentRepository.getAppointmentsByPatientId(patientId);
        adapter = new PatientAppointmentAdapter(appointment -> {
            Bundle args = new Bundle();
            args.putInt("appointmentId", (int) appointment.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_patientDetail_to_appointmentDetail, args);
        });
        appointmentsRecycler.setAdapter(adapter);
        adapter.setAppointments(appointments);
    }
}

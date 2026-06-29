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
import com.medcare.app.R;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.utils.PreferencesManager;
public class AppointmentDetailFragment extends Fragment {
    private static final String ARG_APPOINTMENT_ID = "appointmentId";
    private TextView nameText;
    private TextView patientText;
    private TextView dateText;
    private TextView timeText;
    private TextView durationText;
    private TextView notesText;
    private AppointmentRepository appointmentRepository;
    private PatientRepository patientRepository;
    private PreferencesManager preferencesManager;
    private long appointmentId;
    private Appointment appointment;
    private Patient patient;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            appointmentId = getArguments().getInt(ARG_APPOINTMENT_ID, -1);
        }
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointment_detail, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appointmentRepository = new AppointmentRepository(requireContext());
        patientRepository = new PatientRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());
        nameText = view.findViewById(R.id.appointment_name_text);
        patientText = view.findViewById(R.id.appointment_patient_text);
        dateText = view.findViewById(R.id.appointment_date_text);
        timeText = view.findViewById(R.id.appointment_time_text);
        durationText = view.findViewById(R.id.appointment_duration_text);
        notesText = view.findViewById(R.id.appointment_notes_text);
        view.findViewById(R.id.back_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.edit_button).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("appointmentId", (int) appointmentId);
            Navigation.findNavController(view)
                    .navigate(R.id.action_appointmentDetail_to_appointmentForm, args);
        });
        loadAppointment();
    }
    private void loadAppointment() {
        if (appointmentId == -1) return;
        appointment = appointmentRepository.getAppointmentById(appointmentId, preferencesManager.getLoggedInUserId());
        if (appointment == null) {
            Navigation.findNavController(requireView()).navigateUp();
            return;
        }
        patient = patientRepository.getPatientById(appointment.getPatientId(), preferencesManager.getLoggedInUserId());
        nameText.setText(appointment.getName());
        String patientName = patient != null ? patient.getFullName() : "Unknown";
        patientText.setText(patientName);
        patientText.setOnClickListener(v -> {
            if (patient != null) {
                Bundle args = new Bundle();
                args.putInt("patientId", (int) patient.getId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_appointmentDetail_to_patientDetail, args);
            }
        });
        dateText.setText(appointment.getDate());
        timeText.setText(appointment.getTime());
        durationText.setText(appointment.getDuration() + " min");
        String notes = appointment.getNotes();
        notesText.setText(notes != null && !notes.isEmpty() ? notes : null);
    }
}

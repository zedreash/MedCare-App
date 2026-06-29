package com.medcare.app.ui.appointments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import com.medcare.app.R;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.utils.DateUtils;
import com.medcare.app.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AppointmentFormFragment extends Fragment {

    private AppointmentRepository appointmentRepository;
    private PatientRepository patientRepository;
    private long appointmentId = -1;
    private Appointment currentAppointment;
    private long selectedPatientId = -1;
    private String selectedPatientName;

    private TextInputLayout nameLayout;
    private TextInputLayout patientLayout;
    private TextInputLayout dateLayout;
    private TextInputLayout timeLayout;
    private TextInputLayout notesLayout;
    private TextInputLayout durationLayout;

    private EditText nameInput;
    private EditText patientInput;
    private EditText dateInput;
    private EditText timeInput;
    private EditText durationInput;
    private EditText notesInput;

    private TextView formTitle;
    private View deleteButton;
    private View rootView;

    private void initDurationField() {
        PreferencesManager prefs = new PreferencesManager(requireContext());
        int defaultDuration = prefs.getDefaultAppointmentDuration();
        durationInput.setText(String.valueOf(defaultDuration));
    }

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

        rootView = view;
        appointmentRepository = new AppointmentRepository(requireContext());
        patientRepository = new PatientRepository(requireContext());

        initViews(view);
        initDurationField();
        setupPickers();
        setupErrorClearListeners();

        if (appointmentId != -1) {
            formTitle.setText(R.string.edit_appointment);
            deleteButton.setVisibility(View.VISIBLE);
            loadAppointment();
        }

        view.findViewById(R.id.back_button).setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.save_button).setOnClickListener(v -> onSaveClicked());
        deleteButton.setOnClickListener(v -> onDeleteClicked());
    }

    private void initViews(View view) {
        formTitle = view.findViewById(R.id.form_title);
        deleteButton = view.findViewById(R.id.delete_button);

        nameLayout = view.findViewById(R.id.name_layout);
        patientLayout = view.findViewById(R.id.patient_layout);
        dateLayout = view.findViewById(R.id.date_layout);
        timeLayout = view.findViewById(R.id.time_layout);
        notesLayout = view.findViewById(R.id.notes_layout);
        durationLayout = view.findViewById(R.id.duration_layout);

        nameInput = view.findViewById(R.id.name_input);
        patientInput = view.findViewById(R.id.patient_input);
        dateInput = view.findViewById(R.id.date_input);
        timeInput = view.findViewById(R.id.time_input);
        durationInput = view.findViewById(R.id.duration_input);
        notesInput = view.findViewById(R.id.notes_input);
    }

    private void setupPickers() {
        patientInput.setOnClickListener(v -> showPatientPicker());
        patientInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showPatientPicker();
        });
        patientLayout.setEndIconOnClickListener(v -> showPatientPicker());

        dateInput.setOnClickListener(v -> showDatePicker());
        dateInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker();
        });

        timeInput.setOnClickListener(v -> showTimePicker());
        timeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showTimePicker();
        });
    }

    private void setupErrorClearListeners() {
        patientInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) patientLayout.setError(null); });
        dateInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) dateLayout.setError(null); });
        timeInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) timeLayout.setError(null); });
    }

    private void showPatientPicker() {
        List<Patient> patients = patientRepository.getAllPatients();
        if (patients.isEmpty()) {
            Snackbar.make(rootView, R.string.no_patients, Snackbar.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_patient_search, null);

        EditText searchInput = dialogView.findViewById(R.id.search_input);
        ListView listView = dialogView.findViewById(R.id.patient_list);

        PatientSearchAdapter adapter = new PatientSearchAdapter(requireContext(), patients);
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_patient)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .show();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Patient patient = adapter.getItem(position);
            selectedPatientId = patient.getId();
            selectedPatientName = patient.getFullName();
            patientInput.setText(selectedPatientName);
            patientLayout.setError(null);
            dialog.dismiss();
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private static class PatientSearchAdapter extends ArrayAdapter<Patient> {

        private List<Patient> originalList;
        private List<Patient> filteredList;
        private final Object lock = new Object();

        PatientSearchAdapter(Context context, List<Patient> patients) {
            super(context, 0, patients);
            this.originalList = new ArrayList<>(patients);
            this.filteredList = new ArrayList<>(patients);
        }

        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Override
        public Patient getItem(int position) {
            return filteredList.get(position);
        }

        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_patient_search, parent, false);
            }
            Patient patient = getItem(position);
            TextView nameText = convertView.findViewById(R.id.patient_name);
            TextView infoText = convertView.findViewById(R.id.patient_info);
            nameText.setText(patient.getFullName());
            String info = patient.getPhone();
            if (patient.getAddress() != null && !patient.getAddress().isEmpty()) {
                info += " | " + patient.getAddress();
            }
            if (patient.getDiagnosis() != null && !patient.getDiagnosis().isEmpty()) {
                info += " | " + getContext().getString(R.string.patient_diagnosis) + ": " + patient.getDiagnosis();
            }
            infoText.setText(info);
            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint == null || constraint.length() == 0) {
                        synchronized (lock) {
                            results.values = new ArrayList<>(originalList);
                            results.count = originalList.size();
                        }
                    } else {
                        String query = constraint.toString().toLowerCase();
                        List<Patient> filtered = new ArrayList<>();
                    for (Patient p : originalList) {
                        if (p.getFullName().toLowerCase().contains(query)
                                || p.getPhone().toLowerCase().contains(query)
                                || (p.getDiagnosis() != null && p.getDiagnosis().toLowerCase().contains(query))
                                || (p.getAddress() != null && p.getAddress().toLowerCase().contains(query))) {
                                filtered.add(p);
                            }
                        }
                        results.values = filtered;
                        results.count = filtered.size();
                    }
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredList = (List<Patient>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }

    private void showDatePicker() {
        Locale locale = resolveAppLocale();
        if (locale != null) {
            Locale.setDefault(locale);
        }

        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    String formatted = String.format("%02d/%02d/%04d", day, month + 1, year);
                    dateInput.setText(formatted);
                    dateLayout.setError(null);
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePicker.show();
    }

    private Locale resolveAppLocale() {
        PreferencesManager prefs = new PreferencesManager(requireContext());
        String lang = prefs.getLanguage();
        if ("system".equals(lang)) {
            String sysLang = Locale.getDefault().getLanguage();
            if (!sysLang.equals("en") && !sysLang.equals("ar")
                    && !sysLang.equals("iw") && !sysLang.equals("he")) {
                return new Locale("iw", "IL");
            }
            return null;
        }
        if ("he".equals(lang)) {
            return new Locale("iw", "IL");
        }
        return new Locale(lang);
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                (view, hour, minute) -> {
                    String formatted = String.format("%02d:%02d", hour, minute);
                    timeInput.setText(formatted);
                    timeLayout.setError(null);
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        timePicker.show();
    }

    private void loadAppointment() {
        currentAppointment = appointmentRepository.getAppointmentById(appointmentId);
        if (currentAppointment == null) {
            Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
            Navigation.findNavController(rootView).navigateUp();
            return;
        }

        Patient patient = patientRepository.getPatientById(currentAppointment.getPatientId());
        if (patient != null) {
            selectedPatientId = patient.getId();
            selectedPatientName = patient.getFullName();
            patientInput.setText(selectedPatientName);
        }

        nameInput.setText(currentAppointment.getName());
        dateInput.setText(currentAppointment.getDate());
        timeInput.setText(currentAppointment.getTime());
        durationInput.setText(String.valueOf(currentAppointment.getDuration()));
        notesInput.setText(currentAppointment.getNotes());
    }

    private void onSaveClicked() {
        if (!validateInputs()) {
            return;
        }

        String nameValue = nameInput.getText().toString().trim();
        String date = dateInput.getText().toString().trim();
        String time = timeInput.getText().toString().trim();
        int duration = Integer.parseInt(durationInput.getText().toString().trim());
        String notes = notesInput.getText().toString().trim();

        if (appointmentId == -1) {
            Appointment appointment = new Appointment(selectedPatientId, nameValue, date, time, duration, notes,
                    DateUtils.getCurrentTimestamp());
            appointmentRepository.insert(appointment);
            Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
        } else {
            currentAppointment.setPatientId(selectedPatientId);
            currentAppointment.setName(nameValue);
            currentAppointment.setDate(date);
            currentAppointment.setTime(time);
            currentAppointment.setDuration(duration);
            currentAppointment.setNotes(notes);
            appointmentRepository.update(currentAppointment);
            Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
        }

        Navigation.findNavController(rootView).navigateUp();
    }

    private void onDeleteClicked() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    appointmentRepository.delete(currentAppointment);
                    Snackbar.make(rootView, R.string.success_deleted, Snackbar.LENGTH_SHORT).show();
                    Navigation.findNavController(rootView).navigateUp();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean validateInputs() {
        boolean valid = true;

        if (TextUtils.isEmpty(nameInput.getText())) {
            nameLayout.setError(getString(R.string.field_required));
            valid = false;
        } else {
            nameLayout.setError(null);
        }

        if (selectedPatientId == -1) {
            patientLayout.setError(getString(R.string.field_required));
            valid = false;
        } else {
            patientLayout.setError(null);
        }

        if (TextUtils.isEmpty(dateInput.getText())) {
            dateLayout.setError(getString(R.string.field_required));
            valid = false;
        } else {
            dateLayout.setError(null);
        }

        if (TextUtils.isEmpty(timeInput.getText())) {
            timeLayout.setError(getString(R.string.field_required));
            valid = false;
        } else {
            timeLayout.setError(null);
        }

        if (TextUtils.isEmpty(durationInput.getText())) {
            durationLayout.setError(getString(R.string.field_required));
            valid = false;
        } else {
            try {
                int d = Integer.parseInt(durationInput.getText().toString().trim());
                if (d <= 0) {
                    durationLayout.setError(getString(R.string.field_required));
                    valid = false;
                } else {
                    durationLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                durationLayout.setError(getString(R.string.field_required));
                valid = false;
            }
        }

        return valid;
    }
}

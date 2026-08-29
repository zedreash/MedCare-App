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
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.medcare.app.R;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.utils.DateUtils;
import com.medcare.app.utils.FieldHint;
import com.medcare.app.utils.PreferencesManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
public class AppointmentFormFragment extends Fragment {
    private AppointmentRepository appointmentRepository;
    private PatientRepository patientRepository;
    private PreferencesManager preferencesManager;
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
    private ChipGroup repeatOptions;
    private View repeatCustomLayout;
    private EditText repeatCustomInput;
    private ChipGroup repeatCustomUnit;
    private View repeatCountLayout;
    private TextView repeatCountValue;
    private com.google.android.material.slider.Slider repeatCountSlider;
    private String repeatType;
    private int repeatCount = 1;
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
        preferencesManager = new PreferencesManager(requireContext());
        initViews(view);
        initDurationField();
        setupPickers();
        setupErrorClearListeners();
        if (appointmentId != -1) {
            formTitle.setText(R.string.edit_appointment);
            deleteButton.setVisibility(View.VISIBLE);
            loadAppointment();
        }
        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            hideKeyboard();
            Navigation.findNavController(view).navigateUp();
        });
        view.findViewById(R.id.save_button).setOnClickListener(v -> onSaveClicked());
        deleteButton.setOnClickListener(v -> onDeleteClicked());

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        hideKeyboard();
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                });
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
        repeatOptions = view.findViewById(R.id.repeat_options);
        repeatCustomLayout = view.findViewById(R.id.repeat_custom_layout);
        repeatCustomInput = view.findViewById(R.id.repeat_custom_input);
        repeatCustomUnit = view.findViewById(R.id.repeat_custom_unit);
        repeatCountLayout = view.findViewById(R.id.repeat_count_layout);
        repeatCountValue = view.findViewById(R.id.repeat_count_value);
        repeatCountSlider = view.findViewById(R.id.repeat_count_slider);
        FieldHint.required(nameLayout, R.string.appointment_name);
        FieldHint.required(patientLayout, R.string.select_patient);
        FieldHint.required(dateLayout, R.string.appointment_date);
        FieldHint.required(timeLayout, R.string.appointment_time);
        FieldHint.required(durationLayout, R.string.appointment_duration);
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
        repeatOptions.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.repeat_custom_chip) {
                repeatType = buildCustomRule();
                repeatCustomLayout.setVisibility(View.VISIBLE);
            } else {
                repeatType = chipToRepeat(id);
                repeatCustomLayout.setVisibility(View.GONE);
            }
            if (repeatType != null && repeatCount <= 1) {
                repeatCount = 2;
            }
            updateRepeatRows();
        });
        repeatCustomInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                repeatType = buildCustomRule();
            }
        });
        repeatCustomUnit.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds != null && !checkedIds.isEmpty()) {
                repeatType = buildCustomRule();
            }
        });
        repeatCountSlider.addOnChangeListener((slider, value, fromUser) -> {
            repeatCount = (int) value;
            repeatCountValue.setText(String.valueOf(repeatCount));
        });
    }
    private void setupErrorClearListeners() {
        patientInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) patientLayout.setError(null); });
        dateInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) dateLayout.setError(null); });
        timeInput.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) timeLayout.setError(null); });
    }
    private void showPatientPicker() {
        patientRepository.getAllPatients(preferencesManager.getLoggedInUserId(), new PatientRepository.Callback<List<Patient>>() {
            @Override
            public void onResult(List<Patient> patients) {
                if (!isAdded()) return;
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
                        if ((p.getFullName() != null && p.getFullName().toLowerCase().contains(query))
                                || (p.getPhone() != null && p.getPhone().toLowerCase().contains(query))
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
        Calendar maxCalendar = Calendar.getInstance();
        maxCalendar.add(Calendar.YEAR, 1);
        datePicker.getDatePicker().setMaxDate(maxCalendar.getTimeInMillis());
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
        appointmentRepository.getAppointmentById(appointmentId, preferencesManager.getLoggedInUserId(), new AppointmentRepository.Callback<Appointment>() {
            @Override
            public void onResult(Appointment result) {
                if (!isAdded()) return;
                currentAppointment = result;
                if (currentAppointment == null) {
                    Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
                    Navigation.findNavController(rootView).navigateUp();
                    return;
                }
                patientRepository.getPatientById(currentAppointment.getPatientId(), preferencesManager.getLoggedInUserId(), new PatientRepository.Callback<Patient>() {
                    @Override
                    public void onResult(Patient patient) {
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
                        String rule = currentAppointment.getRecurrenceRule();
                        if (rule != null) {
                            repeatType = rule;
                            repeatCount = 2;
                        }
                        syncRepeatUiFromType();
                    }
                });
            }
        });
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
        boolean editingWithSameDateTime = appointmentId != -1 && currentAppointment != null
                && date.equals(currentAppointment.getDate())
                && time.equals(currentAppointment.getTime());
        if (!editingWithSameDateTime && isDateTimeInPast(date, time)) {
            timeLayout.setError(getString(R.string.appointment_past_error));
            Snackbar.make(rootView, R.string.appointment_past_error, Snackbar.LENGTH_LONG).show();
            return;
        }
        long ownerId = preferencesManager.getLoggedInUserId();
        appointmentRepository.getAppointmentsByDate(date, ownerId, new AppointmentRepository.Callback<List<Appointment>>() {
            @Override
            public void onResult(List<Appointment> existing) {
                if (!isAdded()) return;
                if (hasTimeConflict(existing, time, duration)) {
                    timeLayout.setError(getString(R.string.time_conflict));
                    Snackbar.make(rootView, R.string.time_conflict, Snackbar.LENGTH_LONG).show();
                    return;
                }
                int overflow = overflowMinutes(time, duration);
                if (overflow > 0) {
                    String nextDate = addDays(date, 1);
                    appointmentRepository.getAppointmentsByDate(nextDate, ownerId, new AppointmentRepository.Callback<List<Appointment>>() {
                        @Override
                        public void onResult(List<Appointment> nextExisting) {
                            if (!isAdded()) return;
                            if (hasOverflowConflict(nextExisting, overflow)) {
                                timeLayout.setError(getString(R.string.time_conflict));
                                Snackbar.make(rootView, R.string.time_conflict, Snackbar.LENGTH_LONG).show();
                                return;
                            }
                            saveAppointment(nameValue, date, time, duration, notes, repeatType, repeatCount);
                        }
                    });
                } else {
                    saveAppointment(nameValue, date, time, duration, notes, repeatType, repeatCount);
                }
            }
        });
    }

    private int overflowMinutes(String time, int duration) {
        long start = parseTimeToMinutes(time);
        if (start < 0) return 0;
        long end = start + duration;
        return end > 1440 ? (int) (end - 1440) : 0;
    }

    private boolean hasOverflowConflict(List<Appointment> nextDay, int overflowMinutes) {
        for (Appointment a : nextDay) {
            long start = parseTimeToMinutes(a.getTime());
            if (start < 0) continue;
            long end = start + a.getDuration();
            if (start < overflowMinutes && 0 < end) return true;
        }
        return false;
    }

    private String addDays(String date, int days) {
        try {
            String[] p = date.split("/");
            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(p[2]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[0]));
            c.add(Calendar.DAY_OF_MONTH, days);
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.getTime());
        } catch (Exception e) {
            return date;
        }
    }
    private void saveAppointment(String nameValue, String date, String time, int duration, String notes,
                                 String repeatType, int occurrences) {
        long ownerId = preferencesManager.getLoggedInUserId();
        if (appointmentId == -1) {
            boolean repeating = repeatType != null && occurrences > 1;
            Long groupId = repeating ? System.currentTimeMillis() : null;
            final int[] inserted = {0};
            final int total = repeating ? occurrences : 1;
            Calendar base = parseDateOnly(date);
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            for (int i = 0; i < total; i++) {
                Calendar c = (Calendar) base.clone();
                if (i > 0) {
                    applyRepeatOffset(c, repeatType, i);
                }
                String d = fmt.format(c.getTime());
                Appointment appointment = new Appointment(
                        selectedPatientId, nameValue, d, time, duration, notes,
                        DateUtils.getCurrentTimestamp());
                appointment.setOwnerId(ownerId);
                if (repeating) {
                    appointment.setRecurrenceGroupId(groupId);
                    appointment.setRecurrenceRule(repeatType);
                }
                appointmentRepository.insert(appointment, new AppointmentRepository.Callback<Long>() {
                    @Override
                    public void onResult(Long result) {
                        appointment.setId(result);
                        scheduleFollowUp(appointment);
                        inserted[0]++;
                        if (inserted[0] == total) {
                            if (isAdded()) {
                                Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
                                Navigation.findNavController(rootView).navigateUp();
                            }
                        }
                    }
                });
            }
        } else {
            currentAppointment.setPatientId(selectedPatientId);
            currentAppointment.setName(nameValue);
            currentAppointment.setDate(date);
            currentAppointment.setTime(time);
            currentAppointment.setDuration(duration);
            currentAppointment.setNotes(notes);
            appointmentRepository.update(currentAppointment, new AppointmentRepository.Callback<Void>() {
                @Override
                public void onResult(Void result) {
                    scheduleFollowUp(currentAppointment);
                    if (isAdded()) {
                        Snackbar.make(rootView, R.string.success_saved, Snackbar.LENGTH_SHORT).show();
                        Navigation.findNavController(rootView).navigateUp();
                    }
                }
            });
        }
    }

    private void scheduleFollowUp(com.medcare.app.data.entity.Appointment a) {
        try {
            long end = AppointmentRepository.toEpochMillis(a.getDate(), a.getTime());
            if (end <= 0) return;
            int dur = a.getDuration() > 0 ? a.getDuration() : 0;
            com.medcare.app.notifications.FollowUpScheduler.scheduleAtEnd(
                    requireContext(), a.getId(), end + dur * 60000L);
            com.medcare.app.background.BackgroundScheduler.rescheduleAll(requireContext());
        } catch (Exception ignored) {}
    }
    private Calendar parseDateOnly(String date) {
        try {
            String[] p = date.split("/");
            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(p[2]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[0]));
            return c;
        } catch (Exception e) {
            return Calendar.getInstance();
        }
    }
    private String chipToRepeat(int chipId) {
        if (chipId == R.id.repeat_daily_chip) return "daily";
        if (chipId == R.id.repeat_weekly_chip) return "weekly";
        if (chipId == R.id.repeat_monthly_chip) return "monthly";
        if (chipId == R.id.repeat_quarterly_chip) return "quarterly";
        if (chipId == R.id.repeat_yearly_chip) return "yearly";
        if (chipId == R.id.repeat_custom_chip) return buildCustomRule();
        return null;
    }

    private int repeatToChipRes(String rule) {
        if (rule == null) return R.id.repeat_none_chip;
        if ("daily".equals(rule)) return R.id.repeat_daily_chip;
        if ("weekly".equals(rule)) return R.id.repeat_weekly_chip;
        if ("monthly".equals(rule)) return R.id.repeat_monthly_chip;
        if ("quarterly".equals(rule)) return R.id.repeat_quarterly_chip;
        if ("yearly".equals(rule)) return R.id.repeat_yearly_chip;
        return R.id.repeat_custom_chip;
    }

    private void syncRepeatUiFromType() {
        int chipId = repeatToChipRes(repeatType);
        repeatOptions.check(chipId);
        if (chipId == R.id.repeat_custom_chip) {
            prefillCustomRepeat(repeatType);
            repeatCustomLayout.setVisibility(View.VISIBLE);
        } else {
            repeatCustomLayout.setVisibility(View.GONE);
        }
        updateRepeatRows();
    }

    private String buildCustomRule() {
        int n = 1;
        try {
            n = Math.max(1, Integer.parseInt(repeatCustomInput.getText().toString().trim()));
        } catch (NumberFormatException ignored) {}
        if (n > 365) n = 365;
        String unit = "weeks";
        int unitId = repeatCustomUnit.getCheckedChipId();
        if (unitId == R.id.repeat_unit_days) unit = "days";
        else if (unitId == R.id.repeat_unit_months) unit = "months";
        else if (unitId == R.id.repeat_unit_years) unit = "years";
        return "every:" + n + ":" + unit;
    }

    private void prefillCustomRepeat(String rule) {
        int n = 1;
        String unit = "weeks";
        if (rule != null && rule.startsWith("every:")) {
            String[] parts = rule.split(":");
            if (parts.length == 3) {
                try {
                    n = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {}
                unit = parts[2];
            }
        }
        repeatCustomInput.setText(String.valueOf(n));
        int unitRes;
        if ("days".equals(unit)) unitRes = R.id.repeat_unit_days;
        else if ("months".equals(unit)) unitRes = R.id.repeat_unit_months;
        else if ("years".equals(unit)) unitRes = R.id.repeat_unit_years;
        else unitRes = R.id.repeat_unit_weeks;
        repeatCustomUnit.check(unitRes);
    }

    private void updateRepeatRows() {
        boolean repeating = repeatType != null;
        repeatCountLayout.setVisibility(repeating ? View.VISIBLE : View.GONE);
        repeatCountValue.setText(String.valueOf(repeatCount));
        if (repeatCountSlider != null) {
            repeatCountSlider.setValue(repeatCount);
        }
    }

    private void applyRepeatOffset(Calendar c, String rule, int i) {
        if (rule == null || i <= 0) return;
        int step = 1;
        String type = rule;
        if (rule.startsWith("every:")) {
            String[] parts = rule.split(":");
            if (parts.length == 3) {
                try {
                    step = Math.max(1, Integer.parseInt(parts[1]));
                } catch (NumberFormatException e) {
                    step = 1;
                }
                type = parts[2];
            }
        }
        switch (type) {
            case "daily":
            case "days": c.add(Calendar.DAY_OF_MONTH, step * i); break;
            case "weekly":
            case "weeks": c.add(Calendar.DAY_OF_MONTH, step * 7 * i); break;
            case "monthly":
            case "months": c.add(Calendar.MONTH, step * i); break;
            case "quarterly": c.add(Calendar.MONTH, 3 * i); break;
            case "yearly":
            case "years": c.add(Calendar.YEAR, step * i); break;
        }
    }

    private boolean isDateTimeInPast(String date, String time) {
        try {
            String[] dateParts = date.split("/");
            if (dateParts.length != 3) return false;
            String[] timeParts = time.split(":");
            if (timeParts.length != 2) return false;
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(dateParts[2]),
                    Integer.parseInt(dateParts[1]) - 1,
                    Integer.parseInt(dateParts[0]),
                    Integer.parseInt(timeParts[0]),
                    Integer.parseInt(timeParts[1]),
                    0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis() < System.currentTimeMillis();
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private boolean hasTimeConflict(List<Appointment> existing, String time, int duration) {
        long newStart = parseTimeToMinutes(time);
        if (newStart < 0) return false;
        long newEnd = newStart + duration;
        for (Appointment appt : existing) {
            if (appt.getId() == appointmentId) continue;
            long start = parseTimeToMinutes(appt.getTime());
            if (start < 0) continue;
            long end = start + appt.getDuration();
            if (newStart < end && start < newEnd) return true;
        }
        return false;
    }
    private long parseTimeToMinutes(String time) {
        if (time == null || time.isEmpty()) return -1;
        String[] parts = time.split(":");
        if (parts.length != 2) return -1;
        try {
            return Long.parseLong(parts[0]) * 60 + Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    private void onDeleteClicked() {
        if (currentAppointment == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_appointment_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    com.medcare.app.notifications.FollowUpScheduler.cancelAtEnd(
                            requireContext(), currentAppointment.getId());
                    appointmentRepository.delete(currentAppointment, new AppointmentRepository.Callback<Void>() {
                        @Override
                        public void onResult(Void result) {
                            Snackbar.make(rootView, R.string.success_deleted, Snackbar.LENGTH_SHORT).show();
                            Navigation.findNavController(rootView).navigateUp();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void hideKeyboard() {
        View focused = requireActivity().getCurrentFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
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

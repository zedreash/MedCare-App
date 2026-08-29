package com.medcare.app.ui.appointments;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import com.medcare.app.R;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.utils.AppointmentStatus;
import com.medcare.app.utils.PreferencesManager;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class AppointmentDetailFragment extends Fragment {
    private static final String ARG_APPOINTMENT_ID = "appointmentId";
    private TextView nameText;
    private TextView patientText;
    private TextView dateText;
    private TextView timeText;
    private TextView durationText;
    private TextView notesText;
    private TextView statusText;
    private TextView recurrenceText;
    private View deleteSeriesButton;
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
        statusText = view.findViewById(R.id.appointment_status_text);
        recurrenceText = view.findViewById(R.id.appointment_recurrence_text);
        deleteSeriesButton = view.findViewById(R.id.delete_series_button);
        view.findViewById(R.id.status_row).setOnClickListener(v -> showStatusDialog());
        deleteSeriesButton.setOnClickListener(v -> confirmDeleteSeries());
        view.findViewById(R.id.back_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.edit_button).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("appointmentId", (int) appointmentId);
            Navigation.findNavController(view)
                    .navigate(R.id.action_appointmentDetail_to_appointmentForm, args);
        });
        view.findViewById(R.id.reschedule_button).setOnClickListener(v -> showRescheduleDialog());
        view.findViewById(R.id.delete_button).setOnClickListener(v -> confirmDelete());
        loadAppointment();
    }

    private void confirmDelete() {
        if (appointment == null) return;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_appointment_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                        com.medcare.app.notifications.FollowUpScheduler.cancelAtEnd(
                                requireContext(), appointment.getId());
                        appointmentRepository.delete(appointment, new AppointmentRepository.Callback<Void>() {
                            @Override
                            public void onResult(Void result) {
                                com.medcare.app.background.BackgroundScheduler
                                        .rescheduleAll(requireContext());
                                if (isAdded()) {
                                    Snackbar.make(requireView(), R.string.success_deleted, Snackbar.LENGTH_SHORT).show();
                                    Navigation.findNavController(requireView()).navigateUp();
                                }
                            }
                        });
                    })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void loadAppointment() {
        if (appointmentId == -1) return;
        appointmentRepository.getAppointmentById(appointmentId, preferencesManager.getLoggedInUserId(), new AppointmentRepository.Callback<Appointment>() {
            @Override
            public void onResult(Appointment result) {
                if (!isAdded()) return;
                appointment = result;
                if (appointment == null) {
                    Navigation.findNavController(requireView()).navigateUp();
                    return;
                }
                patientRepository.getPatientById(appointment.getPatientId(), preferencesManager.getLoggedInUserId(), new PatientRepository.Callback<Patient>() {
                    @Override
                    public void onResult(Patient result) {
                        patient = result;
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
                        updateStatusDisplay();
                        updateRecurrenceDisplay();
                        String notes = appointment.getNotes();
                        notesText.setText(notes != null && !notes.isEmpty() ? notes : null);
                    }
                });
            }
        });
    }

    private void showStatusDialog() {
        if (appointment == null) return;
        String[] values = AppointmentStatus.values();
        String[] labels = new String[values.length];
        int current = 0;
        for (int i = 0; i < values.length; i++) {
            labels[i] = getString(AppointmentStatus.labelRes(values[i]));
            if (values[i].equals(appointment.getStatus())) current = i;
        }
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_status)
                .setSingleChoiceItems(labels, current, (dialog, which) -> {
                    appointment.setStatus(values[which]);
                    appointmentRepository.update(appointment, new AppointmentRepository.Callback<Void>() {
                        @Override
                        public void onResult(Void result) {
                            updateStatusDisplay();
                        }
                    });
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateStatusDisplay() {
        if (appointment == null || statusText == null) return;
        String status = appointment.getStatus();
        statusText.setText(getString(AppointmentStatus.labelRes(status)));
        int color;
        if (AppointmentStatus.CANCELLED.equals(status)) {
            color = MaterialColors.getColor(requireContext(),
                    com.google.android.material.R.attr.colorError,
                    ContextCompat.getColor(requireContext(), R.color.error));
        } else if (AppointmentStatus.COMPLETED.equals(status)) {
            color = ContextCompat.getColor(requireContext(), R.color.success);
        } else if (AppointmentStatus.NO_SHOW.equals(status)) {
            color = ContextCompat.getColor(requireContext(), R.color.warning);
        } else if (AppointmentStatus.RESCHEDULED.equals(status)) {
            color = MaterialColors.getColor(requireContext(),
                    com.google.android.material.R.attr.colorPrimary,
                    ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            color = MaterialColors.getColor(requireContext(),
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
        statusText.setTextColor(color);
    }

    private void updateRecurrenceDisplay() {
        if (appointment == null) return;
        Long groupId = appointment.getRecurrenceGroupId();
        if (groupId != null) {
            String label = repeatLabel(appointment.getRecurrenceRule());
            recurrenceText.setVisibility(android.view.View.VISIBLE);
            recurrenceText.setText(label + " \u00B7 " + getString(R.string.series));
            deleteSeriesButton.setVisibility(android.view.View.VISIBLE);
        } else {
            recurrenceText.setVisibility(android.view.View.GONE);
            deleteSeriesButton.setVisibility(android.view.View.GONE);
        }
    }

    private String repeatLabel(String rule) {
        if (rule == null) return getString(R.string.repeat_none);
        switch (rule) {
            case "daily": return getString(R.string.repeat_daily);
            case "weekly": return getString(R.string.repeat_weekly);
            case "monthly": return getString(R.string.repeat_monthly);
            case "quarterly": return getString(R.string.repeat_quarterly);
            case "yearly": return getString(R.string.repeat_yearly);
        }
        int n = 1;
        String unit = "weeks";
        if (rule.startsWith("every:")) {
            String[] parts = rule.split(":");
            if (parts.length == 3) {
                try {
                    n = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {}
                unit = parts[2];
            }
        }
        String unitLabel;
        if ("days".equals(unit)) unitLabel = getString(R.string.repeat_unit_days);
        else if ("months".equals(unit)) unitLabel = getString(R.string.repeat_unit_months);
        else if ("years".equals(unit)) unitLabel = getString(R.string.repeat_unit_years);
        else unitLabel = getString(R.string.repeat_unit_weeks);
        return getString(R.string.repeat_every, n, unitLabel);
    }

    private void confirmDeleteSeries() {
        if (appointment == null || appointment.getRecurrenceGroupId() == null) return;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_series)
                .setMessage(R.string.delete_series_confirm)
                .setPositiveButton(R.string.confirm, (dialog, which) ->
                        appointmentRepository.deleteByRecurrenceGroup(
                                appointment.getRecurrenceGroupId(),
                                new AppointmentRepository.Callback<Void>() {
                                    @Override
                                    public void onResult(Void result) {
                                        com.medcare.app.background.BackgroundScheduler
                                                .rescheduleAll(requireContext());
                                        if (isAdded()) {
                                            Navigation.findNavController(requireView()).navigateUp();
                                        }
                                    }
                                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showRescheduleDialog() {
        if (appointment == null) return;
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = getResources().getDimensionPixelSize(R.dimen.margin_large);
        layout.setPadding(pad, pad, pad, pad);

        final String[] pendingDate = {appointment.getDate()};
        final String[] pendingTime = {appointment.getTime()};

        TextView dateValue = buildPickerRow(layout, R.string.reschedule_date, pendingDate[0]);
        dateValue.setOnClickListener(v -> showRescheduleDatePicker(pendingDate, dateValue));
        TextView timeValue = buildPickerRow(layout, R.string.reschedule_time, pendingTime[0]);
        timeValue.setOnClickListener(v -> showRescheduleTimePicker(pendingTime, timeValue));

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.reschedule)
                .setView(layout)
                .setPositiveButton(R.string.reschedule, (dialog, which) ->
                        validateAndApplyReschedule(pendingDate[0], pendingTime[0]))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private TextView buildPickerRow(LinearLayout parent, int labelRes, String value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int smallPad = getResources().getDimensionPixelSize(R.dimen.margin_small);
        row.setPadding(smallPad, smallPad, smallPad, smallPad);
        row.setBackgroundResource(android.R.drawable.list_selector_background);

        TextView label = new TextView(requireContext());
        label.setText(labelRes);
        label.setTextColor(MaterialColors.getColor(requireContext(),
                com.google.android.material.R.attr.colorOnSurfaceVariant, Color.WHITE));
        label.setTextSize(14);

        TextView valueView = new TextView(requireContext());
        valueView.setText(value);
        valueView.setTextColor(MaterialColors.getColor(requireContext(),
                com.google.android.material.R.attr.colorPrimary, Color.WHITE));
        valueView.setTextSize(16);
        valueView.setTextIsSelectable(false);

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(label, labelParams);
        row.addView(valueView);
        parent.addView(row);
        return valueView;
    }

    private void showRescheduleDatePicker(final String[] pendingDate, final TextView value) {
        Calendar calendar = Calendar.getInstance();
        try {
            String[] parts = pendingDate[0].split("/");
            calendar.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[0]));
        } catch (Exception ignored) {}
        DatePickerDialog picker = new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    String formatted = String.format("%02d/%02d/%04d", day, month + 1, year);
                    pendingDate[0] = formatted;
                    value.setText(formatted);
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        Calendar min = Calendar.getInstance();
        picker.getDatePicker().setMinDate(min.getTimeInMillis());
        Calendar max = Calendar.getInstance();
        max.add(Calendar.YEAR, 1);
        picker.getDatePicker().setMaxDate(max.getTimeInMillis());
        picker.show();
    }

    private void showRescheduleTimePicker(final String[] pendingTime, final TextView value) {
        Calendar calendar = Calendar.getInstance();
        try {
            String[] parts = pendingTime[0].split(":");
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        } catch (Exception ignored) {}
        TimePickerDialog picker = new TimePickerDialog(requireContext(),
                (view, hour, minute) -> {
                    String formatted = String.format("%02d:%02d", hour, minute);
                    pendingTime[0] = formatted;
                    value.setText(formatted);
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        picker.show();
    }

    private void validateAndApplyReschedule(String newDate, String newTime) {
        if (isDateTimeInPast(newDate, newTime)) {
            if (isAdded()) {
                Snackbar.make(requireView(), R.string.appointment_past_error, Snackbar.LENGTH_SHORT).show();
            }
            return;
        }
        long ownerId = preferencesManager.getLoggedInUserId();
        appointmentRepository.getAppointmentsByDate(newDate, ownerId, new AppointmentRepository.Callback<List<Appointment>>() {
            @Override
            public void onResult(List<Appointment> existing) {
                if (!isAdded()) return;
                long newStart = parseTimeToMinutes(newTime);
                if (newStart < 0) {
                    onRescheduleConfirmed(newDate, newTime);
                    return;
                }
                long newEnd = newStart + appointment.getDuration();
                for (Appointment a : existing) {
                    if (a.getId() == appointment.getId()) continue;
                    long start = parseTimeToMinutes(a.getTime());
                    if (start < 0) continue;
                    long end = start + a.getDuration();
                    if (newStart < end && start < newEnd) {
                        if (isAdded()) {
                            Snackbar.make(requireView(), R.string.time_conflict, Snackbar.LENGTH_SHORT).show();
                        }
                        return;
                    }
                }
                onRescheduleConfirmed(newDate, newTime);
            }
        });
    }

    private void onRescheduleConfirmed(final String newDate, final String newTime) {
        Long groupId = appointment.getRecurrenceGroupId();
        if (groupId != null) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.reschedule)
                    .setItems(new CharSequence[]{
                            getString(R.string.reschedule_this_only),
                            getString(R.string.reschedule_entire_series)
                    }, (dialog, which) -> {
                        if (which == 0) {
                            rescheduleThis(newDate, newTime);
                        } else {
                            rescheduleSeries(newDate, newTime);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            rescheduleThis(newDate, newTime);
        }
    }

    private void rescheduleThis(String newDate, String newTime) {
        appointment.setDate(newDate);
        appointment.setTime(newTime);
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        clearReminderState(appointment.getId());
        appointmentRepository.update(appointment, new AppointmentRepository.Callback<Void>() {
            @Override
            public void onResult(Void result) {
                scheduleFollowUp(appointment);
                if (isAdded()) {
                    Snackbar.make(requireView(), R.string.reschedule_success, Snackbar.LENGTH_SHORT).show();
                    loadAppointment();
                }
            }
        });
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

    private void rescheduleSeries(String newDate, String newTime) {
        long oldEpoch = AppointmentRepository.toEpochMillis(appointment.getDate(), appointment.getTime());
        long newEpoch = AppointmentRepository.toEpochMillis(newDate, newTime);
        if (oldEpoch <= 0 || newEpoch <= 0) return;
        final long delta = newEpoch - oldEpoch;
        appointmentRepository.rescheduleSeries(appointment.getRecurrenceGroupId(), delta,
                new AppointmentRepository.Callback<Void>() {
                    @Override
                    public void onResult(Void result) {
                        com.medcare.app.background.BackgroundScheduler
                                .rescheduleAll(requireContext());
                        if (isAdded()) {
                            Snackbar.make(requireView(), R.string.reschedule_success, Snackbar.LENGTH_SHORT).show();
                            loadAppointment();
                        }
                    }
                });
    }

    private void clearReminderState(long id) {
        PreferencesManager prefs = new PreferencesManager(requireContext());
        Set<String> prompted = new HashSet<>(prefs.getFollowUpPromptedIds());
        prompted.remove(String.valueOf(id));
        prefs.setFollowUpPromptedIds(prompted);
        Set<String> notified = new HashSet<>(prefs.getNotifiedReminderKeys());
        notified.remove(id + ":" + prefs.getReminderLeadMinutes());
        prefs.setNotifiedReminderKeys(notified);
        android.app.NotificationManager nm = requireContext()
                .getSystemService(android.app.NotificationManager.class);
        if (nm != null) {
            nm.cancel((int) (id % Integer.MAX_VALUE));
        }
    }

    private boolean isDateTimeInPast(String date, String time) {
        try {
            String[] d = date.split("/");
            if (d.length != 3) return false;
            String[] t = time.split(":");
            if (t.length != 2) return false;
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(d[2]), Integer.parseInt(d[1]) - 1,
                    Integer.parseInt(d[0]), Integer.parseInt(t[0]),
                    Integer.parseInt(t[1]), 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis() < System.currentTimeMillis();
        } catch (NumberFormatException e) {
            return false;
        }
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
}

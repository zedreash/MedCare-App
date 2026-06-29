package com.medcare.app.ui.dashboard;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.card.MaterialCardView;
import com.medcare.app.R;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.User;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.PreferencesManager;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private PatientRepository patientRepository;
    private AppointmentRepository appointmentRepository;
    private UserRepository userRepository;
    private PreferencesManager preferencesManager;
    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;
    private TextView welcomeText;
    private TextView totalPatientsText;
    private TextView totalAppointmentsText;
    private TextView todayCountText;
    private TextView totalPatientsLabel;
    private TextView todayCountLabel;
    private TextView totalAppointmentsLabel;
    private MaterialCardView totalPatientsCard;
    private MaterialCardView todayCountCard;
    private MaterialCardView totalAppointmentsCard;
    private LinearLayout scheduleContainer;
    private TextView noAppointmentsText;
    private View rootView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        patientRepository = new PatientRepository(requireContext());
        appointmentRepository = new AppointmentRepository(requireContext());
        userRepository = new UserRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());
        initViews(view);
        setupClickListeners(view);
        loadData();
    }

    private void initViews(View view) {
        welcomeText = view.findViewById(R.id.welcome_text);
        totalPatientsText = view.findViewById(R.id.total_patients_value);
        totalAppointmentsText = view.findViewById(R.id.total_appointments_value);
        todayCountText = view.findViewById(R.id.today_count_value);
        totalPatientsLabel = view.findViewById(R.id.total_patients_label);
        todayCountLabel = view.findViewById(R.id.today_count_label);
        totalAppointmentsLabel = view.findViewById(R.id.total_appointments_label);
        totalPatientsCard = view.findViewById(R.id.total_patients_card);
        todayCountCard = view.findViewById(R.id.today_count_card);
        totalAppointmentsCard = view.findViewById(R.id.total_appointments_card);
        scheduleContainer = view.findViewById(R.id.schedule_container);
        noAppointmentsText = view.findViewById(R.id.no_appointments_text);
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.total_patients_card).setOnClickListener(v ->
                Navigation.findNavController(rootView)
                        .navigate(R.id.action_dashboard_to_patientList));
        view.findViewById(R.id.total_appointments_card).setOnClickListener(v ->
                Navigation.findNavController(rootView)
                        .navigate(R.id.action_dashboard_to_appointmentList));
        view.findViewById(R.id.today_count_card).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("filterDate", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
            Navigation.findNavController(rootView)
                    .navigate(R.id.action_dashboard_to_appointmentList, args);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
        startAutoRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    private void loadData() {
        long userId = preferencesManager.getLoggedInUserId();
        User user = userRepository.getUserById(userId);
        if (user != null) {
            welcomeText.setText(getString(R.string.welcome_back, user.getFullName()));
        } else {
            welcomeText.setVisibility(View.GONE);
        }

        int patientCount = patientRepository.getPatientCount();
        int appointmentCount = appointmentRepository.getAppointmentCount();
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        int todayCount = appointmentRepository.getAppointmentCountByDate(today);
        List<Appointment> todayAppointments = appointmentRepository.getAppointmentsByDate(today);

        totalPatientsText.setText(String.valueOf(patientCount));
        totalAppointmentsText.setText(String.valueOf(appointmentCount));
        todayCountText.setText(String.valueOf(todayCount));

        scheduleContainer.removeAllViews();
        if (todayAppointments.isEmpty()) {
            noAppointmentsText.setVisibility(View.VISIBLE);
        } else {
            noAppointmentsText.setVisibility(View.GONE);
            for (Appointment appointment : todayAppointments) {
                View itemView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_appointment_minimal, scheduleContainer, false);
                TextView nameText = itemView.findViewById(R.id.appointment_name_text);
                TextView dateText = itemView.findViewById(R.id.appointment_date_text);
                TextView timeText = itemView.findViewById(R.id.appointment_time_text);
                String patientName = getPatientName(appointment.getPatientId());
                String apptName = appointment.getName();
                if (apptName != null && !apptName.isEmpty()) {
                    nameText.setText(apptName + " \u00B7 " + patientName);
                } else {
                    nameText.setText(patientName);
                }
                String time = appointment.getTime() != null ? appointment.getTime() : "";
                String durationStr = appointment.getDuration() > 0
                        ? appointment.getDuration() + " min"
                        : "";
                int durationInt = appointment.getDuration();

                dateText.setText(time);

                boolean isPast = isPastAppointment(time, durationInt);
                boolean isNow = false;
                String statusText = "";

                if (!isPast && !time.isEmpty()) {
                    Calendar apptCal = parseToCalendar(time);
                    if (apptCal != null) {
                        Calendar now = Calendar.getInstance();
                        if (!now.before(apptCal)) {
                            Calendar endCal = (Calendar) apptCal.clone();
                            endCal.add(Calendar.MINUTE, Math.max(durationInt, 0));
                            if (now.before(endCal)) {
                                isNow = true;
                            }
                        }
                    }
                }

                if (isNow) {
                    timeText.setText(formatTimeLine(durationStr, getString(R.string.now)));
                    timeText.setTextColor(requireContext().getColor(R.color.success));
                } else {
                    timeText.setTextColor(requireContext().getColor(R.color.text_secondary));
                    if (!isPast) {
                        statusText = computeTimeUntil(time);
                    }
                    timeText.setText(formatTimeLine(durationStr, statusText));
                }

                if (isPast) {
                    nameText.setAlpha(0.5f);
                    dateText.setAlpha(0.5f);
                    timeText.setAlpha(0.5f);
                    nameText.setPaintFlags(nameText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    dateText.setPaintFlags(dateText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    timeText.setPaintFlags(timeText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    nameText.setAlpha(1f);
                    dateText.setAlpha(1f);
                    timeText.setAlpha(1f);
                    nameText.setPaintFlags(nameText.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    dateText.setPaintFlags(dateText.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    timeText.setPaintFlags(timeText.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                }

                itemView.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putInt("appointmentId", (int) appointment.getId());
                    Navigation.findNavController(rootView)
                            .navigate(R.id.action_dashboard_to_appointmentDetail, args);
                });
                scheduleContainer.addView(itemView);
            }
        }

        rootView.post(() -> {
            if (fitTextsToWidth()) {
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        equalizeCardHeights();
                    }
                });
            } else {
                equalizeCardHeights();
            }
        });
    }

    private String formatTimeLine(String duration, String timeUntil) {
        if (duration.isEmpty() && timeUntil.isEmpty()) return "";
        if (duration.isEmpty()) return timeUntil;
        if (timeUntil.isEmpty()) return duration;
        return duration + " \u00B7 " + timeUntil;
    }

    private String computeTimeUntil(String appointmentTime) {
        Calendar apptCal = parseToCalendar(appointmentTime);
        if (apptCal == null) return "";

        Calendar now = Calendar.getInstance();
        if (apptCal.before(now)) return "";

        long diffMs = apptCal.getTimeInMillis() - now.getTimeInMillis();
        long diffMin = diffMs / 60000;

        if (diffMin < 1) return "";
        if (diffMin < 60) return "In " + diffMin + " min";

        long hours = diffMin / 60;
        long mins = diffMin % 60;

        if (mins == 0) return "In " + hours + "h";
        return "In " + hours + "h " + mins + "min";
    }

    private boolean isPastAppointment(String time, int duration) {
        Calendar apptCal = parseToCalendar(time);
        if (apptCal == null) return true;

        Calendar endCal = (Calendar) apptCal.clone();
        endCal.add(Calendar.MINUTE, Math.max(duration, 0));
        return Calendar.getInstance().after(endCal);
    }

    private Calendar parseToCalendar(String time) {
        if (time == null || time.isEmpty()) return null;
        String[] parts = time.split(":");
        if (parts.length < 2) return null;
        try {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshHandler = new Handler(Looper.getMainLooper());
        autoRefreshRunnable = () -> {
            if (isAdded()) {
                loadData();
                startAutoRefresh();
            }
        };
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 30000);
    }

    private void stopAutoRefresh() {
        if (autoRefreshHandler != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
            autoRefreshHandler = null;
            autoRefreshRunnable = null;
        }
    }

    private void equalizeCardHeights() {
        int maxHeight = Math.max(totalPatientsCard.getHeight(),
                Math.max(todayCountCard.getHeight(), totalAppointmentsCard.getHeight()));
        if (maxHeight <= 0) return;
        LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams) totalPatientsCard.getLayoutParams();
        LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) todayCountCard.getLayoutParams();
        LinearLayout.LayoutParams lp3 = (LinearLayout.LayoutParams) totalAppointmentsCard.getLayoutParams();
        if (lp1.height != maxHeight) {
            lp1.height = maxHeight;
            totalPatientsCard.requestLayout();
        }
        if (lp2.height != maxHeight) {
            lp2.height = maxHeight;
            todayCountCard.requestLayout();
        }
        if (lp3.height != maxHeight) {
            lp3.height = maxHeight;
            totalAppointmentsCard.requestLayout();
        }
    }

    private boolean fitTextsToWidth() {
        float s1 = computeTargetSp(totalPatientsLabel);
        float s2 = computeTargetSp(todayCountLabel);
        float s3 = computeTargetSp(totalAppointmentsLabel);

        float[] sizes = {s1, s2, s3};
        float minSize = Float.MAX_VALUE;
        boolean anyResized = false;
        for (float s : sizes) {
            if (s > 0) {
                minSize = Math.min(minSize, s);
                anyResized = true;
            }
        }

        if (!anyResized) return false;

        totalPatientsLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, minSize);
        todayCountLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, minSize);
        totalAppointmentsLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, minSize);
        return true;
    }

    private float computeTargetSp(TextView textView) {
        int availableWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
        if (availableWidth <= 0) return -1;

        String text = textView.getText().toString();
        if (text.isEmpty()) return -1;

        Paint paint = textView.getPaint();
        float textWidth = paint.measureText(text);
        if (textWidth <= availableWidth) return -1;

        float currentSizeSp = textView.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
        return currentSizeSp * availableWidth / textWidth;
    }

    private String getPatientName(long patientId) {
        com.medcare.app.data.entity.Patient patient = patientRepository.getPatientById(patientId);
        return patient != null ? patient.getFullName() : "";
    }
}

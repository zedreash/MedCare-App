package com.medcare.app.ui.dashboard;
import android.os.Bundle;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.medcare.app.R;
import com.medcare.app.adapter.TodayAppointmentAdapter;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.entity.User;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.data.repository.UserRepository;
import com.medcare.app.utils.PreferencesManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {
    private PatientRepository patientRepository;
    private AppointmentRepository appointmentRepository;
    private UserRepository userRepository;
    private PreferencesManager preferencesManager;
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
    private RecyclerView scheduleRecycler;
    private TodayAppointmentAdapter scheduleAdapter;
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
        scheduleRecycler = view.findViewById(R.id.schedule_recycler);
        scheduleRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        scheduleAdapter = new TodayAppointmentAdapter(appointment -> {
            Bundle args = new Bundle();
            args.putInt("appointmentId", (int) appointment.getId());
            Navigation.findNavController(rootView)
                    .navigate(R.id.action_dashboard_to_appointmentDetail, args);
        });
        scheduleRecycler.setAdapter(scheduleAdapter);
        scheduleRecycler.setNestedScrollingEnabled(false);
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
        view.findViewById(R.id.open_calendar_button).setOnClickListener(v ->
                Navigation.findNavController(rootView)
                        .navigate(R.id.action_dashboard_to_calendar));
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

        long ownerId = preferencesManager.getLoggedInUserId();
        int patientCount = patientRepository.getPatientCount(ownerId);
        int appointmentCount = appointmentRepository.getAppointmentCount(ownerId);
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        int todayCount = appointmentRepository.getAppointmentCountByDate(today, ownerId);
        List<Appointment> todayAppointments = appointmentRepository.getAppointmentsByDate(today, ownerId);

        totalPatientsText.setText(String.valueOf(patientCount));
        totalAppointmentsText.setText(String.valueOf(appointmentCount));
        todayCountText.setText(String.valueOf(todayCount));

        Map<Long, String> nameMap = new HashMap<>();
        for (Appointment a : todayAppointments) {
            if (!nameMap.containsKey(a.getPatientId())) {
                Patient p = patientRepository.getPatientById(a.getPatientId(), preferencesManager.getLoggedInUserId());
                nameMap.put(a.getPatientId(), p != null ? p.getFullName() : "Unknown");
            }
        }
        scheduleAdapter.setAppointments(todayAppointments, nameMap);
        if (todayAppointments.isEmpty()) {
            noAppointmentsText.setVisibility(View.VISIBLE);
            scheduleRecycler.setVisibility(View.GONE);
        } else {
            noAppointmentsText.setVisibility(View.GONE);
            scheduleRecycler.setVisibility(View.VISIBLE);
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

    private void startAutoRefresh() {
        stopAutoRefresh();
        if (rootView == null) return;
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || rootView == null) return;
                loadData();
                rootView.postDelayed(this, 30000);
            }
        };
        rootView.postDelayed(autoRefreshRunnable, 30000);
    }

    private void stopAutoRefresh() {
        if (rootView != null && autoRefreshRunnable != null) {
            rootView.removeCallbacks(autoRefreshRunnable);
        }
        autoRefreshRunnable = null;
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

}

package com.medcare.app.ui.dashboard;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
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
        long ownerId = preferencesManager.getLoggedInUserId();
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        userRepository.getUserById(ownerId, new UserRepository.Callback<User>() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    welcomeText.setText(getString(R.string.welcome_back, user.getFullName()));
                } else {
                    welcomeText.setVisibility(View.GONE);
                }

                patientRepository.getPatientCount(ownerId, new PatientRepository.Callback<Integer>() {
                    @Override
                    public void onResult(Integer patientCount) {
                        totalPatientsText.setText(String.valueOf(patientCount));

                        appointmentRepository.getAppointmentCount(ownerId, new AppointmentRepository.Callback<Integer>() {
                            @Override
                            public void onResult(Integer appointmentCount) {
                                totalAppointmentsText.setText(String.valueOf(appointmentCount));

                                appointmentRepository.getAppointmentCountByDate(today, ownerId, new AppointmentRepository.Callback<Integer>() {
                                    @Override
                                    public void onResult(Integer todayCount) {
                                        todayCountText.setText(String.valueOf(todayCount));

                                        appointmentRepository.getAppointmentsByDate(today, ownerId, new AppointmentRepository.Callback<List<Appointment>>() {
                                            @Override
                                            public void onResult(List<Appointment> todayAppointments) {
                                                Map<Long, String> nameMap = new HashMap<>();
                                                java.util.List<Long> uniqueIds = new java.util.ArrayList<>();
                                                for (Appointment a : todayAppointments) {
                                                    if (!nameMap.containsKey(a.getPatientId())) {
                                                        nameMap.put(a.getPatientId(), "Unknown");
                                                        uniqueIds.add(a.getPatientId());
                                                    }
                                                }

                                                Runnable updateUI = () -> {
                                                    scheduleAdapter.setAppointments(todayAppointments, nameMap);
                                                    if (todayAppointments.isEmpty()) {
                                                        noAppointmentsText.setVisibility(View.VISIBLE);
                                                        scheduleRecycler.setVisibility(View.GONE);
                                                    } else {
                                                        noAppointmentsText.setVisibility(View.GONE);
                                                        scheduleRecycler.setVisibility(View.VISIBLE);
                                                    }
                                                };

                                                if (uniqueIds.isEmpty()) {
                                                    updateUI.run();
                                                } else {
                                                    final int[] remaining = {uniqueIds.size()};
                                                    for (long pid : uniqueIds) {
                                                        patientRepository.getPatientById(pid, ownerId, new PatientRepository.Callback<Patient>() {
                                                            @Override
                                                            public void onResult(Patient p) {
                                                                nameMap.put(pid, p != null ? p.getFullName() : "Unknown");
                                                                remaining[0]--;
                                                                if (remaining[0] == 0) {
                                                                    updateUI.run();
                                                                }
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

        rootView.post(this::equalizeCardHeights);
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

}

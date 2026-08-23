package com.medcare.app.ui.calendar;

import android.graphics.Typeface;
import android.os.Bundle;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.medcare.app.R;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.utils.PreferencesManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private enum ViewMode { DAY, SIX_DAY, MONTH }

    private AppointmentRepository appointmentRepo;
    private PatientRepository patientRepo;
    private PreferencesManager preferencesManager;
    private boolean scrollToNowOnNextRender = false;

    private TextView dateHeaderText;
    private ImageButton prevButton, nextButton;
    private MaterialButton todayButton;
    private Chip dayChip, sixDayChip, monthChip;
    private FrameLayout calendarContent;

    private ViewMode currentMode = ViewMode.DAY;
    private Calendar focusedDate = Calendar.getInstance();
    private boolean[] expandedRows = new boolean[24];
    private int pendingScrollY = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appointmentRepo = new AppointmentRepository(requireContext());
        patientRepo = new PatientRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());
        focusedDate = Calendar.getInstance();

        dateHeaderText = view.findViewById(R.id.date_header_text);
        prevButton = view.findViewById(R.id.prev_button);
        nextButton = view.findViewById(R.id.next_button);
        todayButton = view.findViewById(R.id.today_button);
        dayChip = view.findViewById(R.id.day_chip);
        sixDayChip = view.findViewById(R.id.six_day_chip);
        monthChip = view.findViewById(R.id.month_chip);
        calendarContent = view.findViewById(R.id.calendar_content);

        prevButton.setOnClickListener(v -> navigateDate(-1));
        nextButton.setOnClickListener(v -> navigateDate(1));
        todayButton.setOnClickListener(v -> {
            focusedDate = Calendar.getInstance();
            resetExpandedRows();
            updateDateHeader();
            refresh();
        });

        dayChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && currentMode != ViewMode.DAY) {
                currentMode = ViewMode.DAY;
                refresh();
            }
        });
        sixDayChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && currentMode != ViewMode.SIX_DAY) {
                currentMode = ViewMode.SIX_DAY;
                refresh();
            }
        });
        monthChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && currentMode != ViewMode.MONTH) {
                currentMode = ViewMode.MONTH;
                refresh();
            }
        });

        if (savedInstanceState != null) {
            focusedDate.setTimeInMillis(savedInstanceState.getLong("focusedDate", focusedDate.getTimeInMillis()));
            currentMode = ViewMode.valueOf(savedInstanceState.getString("currentMode", currentMode.name()));
            boolean[] saved = savedInstanceState.getBooleanArray("expandedRows");
            if (saved != null && saved.length == 24) {
                expandedRows = saved;
            }
        }

        switch (currentMode) {
            case DAY: dayChip.setChecked(true); break;
            case SIX_DAY: sixDayChip.setChecked(true); break;
            case MONTH: monthChip.setChecked(true); break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh();
    }

    private void navigateDate(int direction) {
        switch (currentMode) {
            case DAY:
                focusedDate.add(Calendar.DAY_OF_MONTH, direction);
                break;
            case SIX_DAY:
                focusedDate.add(Calendar.DAY_OF_MONTH, direction * 6);
                break;
            case MONTH:
                focusedDate.add(Calendar.MONTH, direction);
                break;
        }
        resetExpandedRows();
        updateDateHeader();
        refresh();
    }

    private void updateDateHeader() {
        Locale locale = Locale.getDefault();
        switch (currentMode) {
            case DAY: {
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy", locale);
                dateHeaderText.setText(sdf.format(focusedDate.getTime()));
                break;
            }
            case SIX_DAY: {
                Calendar end = (Calendar) focusedDate.clone();
                end.add(Calendar.DAY_OF_MONTH, 5);
                SimpleDateFormat fmt = new SimpleDateFormat("dd MMM", locale);
                SimpleDateFormat yearFmt = new SimpleDateFormat("dd MMM yyyy", locale);
                if (focusedDate.get(Calendar.YEAR) != end.get(Calendar.YEAR)) {
                    dateHeaderText.setText(yearFmt.format(focusedDate.getTime()) + " - " + yearFmt.format(end.getTime()));
                } else {
                    dateHeaderText.setText(fmt.format(focusedDate.getTime()) + " - " + fmt.format(end.getTime()) + " " + focusedDate.get(Calendar.YEAR));
                }
                break;
            }
            case MONTH: {
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", locale);
                dateHeaderText.setText(sdf.format(focusedDate.getTime()));
                break;
            }
        }
    }

    private void refresh() {
        updateDateHeader();
        calendarContent.removeAllViews();
        scrollToNowOnNextRender = true;
        if (currentMode != ViewMode.DAY) {
            resetExpandedRows();
        }
        switch (currentMode) {
            case DAY:
                renderDailyView();
                break;
            case SIX_DAY:
                renderSixDayView();
                break;
            case MONTH:
                renderMonthView();
                break;
        }
    }

    private void resetExpandedRows() {
        expandedRows = new boolean[24];
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong("focusedDate", focusedDate.getTimeInMillis());
        outState.putString("currentMode", currentMode.name());
        outState.putBooleanArray("expandedRows", expandedRows);
        super.onSaveInstanceState(outState);
    }

    private void renderDailyView() {
        String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(focusedDate.getTime());
        appointmentRepo.getAppointmentsByDate(dateStr, preferencesManager.getLoggedInUserId(), new AppointmentRepository.Callback<List<Appointment>>() {
            @Override
            public void onResult(List<Appointment> appointments) {
                if (!isAdded()) return;
                ScrollView scrollView = new ScrollView(requireContext());
                scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                FrameLayout contentFrame = new FrameLayout(requireContext());
                contentFrame.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

                LinearLayout hourLayout = new LinearLayout(requireContext());
                hourLayout.setOrientation(LinearLayout.VERTICAL);
                hourLayout.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

                int hourHeightPx = dpToPx(60);
                float minuteHeightPx = (float) hourHeightPx / 60f;

                if (appointments.isEmpty()) {
                    TextView emptyText = new TextView(requireContext());
                    emptyText.setText(getString(R.string.no_appointments_day));
                    emptyText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
                    emptyText.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, ContextCompat.getColor(requireContext(), R.color.text_secondary)));
                    emptyText.setGravity(Gravity.CENTER);
                    emptyText.setPadding(0, dpToPx(80), 0, 0);
                    hourLayout.addView(emptyText);
                }

                for (int hour = 0; hour < 24; hour++) {
                    List<Appointment> hourAppts = new java.util.ArrayList<>();
                    for (Appointment appt : appointments) {
                        if (appt.getTime() != null && !appt.getTime().isEmpty()) {
                            String[] parts = appt.getTime().split(":");
                            try {
                                int apptHour = Integer.parseInt(parts[0]);
                                if (apptHour == hour) {
                                    hourAppts.add(appt);
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    int apptCount = hourAppts.size();
                    boolean isExpanded = expandedRows[hour];

                    LinearLayout row = new LinearLayout(requireContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);

                    if (apptCount > 1 && isExpanded) {
                        row.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    } else {
                        row.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, hourHeightPx));
                    }

                    TextView hourLabel = new TextView(requireContext());
                    hourLabel.setText(String.format(Locale.getDefault(), "%02d:00", hour));
                    hourLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
                    hourLabel.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, ContextCompat.getColor(requireContext(), R.color.text_secondary)));
                    hourLabel.setWidth(dpToPx(50));
                    hourLabel.setGravity(Gravity.TOP | Gravity.START);
                    hourLabel.setPadding(0, dpToPx(4), 0, 0);

                    LinearLayout apptContainer = new LinearLayout(requireContext());
                    apptContainer.setOrientation(LinearLayout.VERTICAL);
                    apptContainer.setLayoutParams(new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                    if (apptCount <= 1) {
                        for (Appointment appt : hourAppts) {
                            apptContainer.addView(createEventCard(appt));
                        }
                    } else if (!isExpanded) {
                        apptContainer.addView(createEventCard(hourAppts.get(0)));
                        apptContainer.addView(createExpandIndicator(hourAppts.size() - 1));
                    } else {
                        for (Appointment appt : hourAppts) {
                            apptContainer.addView(createEventCard(appt));
                        }
                        apptContainer.addView(createCollapseIndicator());
                    }

                    if (apptCount > 1) {
                        final int currentHour = hour;
                        final ScrollView sv = scrollView;
                        row.setOnClickListener(v -> {
                            expandedRows[currentHour] = !expandedRows[currentHour];
                            pendingScrollY = sv.getScrollY();
                            refresh();
                        });
                    }

                    View divider = new View(requireContext());
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    divider.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorOutlineVariant, ContextCompat.getColor(requireContext(), R.color.divider)));

                    row.addView(hourLabel);
                    row.addView(apptContainer);
                    if (!appointments.isEmpty()) {
                        hourLayout.addView(row);
                        hourLayout.addView(divider);
                    }
                }

                contentFrame.addView(hourLayout);

                boolean hasAppointments = !appointments.isEmpty();
                Calendar now = Calendar.getInstance();
                boolean isToday = isSameDay(focusedDate, now);
                if (hasAppointments && isToday) {
                    int minutesSinceMidnight = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

                    View nowLine = new View(requireContext());
                    nowLine.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorError, ContextCompat.getColor(requireContext(), R.color.error)));
                    nowLine.setEnabled(false);

                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(3));
                    lp.topMargin = (int) (minutesSinceMidnight * minuteHeightPx);
                    nowLine.setLayoutParams(lp);

                    contentFrame.addView(nowLine);

                    if (scrollToNowOnNextRender && pendingScrollY < 0) {
                        scrollToNowOnNextRender = false;
                        int scrollTo = (int) (minutesSinceMidnight * minuteHeightPx) - dpToPx(200);
                        if (scrollTo < 0) scrollTo = 0;
                        final int finalScrollTo = scrollTo;
                        scrollView.post(() -> scrollView.scrollTo(0, finalScrollTo));
                    }
                }

                scrollView.addView(contentFrame);
                calendarContent.removeAllViews();
                calendarContent.addView(scrollView);

                if (pendingScrollY >= 0) {
                    final int target = pendingScrollY;
                    pendingScrollY = -1;
                    scrollToNowOnNextRender = false;
                    scrollView.post(() -> {
                        if (scrollView.getChildCount() > 0) {
                            scrollView.scrollTo(0, Math.min(target, scrollView.getChildAt(0).getHeight()));
                        }
                    });
                }
            }
        });
    }

    private View createEventCard(Appointment appointment) {
        MaterialCardView card = new MaterialCardView(requireContext(), null,
                com.google.android.material.R.attr.materialCardViewOutlinedStyle);
        int marginPx = dpToPx(2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, marginPx, 0, marginPx);
        card.setLayoutParams(lp);
        card.setContentPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        card.setRadius(dpToPx(6));
        card.setStrokeWidth(0);
        card.setCardElevation(dpToPx(2));

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);

        TextView nameText = new TextView(requireContext());
        nameText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        String apptName = appointment.getName();
        nameText.setText(apptName != null && !apptName.isEmpty() ? apptName : "");
        nameText.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        nameText.setMaxLines(1);

        patientRepo.getPatientById(appointment.getPatientId(), preferencesManager.getLoggedInUserId(), new PatientRepository.Callback<Patient>() {
            @Override
            public void onResult(Patient patient) {
                if (!isAdded()) return;
                String patientName = patient != null ? patient.getFullName() : "";
                if (apptName != null && !apptName.isEmpty()) {
                    nameText.setText(apptName + " \u00B7 " + patientName);
                } else {
                    nameText.setText(patientName);
                }
            }
        });

        TextView timeText = new TextView(requireContext());
        timeText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        timeText.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, ContextCompat.getColor(requireContext(), R.color.text_secondary)));
        String time = appointment.getTime() != null ? appointment.getTime() : "";
        if (appointment.getDuration() > 0) {
            timeText.setText(time + " - " + calculateEndTime(time, appointment.getDuration()));
        } else {
            timeText.setText(time);
        }

        inner.addView(nameText);
        inner.addView(timeText);
        card.addView(inner);

        card.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("appointmentId", (int) appointment.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_calendar_to_appointmentDetail, args);
        });

        return card;
    }

    private TextView createExpandIndicator(int moreCount) {
        TextView tv = new TextView(requireContext());
        tv.setText(String.format(Locale.getDefault(), getString(R.string.calendar_expand), moreCount));
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(themeColor(com.google.android.material.R.attr.colorPrimary, ContextCompat.getColor(requireContext(), R.color.primary)));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setPadding(0, dpToPx(2), 0, dpToPx(2));
        return tv;
    }

    private TextView createCollapseIndicator() {
        TextView tv = new TextView(requireContext());
        tv.setText(getString(R.string.calendar_collapse));
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(themeColor(com.google.android.material.R.attr.colorPrimary, ContextCompat.getColor(requireContext(), R.color.primary)));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setPadding(0, dpToPx(2), 0, dpToPx(2));
        return tv;
    }

    private void renderSixDayView() {
        appointmentRepo.getAllAppointments(preferencesManager.getLoggedInUserId(), new AppointmentRepository.Callback<List<Appointment>>() {
            @Override
            public void onResult(List<Appointment> allAppointments) {
                if (!isAdded()) return;
                ScrollView vScroll = new ScrollView(requireContext());
                vScroll.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                HorizontalScrollView hScroll = new HorizontalScrollView(requireContext());
                hScroll.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                LinearLayout container = new LinearLayout(requireContext());
                container.setOrientation(LinearLayout.HORIZONTAL);

                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int colWidthPx = screenWidth / 3;

                Calendar today = Calendar.getInstance();

        for (int i = 0; i < 6; i++) {
            Calendar day = (Calendar) focusedDate.clone();
            day.add(Calendar.DAY_OF_MONTH, i);
            String dayDateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(day.getTime());

            LinearLayout column = new LinearLayout(requireContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setLayoutParams(new LinearLayout.LayoutParams(colWidthPx, LinearLayout.LayoutParams.WRAP_CONTENT));
            column.setPadding(dpToPx(4), 0, dpToPx(4), 0);

            TextView header = new TextView(requireContext());
            SimpleDateFormat dayFmt = new SimpleDateFormat("EEE\ndd/MM", Locale.getDefault());
            header.setText(dayFmt.format(day.getTime()));
            header.setGravity(Gravity.CENTER);
            header.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
            header.setTypeface(header.getTypeface(), Typeface.BOLD);
            header.setPadding(0, dpToPx(8), 0, dpToPx(8));

            boolean isToday = isSameDay(day, today);
            if (isToday) {
                header.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorPrimary, ContextCompat.getColor(requireContext(), R.color.primary)));
                header.setTextColor(themeColor(com.google.android.material.R.attr.colorOnPrimary, ContextCompat.getColor(requireContext(), R.color.on_primary)));
            } else {
                header.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurface, ContextCompat.getColor(requireContext(), R.color.surface)));
            }

            final Calendar targetDay = (Calendar) day.clone();
            header.setOnClickListener(v -> {
                focusedDate = targetDay;
                currentMode = ViewMode.DAY;
                dayChip.setChecked(true);
                resetExpandedRows();
                refresh();
            });

            column.addView(header);

            for (Appointment appt : allAppointments) {
                if (appt.getDate() != null && appt.getDate().equals(dayDateStr)) {
                    String initialDisplay = appt.getName() != null && !appt.getName().isEmpty()
                            ? appt.getName() : "";

                    MaterialCardView chip = new MaterialCardView(requireContext(), null,
                            com.google.android.material.R.attr.materialCardViewOutlinedStyle);
                    chip.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    chip.setContentPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
                    chip.setRadius(dpToPx(6));
                    chip.setStrokeWidth(0);
                    chip.setCardElevation(dpToPx(1));
                    int chipMargin = dpToPx(2);
                    LinearLayout.LayoutParams chipLp = (LinearLayout.LayoutParams) chip.getLayoutParams();
                    chipLp.setMargins(0, chipMargin, 0, chipMargin);

                    LinearLayout chipInner = new LinearLayout(requireContext());
                    chipInner.setOrientation(LinearLayout.VERTICAL);

                    TextView chipTime = new TextView(requireContext());
                    chipTime.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall);
                    chipTime.setText(appt.getTime());
                    chipTime.setTextColor(themeColor(com.google.android.material.R.attr.colorPrimary, ContextCompat.getColor(requireContext(), R.color.primary)));
                    chipInner.addView(chipTime);

                    TextView chipText = new TextView(requireContext());
                    chipText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
                    chipText.setText(initialDisplay);
                    chipText.setMaxLines(2);
                    chipText.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    chipInner.addView(chipText);

                    chip.addView(chipInner);

                    patientRepo.getPatientById(appt.getPatientId(), preferencesManager.getLoggedInUserId(), new PatientRepository.Callback<Patient>() {
                        @Override
                        public void onResult(Patient patient) {
                            if (!isAdded()) return;
                            String patientName = patient != null ? patient.getFullName() : "";
                            String displayName = appt.getName() != null && !appt.getName().isEmpty()
                                    ? appt.getName() : patientName;
                            chipText.setText(displayName);
                        }
                    });

                    final Appointment fAppt = appt;
                    chip.setOnClickListener(v -> {
                        Bundle args = new Bundle();
                        args.putInt("appointmentId", (int) fAppt.getId());
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_calendar_to_appointmentDetail, args);
                    });

                    column.addView(chip);
                }
            }

            container.addView(column);
        }

                hScroll.addView(container);
                vScroll.addView(hScroll);
                calendarContent.removeAllViews();
                calendarContent.addView(vScroll);
            }
        });
    }

    private void renderMonthView() {
        appointmentRepo.getAllAppointments(preferencesManager.getLoggedInUserId(), new AppointmentRepository.Callback<List<Appointment>>() {
            @Override
            public void onResult(List<Appointment> allAppointments) {
                if (!isAdded()) return;
                Calendar firstOfMonth = (Calendar) focusedDate.clone();
                firstOfMonth.set(Calendar.DAY_OF_MONTH, 1);
                int firstDayOfWeek = firstOfMonth.get(Calendar.DAY_OF_WEEK);
                int daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
                Calendar today = Calendar.getInstance();

                String monthSuffix = String.format(Locale.getDefault(), "/%02d/%04d",
                        focusedDate.get(Calendar.MONTH) + 1, focusedDate.get(Calendar.YEAR));
                java.util.Map<Integer, Integer> dayCounts = new java.util.HashMap<>();
                for (Appointment a : allAppointments) {
                    String date = a.getDate();
                    if (date != null && date.endsWith(monthSuffix)) {
                        try {
                            int day = Integer.parseInt(date.substring(0, 2));
                            dayCounts.put(day, dayCounts.containsKey(day) ? dayCounts.get(day) + 1 : 1);
                        } catch (NumberFormatException ignored) {}
                    }
                }

                int localeFirstDay = firstOfMonth.getFirstDayOfWeek();
                int firstCol = (firstDayOfWeek - localeFirstDay + 7) % 7;

                ScrollView scrollView = new ScrollView(requireContext());
                scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                LinearLayout container = new LinearLayout(requireContext());
                container.setOrientation(LinearLayout.VERTICAL);

                LinearLayout headerRow = new LinearLayout(requireContext());
                headerRow.setOrientation(LinearLayout.HORIZONTAL);

                Calendar refCal = (Calendar) focusedDate.clone();
                refCal.set(Calendar.DAY_OF_WEEK, localeFirstDay);
                SimpleDateFormat dayNameFmt = new SimpleDateFormat("EEE", Locale.getDefault());

                for (int i = 0; i < 7; i++) {
                    TextView dayHeader = new TextView(requireContext());
                    dayHeader.setText(dayNameFmt.format(refCal.getTime()));
                    dayHeader.setGravity(Gravity.CENTER);
                    dayHeader.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
                    dayHeader.setTypeface(dayHeader.getTypeface(), Typeface.BOLD);
                    dayHeader.setLayoutParams(new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                    dayHeader.setPadding(0, dpToPx(8), 0, dpToPx(8));
                    headerRow.addView(dayHeader);
                    refCal.add(Calendar.DAY_OF_MONTH, 1);
                }

                container.addView(headerRow);

                LinearLayout weekRow = new LinearLayout(requireContext());
                weekRow.setOrientation(LinearLayout.HORIZONTAL);
                int cellSizePx = dpToPx(52);
                final int selectedDay = focusedDate.get(Calendar.DAY_OF_MONTH);

                for (int i = 0; i < firstCol; i++) {
                    TextView empty = new TextView(requireContext());
                    empty.setLayoutParams(new LinearLayout.LayoutParams(0, cellSizePx, 1));
                    weekRow.addView(empty);
                }

                for (int day = 1; day <= daysInMonth; day++) {
                    if (weekRow.getChildCount() == 7) {
                        container.addView(weekRow);
                        weekRow = new LinearLayout(requireContext());
                        weekRow.setOrientation(LinearLayout.HORIZONTAL);
                    }

                    final int currentDay = day;
                    boolean isToday = isSameMonthDay(focusedDate, today) && day == today.get(Calendar.DAY_OF_MONTH);
                    boolean isSelected = day == selectedDay;
                    int primaryColor = themeColor(com.google.android.material.R.attr.colorPrimary, ContextCompat.getColor(requireContext(), R.color.primary));
                    int onPrimaryColor = themeColor(com.google.android.material.R.attr.colorOnPrimary, ContextCompat.getColor(requireContext(), R.color.on_primary));
                    int containerColor = themeColor(com.google.android.material.R.attr.colorPrimaryContainer, ContextCompat.getColor(requireContext(), R.color.primary_container));
                    int onContainerColor = themeColor(com.google.android.material.R.attr.colorOnPrimaryContainer, ContextCompat.getColor(requireContext(), R.color.on_primary_container));

                    LinearLayout dayCell = new LinearLayout(requireContext());
                    dayCell.setOrientation(LinearLayout.VERTICAL);
                    dayCell.setGravity(Gravity.CENTER);
                    dayCell.setLayoutParams(new LinearLayout.LayoutParams(0, cellSizePx, 1));
                    dayCell.setClickable(true);
                    dayCell.setFocusable(true);

                    LinearLayout box = new LinearLayout(requireContext());
                    box.setOrientation(LinearLayout.VERTICAL);
                    box.setGravity(Gravity.CENTER);
                    box.setPadding(dpToPx(6), dpToPx(3), dpToPx(6), dpToPx(3));
                    LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    boxLp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
                    box.setLayoutParams(boxLp);
                    box.setMinimumHeight(dpToPx(42));

                    TextView dayNumber = new TextView(requireContext());
                    dayNumber.setText(String.valueOf(day));
                    dayNumber.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
                    dayNumber.setGravity(Gravity.CENTER);
                    dayNumber.setIncludeFontPadding(false);

                    int boxColor = 0;
                    int boxTextColor = 0;
                    if (isSelected) {
                        boxColor = primaryColor;
                        boxTextColor = onPrimaryColor;
                    } else if (isToday) {
                        boxColor = containerColor;
                        boxTextColor = onContainerColor;
                    }

                    int count = dayCounts.containsKey(day) ? dayCounts.get(day) : 0;
                    LinearLayout dotsRow = new LinearLayout(requireContext());
                    dotsRow.setOrientation(LinearLayout.HORIZONTAL);
                    dotsRow.setGravity(Gravity.CENTER);
                    dotsRow.setPadding(0, dpToPx(2), 0, 0);
                    int dotsToShow = Math.min(count, 3);
                    int dotColor = isSelected ? onPrimaryColor : (isToday ? onContainerColor : primaryColor);
                    for (int d = 0; d < dotsToShow; d++) {
                        View dot = new View(requireContext());
                        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dpToPx(4), dpToPx(4));
                        dotLp.setMargins(dpToPx(1), 0, dpToPx(1), 0);
                        dot.setLayoutParams(dotLp);
                        dot.setBackgroundResource(R.drawable.bg_dot);
                        dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dotColor));
                        dotsRow.addView(dot);
                    }
                    if (count > 3) {
                        TextView more = new TextView(requireContext());
                        more.setText("+" + (count - 3));
                        more.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
                        more.setIncludeFontPadding(false);
                        more.setSingleLine(true);
                        more.setMaxLines(1);
                        more.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                        more.setTextColor(dotColor);
                        more.setPadding(dpToPx(1), 0, 0, 0);
                        dotsRow.addView(more);
                    }

                    box.addView(dayNumber);
                    if (count > 0) {
                        box.addView(dotsRow);
                    }
                    if (boxColor != 0) {
                        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                        bg.setCornerRadius(dpToPx(10));
                        bg.setColor(boxColor);
                        box.setBackground(bg);
                        dayNumber.setTextColor(boxTextColor);
                    }

                    dayCell.addView(box);

                    dayCell.setOnClickListener(v -> {
                        focusedDate.set(Calendar.DAY_OF_MONTH, currentDay);
                        currentMode = ViewMode.DAY;
                        dayChip.setChecked(true);
                        refresh();
                    });

                    weekRow.addView(dayCell);
                }

                while (weekRow.getChildCount() < 7) {
                    TextView empty = new TextView(requireContext());
                    empty.setLayoutParams(new LinearLayout.LayoutParams(0, cellSizePx, 1));
                    weekRow.addView(empty);
                }
                container.addView(weekRow);

                scrollView.addView(container);
                calendarContent.removeAllViews();
                calendarContent.addView(scrollView);
            }
        });
    }

    private String calculateEndTime(String time, int duration) {
        if (time == null || time.isEmpty()) return "";
        String[] parts = time.split(":");
        try {
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            int totalMin = hour * 60 + min + duration;
            int endHour = (totalMin / 60) % 24;
            int endMin = totalMin % 60;
            return String.format(Locale.getDefault(), "%02d:%02d", endHour, endMin);
        } catch (NumberFormatException e) {
            return time;
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isSameMonthDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private int themeColor(int attr, int fallback) {
        return MaterialColors.getColor(requireContext(), attr, fallback);
    }
}
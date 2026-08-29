package com.medcare.app.ui.report;

import android.content.Context;
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

import com.medcare.app.R;
import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.utils.AppointmentStatus;
import com.medcare.app.utils.PdfExporter;
import com.medcare.app.utils.PreferencesManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportFragment extends Fragment {
    private LinearLayout container;
    private final List<String[]> reportRows = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = view.findViewById(R.id.report_container);
        view.findViewById(R.id.back_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.export_pdf_button).setOnClickListener(v -> sharePdf());
        loadStats();
    }

    private void sharePdf() {
        if (reportRows.isEmpty()) return;
        final Context context = requireContext();
        final String subtitle = getString(R.string.generated_on) + " "
                + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        final String[] header = {getString(R.string.metric), getString(R.string.value)};
        AppDatabase.getExecutor().execute(() -> {
            try {
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String fileName = "MedCare_Report_" + date + ".pdf";
                String[][] rows = reportRows.toArray(new String[0][]);
                File file = PdfExporter.writeReportPdf(context, fileName,
                        "MedCare " + getString(R.string.reports), subtitle, header, rows);
                AppDatabase.runOnMainThread(() -> {
                    if (isAdded()) {
                        PdfExporter.share(requireContext(), file);
                    }
                });
            } catch (Exception ignored) {
            }
        });
    }

    private void loadStats() {
        final Context context = requireContext();
        final long ownerId = new PreferencesManager(context).getLoggedInUserId();
        AppDatabase.getExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<Appointment> appointments = db.appointmentDao().getAllAppointments(ownerId);
            List<Patient> patients = db.patientDao().getAllPatients(ownerId);

            int totalAppointments = appointments.size();
            int totalPatients = patients.size();
            int scheduled = 0, completed = 0, noShow = 0, cancelled = 0, rescheduled = 0;
            int thisMonth = 0;
            int[] hourCounts = new int[24];
            Calendar now = Calendar.getInstance();
            for (Appointment a : appointments) {
                String status = a.getStatus();
                if (AppointmentStatus.SCHEDULED.equals(status)) scheduled++;
                else if (AppointmentStatus.COMPLETED.equals(status)) completed++;
                else if (AppointmentStatus.NO_SHOW.equals(status)) noShow++;
                else if (AppointmentStatus.CANCELLED.equals(status)) cancelled++;
                else if (AppointmentStatus.RESCHEDULED.equals(status)) rescheduled++;

                Calendar start = parseDate(a.getDate());
                if (start != null && start.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && start.get(Calendar.MONTH) == now.get(Calendar.MONTH)) {
                    thisMonth++;
                }
                Integer h = parseHour(a.getTime());
                if (h != null) hourCounts[h]++;
            }
            int busiestHour = -1;
            int max = 0;
            for (int i = 0; i < 24; i++) {
                if (hourCounts[i] > max) {
                    max = hourCounts[i];
                    busiestHour = i;
                }
            }
            final double noShowRate = totalAppointments == 0 ? 0.0
                    : (100.0 * noShow / totalAppointments);

            final int fTotalPatients = totalPatients;
            final int fTotalAppointments = totalAppointments;
            final int fThisMonth = thisMonth;
            final int fScheduled = scheduled;
            final int fCompleted = completed;
            final int fNoShow = noShow;
            final int fCancelled = cancelled;
            final int fRescheduled = rescheduled;
            final double fNoShowRate = noShowRate;
            final int fBusiestHour = busiestHour;

            AppDatabase.runOnMainThread(() -> {
                if (!isAdded()) return;
                addRow(getString(R.string.total_patients), String.valueOf(fTotalPatients));
                addRow(getString(R.string.total_appointments), String.valueOf(fTotalAppointments));
                addRow(getString(R.string.this_month), String.valueOf(fThisMonth));
                addRow(getString(R.string.status_scheduled), String.valueOf(fScheduled));
                addRow(getString(R.string.status_completed), String.valueOf(fCompleted));
                addRow(getString(R.string.status_no_show), String.valueOf(fNoShow));
                addRow(getString(R.string.status_cancelled), String.valueOf(fCancelled));
                addRow(getString(R.string.status_rescheduled), String.valueOf(fRescheduled));
                addRow(getString(R.string.no_show_rate), String.format(Locale.getDefault(), "%.1f%%", fNoShowRate));
                addRow(getString(R.string.busiest_hour),
                        fBusiestHour >= 0 ? String.format(Locale.getDefault(), "%02d:00", fBusiestHour) : "-");
            });
        });
    }

    private void addRow(String label, String value) {
        reportRows.add(new String[]{label, value});
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(6), 0, dpToPx(6));
        row.setLayoutParams(lp);

        TextView labelText = new TextView(requireContext());
        labelText.setText(label);
        labelText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        labelText.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(labelText);

        TextView valueText = new TextView(requireContext());
        valueText.setText(value);
        valueText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        valueText.setTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(),
                com.google.android.material.R.attr.colorPrimary,
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary)));
        row.addView(valueText);

        container.addView(row);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private Calendar parseDate(String date) {
        try {
            String[] p = date.split("/");
            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(p[2]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[0]));
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseHour(String time) {
        try {
            return Integer.parseInt(time.split(":")[0]);
        } catch (Exception e) {
            return null;
        }
    }
}
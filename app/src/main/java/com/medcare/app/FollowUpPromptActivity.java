package com.medcare.app;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.notifications.FollowUpScheduler;
import com.medcare.app.utils.AppointmentStatus;
import com.medcare.app.utils.PreferencesManager;

public class FollowUpPromptActivity extends AppCompatActivity {
    private long appointmentId = -1;
    private long ownerId = -1;
    private AppointmentRepository repo;
    private PreferencesManager prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appointmentId = getIntent().getLongExtra("id", -1);
        ownerId = getIntent().getLongExtra("ownerId", -1);
        if (ownerId == -1) {
            ownerId = new PreferencesManager(this).getLoggedInUserId();
        }
        if (appointmentId == -1 || ownerId == -1) {
            finish();
            return;
        }
        repo = new AppointmentRepository(this);
        prefs = new PreferencesManager(this);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        final TextView question = new TextView(this);
        question.setText(getString(R.string.followup_question));
        question.setTextSize(18);
        question.setPadding(0, 0, 0, pad);
        root.addView(question);

        root.addView(button(R.string.followup_showed_up,
                () -> setStatus(AppointmentStatus.COMPLETED)));
        root.addView(button(R.string.followup_no_show,
                () -> setStatus(AppointmentStatus.NO_SHOW)));
        root.addView(button(R.string.followup_ask_30,
                () -> FollowUpScheduler.scheduleOneOff(this, appointmentId, 30)));
        root.addView(button(R.string.followup_ask_60,
                () -> FollowUpScheduler.scheduleOneOff(this, appointmentId, 60)));

        setContentView(root);

        repo.getAppointmentById(appointmentId, ownerId, appointment -> {
            if (appointment != null && appointment.getName() != null
                    && !appointment.getName().isEmpty()) {
                String detail = appointment.getName()
                        + (appointment.getTime() != null ? " \u00B7 " + appointment.getTime() : "");
                question.setText(getString(R.string.followup_question) + "\n" + detail);
            }
        });
    }

    private MaterialButton button(int textRes, Runnable action) {
        MaterialButton b = new MaterialButton(this);
        b.setText(textRes);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        b.setOnClickListener(v -> {
            action.run();
            finish();
        });
        return b;
    }

    private void setStatus(String status) {
        repo.getAppointmentById(appointmentId, ownerId, appointment -> {
            if (appointment == null) {
                clearPrompted();
                return;
            }
            appointment.setStatus(status);
            repo.update(appointment, result -> clearPrompted());
        });
    }

    private void clearPrompted() {
        prefs.clearFollowUpPrompted(appointmentId);
        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        if (nm != null) {
            nm.cancel((int) (appointmentId % Integer.MAX_VALUE));
        }
    }
}
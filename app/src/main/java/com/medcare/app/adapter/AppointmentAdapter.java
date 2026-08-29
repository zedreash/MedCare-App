package com.medcare.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import com.medcare.app.R;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.utils.AppointmentStatus;
import com.medcare.app.utils.AvatarUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentAdapter extends BaseSwipeAdapter<AppointmentAdapter.AppointmentViewHolder> {

    private List<Appointment> appointments = new ArrayList<>();
    private Map<Long, String> patientNames = new HashMap<>();
    private OnAppointmentClickListener listener;
    private OnDeleteClickListener deleteListener;

    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Appointment appointment, int position);
    }

    public AppointmentAdapter(OnAppointmentClickListener listener, OnDeleteClickListener deleteListener) {
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        holder.bind(appointment);
        holder.bindSwipeState(this, position);
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public void setAppointments(List<Appointment> appointments, Map<Long, String> patientNames) {
        resetRevealed();
        this.appointments = appointments;
        this.patientNames = patientNames;
        notifyDataSetChanged();
    }

    class AppointmentViewHolder extends SwipeableViewHolder {
        private TextView nameText;
        private TextView patientText;
        private TextView dateText;
        private TextView timeText;
        private TextView durationText;
        private TextView notesText;
        private TextView statusText;
        private TextView avatarText;

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.appointment_name_text);
            patientText = itemView.findViewById(R.id.appointment_patient_text);
            dateText = itemView.findViewById(R.id.appointment_date_text);
            timeText = itemView.findViewById(R.id.appointment_time_text);
            durationText = itemView.findViewById(R.id.appointment_duration_text);
            notesText = itemView.findViewById(R.id.appointment_notes_text);
            statusText = itemView.findViewById(R.id.appointment_status_text);
            avatarText = itemView.findViewById(R.id.appointment_avatar);
        }

        void bind(Appointment appointment) {
            String n = patientNames.get(appointment.getPatientId());
            nameText.setText(appointment.getName());
            patientText.setText(n != null ? n : "Unknown");
            dateText.setText(appointment.getDate());
            timeText.setText(appointment.getTime());
            durationText.setText(appointment.getDuration() + "m");
            String notes = appointment.getNotes();
            notesText.setText(notes != null && !notes.isEmpty() ? notes : null);
            String status = appointment.getStatus();
            if (status != null && !status.equals(AppointmentStatus.SCHEDULED) && !status.isEmpty()) {
                statusText.setVisibility(android.view.View.VISIBLE);
                statusText.setText(itemView.getContext().getString(AppointmentStatus.labelRes(status)));
                int color;
                if (AppointmentStatus.CANCELLED.equals(status)) {
                    color = MaterialColors.getColor(itemView.getContext(),
                            com.google.android.material.R.attr.colorError,
                            ContextCompat.getColor(itemView.getContext(), R.color.error));
                } else if (AppointmentStatus.COMPLETED.equals(status)) {
                    color = ContextCompat.getColor(itemView.getContext(), R.color.success);
                } else if (AppointmentStatus.RESCHEDULED.equals(status)) {
                    color = MaterialColors.getColor(itemView.getContext(),
                            com.google.android.material.R.attr.colorPrimary,
                            ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                } else {
                    color = ContextCompat.getColor(itemView.getContext(), R.color.warning);
                }
                statusText.setTextColor(color);
            } else {
                statusText.setVisibility(android.view.View.GONE);
            }
            String initialSource = (n != null && !n.isEmpty()) ? n : appointment.getName();
            avatarText.setText(AvatarUtils.getInitials(initialSource));
        }

        @Override
        protected void onItemClick(int position) {
            if (listener != null) {
                listener.onAppointmentClick(appointments.get(position));
            }
        }

        @Override
        protected void onDeleteActionClick(int position) {
            deleteListener.onDeleteClick(appointments.get(position), position);
        }
    }
}

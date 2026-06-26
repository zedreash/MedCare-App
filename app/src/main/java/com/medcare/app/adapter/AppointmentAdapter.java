package com.medcare.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.medcare.app.R;
import com.medcare.app.data.entity.Appointment;

import java.util.ArrayList;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private List<Appointment> appointments = new ArrayList<>();
    private OnAppointmentClickListener listener;

    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
    }

    public AppointmentAdapter(OnAppointmentClickListener listener) {
        this.listener = listener;
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
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
        notifyDataSetChanged();
    }

    class AppointmentViewHolder extends RecyclerView.ViewHolder {

        private TextView dateText;
        private TextView timeText;
        private TextView notesText;

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.appointment_date_text);
            timeText = itemView.findViewById(R.id.appointment_time_text);
            notesText = itemView.findViewById(R.id.appointment_notes_text);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAppointmentClick(appointments.get(position));
                }
            });
        }

        void bind(Appointment appointment) {
            dateText.setText(appointment.getDate());
            timeText.setText(appointment.getTime());
            notesText.setText(appointment.getNotes());
        }
    }
}

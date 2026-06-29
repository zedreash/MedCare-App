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
public class PatientAppointmentAdapter extends RecyclerView.Adapter<PatientAppointmentAdapter.ViewHolder> {
    private List<Appointment> appointments = new ArrayList<>();
    private OnAppointmentClickListener listener;
    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
    }
    public PatientAppointmentAdapter(OnAppointmentClickListener listener) {
        this.listener = listener;
    }
    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_minimal, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        holder.nameText.setText(appointment.getName());
        holder.dateText.setText(appointment.getDate());
        holder.timeText.setText(appointment.getTime());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppointmentClick(appointment);
            }
        });
    }
    @Override
    public int getItemCount() {
        return appointments.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView dateText;
        TextView timeText;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.appointment_name_text);
            dateText = itemView.findViewById(R.id.appointment_date_text);
            timeText = itemView.findViewById(R.id.appointment_time_text);
        }
    }
}

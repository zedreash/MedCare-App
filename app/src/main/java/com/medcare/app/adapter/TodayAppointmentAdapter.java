package com.medcare.app.adapter;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medcare.app.R;
import com.medcare.app.data.entity.Appointment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
public class TodayAppointmentAdapter extends RecyclerView.Adapter<TodayAppointmentAdapter.ViewHolder> {
    private List<Appointment> appointments = new ArrayList<>();
    private Map<Long, String> patientNames;
    private final OnAppointmentClickListener listener;
    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
    }
    public TodayAppointmentAdapter(OnAppointmentClickListener listener) {
        this.listener = listener;
    }
    public void setAppointments(List<Appointment> appointments, Map<Long, String> patientNames) {
        this.appointments = appointments != null ? appointments : new ArrayList<>();
        this.patientNames = patientNames;
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
        String patientName = patientNames != null ? patientNames.get(appointment.getPatientId()) : "";
        if (patientName == null) patientName = "";
        String apptName = appointment.getName();
        if (apptName != null && !apptName.isEmpty()) {
            holder.nameText.setText(apptName + " \u00B7 " + patientName);
        } else {
            holder.nameText.setText(patientName);
        }
        String time = appointment.getTime() != null ? appointment.getTime() : "";
        String durationStr = appointment.getDuration() > 0
                ? appointment.getDuration() + " min"
                : "";
        int durationInt = appointment.getDuration();
        holder.dateText.setText(time);
        boolean isPast = isPastAppointment(time, durationInt);
        boolean isNow = !isPast && isCurrentAppointment(time, durationInt);
        if (isNow) {
            holder.timeText.setText(formatTimeLine(durationStr, "Now"));
            holder.timeText.setTextColor(holder.itemView.getContext().getColor(R.color.success));
        } else {
            holder.timeText.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
            String statusText = "";
            if (!isPast) {
                statusText = computeTimeUntil(time);
            }
            holder.timeText.setText(formatTimeLine(durationStr, statusText));
        }
        if (isPast) {
            holder.nameText.setAlpha(0.5f);
            holder.dateText.setAlpha(0.5f);
            holder.timeText.setAlpha(0.5f);
            holder.nameText.setPaintFlags(holder.nameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.dateText.setPaintFlags(holder.dateText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.timeText.setPaintFlags(holder.timeText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.nameText.setAlpha(1f);
            holder.dateText.setAlpha(1f);
            holder.timeText.setAlpha(1f);
            holder.nameText.setPaintFlags(holder.nameText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.dateText.setPaintFlags(holder.dateText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.timeText.setPaintFlags(holder.timeText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        holder.itemView.setOnClickListener(v -> listener.onAppointmentClick(appointment));
    }
    @Override
    public int getItemCount() {
        return appointments.size();
    }
    private boolean isPastAppointment(String time, int duration) {
        Calendar apptCal = parseToCalendar(time);
        if (apptCal == null) return true;
        Calendar endCal = (Calendar) apptCal.clone();
        endCal.add(Calendar.MINUTE, Math.max(duration, 0));
        return Calendar.getInstance().after(endCal);
    }
    private boolean isCurrentAppointment(String time, int duration) {
        Calendar apptCal = parseToCalendar(time);
        if (apptCal == null) return false;
        Calendar now = Calendar.getInstance();
        if (now.before(apptCal)) return false;
        Calendar endCal = (Calendar) apptCal.clone();
        endCal.add(Calendar.MINUTE, Math.max(duration, 0));
        return now.before(endCal);
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
    private String formatTimeLine(String duration, String timeUntil) {
        if (duration.isEmpty() && timeUntil.isEmpty()) return "";
        if (duration.isEmpty()) return timeUntil;
        if (timeUntil.isEmpty()) return duration;
        return duration + " \u00B7 " + timeUntil;
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

package com.medcare.app.adapter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.medcare.app.R;
import com.medcare.app.data.entity.Appointment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {
    private List<Appointment> appointments = new ArrayList<>();
    private Map<Long, String> patientNames = new HashMap<>();
    private OnAppointmentClickListener listener;
    private OnDeleteClickListener deleteListener;
    private int previouslyRevealed = -1;
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
        holder.cardView.setTranslationX(0);
        if (previouslyRevealed == position && holder.deleteActionWidth > 0) {
            holder.cardView.setTranslationX(-holder.deleteActionWidth);
            holder.isRevealed = true;
        } else {
            holder.cardView.setTranslationX(0);
            holder.isRevealed = false;
        }
    }
    @Override
    public int getItemCount() {
        return appointments.size();
    }
    public void setAppointments(List<Appointment> appointments, Map<Long, String> patientNames) {
        previouslyRevealed = -1;
        this.appointments = appointments;
        this.patientNames = patientNames;
        notifyDataSetChanged();
    }
    public Appointment removeItem(int position) {
        Appointment appointment = appointments.remove(position);
        notifyItemRemoved(position);
        return appointment;
    }
    public void restoreItem(Appointment appointment, int position) {
        appointments.add(position, appointment);
        notifyItemInserted(position);
    }
    public void closeRevealed() {
        if (previouslyRevealed != -1) {
            int pos = previouslyRevealed;
            previouslyRevealed = -1;
            notifyItemChanged(pos);
        }
    }
    class AppointmentViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private TextView nameText;
        private TextView patientText;
        private TextView dateText;
        private TextView timeText;
        private TextView durationText;
        private TextView notesText;
        private View deleteAction;
        private int deleteActionWidth;
        private float startX;
        private boolean isRevealed;
        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_content);
            deleteAction = itemView.findViewById(R.id.delete_action);
            nameText = itemView.findViewById(R.id.appointment_name_text);
            patientText = itemView.findViewById(R.id.appointment_patient_text);
            dateText = itemView.findViewById(R.id.appointment_date_text);
            timeText = itemView.findViewById(R.id.appointment_time_text);
            durationText = itemView.findViewById(R.id.appointment_duration_text);
            notesText = itemView.findViewById(R.id.appointment_notes_text);
            deleteActionWidth = (int) (96 * itemView.getResources().getDisplayMetrics().density);
            deleteAction.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && deleteListener != null) {
                    isRevealed = false;
                    previouslyRevealed = -1;
                    cardView.animate().translationX(0).setDuration(200).start();
                    deleteListener.onDeleteClick(appointments.get(position), position);
                }
            });
            cardView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - startX;
                        float maxReveal = deleteActionWidth;
                        float offset = isRevealed ? -maxReveal : 0;
                        float translation = Math.max(-maxReveal, Math.min(0, dx + offset));
                        cardView.setTranslationX(translation);
                        return true;
                    case MotionEvent.ACTION_UP:
                        float delta = event.getRawX() - startX;
                        float currentTranslation = cardView.getTranslationX();
                        if (isRevealed) {
                            if (delta > 20) {
                                snapCard(0, false);
                            } else {
                                snapCard(-deleteActionWidth, true);
                            }
                        } else {
                            if (currentTranslation < -deleteActionWidth * 0.4f) {
                                snapCard(-deleteActionWidth, true);
                                if (previouslyRevealed != -1 && previouslyRevealed != getAdapterPosition()) {
                                    notifyItemChanged(previouslyRevealed);
                                }
                                previouslyRevealed = getAdapterPosition();
                            } else {
                                snapCard(0, false);
                            }
                        }
                        return true;
                }
                return false;
            });
            cardView.setOnClickListener(v -> {
                if (isRevealed) {
                    snapCard(0, false);
                } else {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onAppointmentClick(appointments.get(position));
                    }
                }
            });
        }
        private void snapCard(float targetX, boolean revealed) {
            isRevealed = revealed;
            if (!revealed && previouslyRevealed == getAdapterPosition()) {
                previouslyRevealed = -1;
            }
            cardView.animate().translationX(targetX).setDuration(200).start();
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
        }
    }
}

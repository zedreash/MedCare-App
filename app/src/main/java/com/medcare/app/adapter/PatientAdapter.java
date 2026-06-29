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
import com.medcare.app.data.entity.Patient;
import java.util.ArrayList;
import java.util.List;
public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
    private List<Patient> patients = new ArrayList<>();
    private OnPatientClickListener listener;
    private OnDeleteClickListener deleteListener;
    private int previouslyRevealed = -1;
    public interface OnPatientClickListener {
        void onPatientClick(Patient patient);
    }
    public interface OnDeleteClickListener {
        void onDeleteClick(Patient patient, int position);
    }
    public PatientAdapter(OnPatientClickListener listener, OnDeleteClickListener deleteListener) {
        this.listener = listener;
        this.deleteListener = deleteListener;
    }
    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient, parent, false);
        return new PatientViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        Patient patient = patients.get(position);
        holder.bind(patient);
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
        return patients.size();
    }
    public void setPatients(List<Patient> patients) {
        previouslyRevealed = -1;
        this.patients = patients;
        notifyDataSetChanged();
    }
    public Patient removeItem(int position) {
        Patient patient = patients.remove(position);
        notifyItemRemoved(position);
        return patient;
    }
    public void restoreItem(Patient patient, int position) {
        patients.add(position, patient);
        notifyItemInserted(position);
    }
    public void closeRevealed() {
        if (previouslyRevealed != -1) {
            int pos = previouslyRevealed;
            previouslyRevealed = -1;
            notifyItemChanged(pos);
        }
    }
    class PatientViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private TextView nameText;
        private TextView phoneText;
        private TextView diagnosisText;
        private TextView addressText;
        private View deleteAction;
        private int deleteActionWidth;
        private float startX;
        private boolean isRevealed;
        PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_content);
            deleteAction = itemView.findViewById(R.id.delete_action);
            nameText = itemView.findViewById(R.id.patient_name_text);
            phoneText = itemView.findViewById(R.id.patient_phone_text);
            diagnosisText = itemView.findViewById(R.id.patient_diagnosis_text);
            addressText = itemView.findViewById(R.id.patient_address_text);
            deleteActionWidth = (int) (96 * itemView.getResources().getDisplayMetrics().density);
            deleteAction.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && deleteListener != null) {
                    isRevealed = false;
                    previouslyRevealed = -1;
                    cardView.animate().translationX(0).setDuration(200).start();
                    deleteListener.onDeleteClick(patients.get(position), position);
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
                        listener.onPatientClick(patients.get(position));
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
        void bind(Patient patient) {
            nameText.setText(patient.getFullName());
            phoneText.setText(patient.getPhone());
            diagnosisText.setText(patient.getDiagnosis());
            String address = patient.getAddress();
            addressText.setText(address != null && !address.isEmpty() ? address : null);
        }
    }
}

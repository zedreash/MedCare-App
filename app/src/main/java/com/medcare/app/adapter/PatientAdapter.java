package com.medcare.app.adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medcare.app.R;
import com.medcare.app.data.entity.Patient;
import java.util.ArrayList;
import java.util.List;
public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
    private List<Patient> patients = new ArrayList<>();
    private OnPatientClickListener listener;
    public interface OnPatientClickListener {
        void onPatientClick(Patient patient);
    }
    public PatientAdapter(OnPatientClickListener listener) {
        this.listener = listener;
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
    }
    @Override
    public int getItemCount() {
        return patients.size();
    }
    public void setPatients(List<Patient> patients) {
        this.patients = patients;
        notifyDataSetChanged();
    }
    class PatientViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView phoneText;
        private TextView diagnosisText;
        private TextView addressText;
        PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.patient_name_text);
            phoneText = itemView.findViewById(R.id.patient_phone_text);
            diagnosisText = itemView.findViewById(R.id.patient_diagnosis_text);
            addressText = itemView.findViewById(R.id.patient_address_text);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPatientClick(patients.get(position));
                }
            });
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

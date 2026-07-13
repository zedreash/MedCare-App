package com.medcare.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.medcare.app.R;
import com.medcare.app.data.entity.Patient;

import java.util.ArrayList;
import java.util.List;

public class PatientAdapter extends BaseSwipeAdapter<PatientAdapter.PatientViewHolder> {

    private List<Patient> patients = new ArrayList<>();
    private OnPatientClickListener listener;
    private OnDeleteClickListener deleteListener;

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
        holder.bindSwipeState(this, position);
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    public void setPatients(List<Patient> patients) {
        resetRevealed();
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

    class PatientViewHolder extends SwipeableViewHolder {
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
        }

        void bind(Patient patient) {
            nameText.setText(patient.getFullName());
            phoneText.setText(patient.getPhone());
            diagnosisText.setText(patient.getDiagnosis());
            String address = patient.getAddress();
            addressText.setText(address != null && !address.isEmpty() ? address : null);
        }

        @Override
        protected void onItemClick(int position) {
            if (listener != null) {
                listener.onPatientClick(patients.get(position));
            }
        }

        @Override
        protected void onDeleteActionClick(int position) {
            deleteListener.onDeleteClick(patients.get(position), position);
        }
    }
}

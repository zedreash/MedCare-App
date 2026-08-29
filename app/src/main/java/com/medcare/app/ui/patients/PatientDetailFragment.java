package com.medcare.app.ui.patients;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.medcare.app.R;
import com.medcare.app.adapter.PatientAppointmentAdapter;
import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.entity.PatientAllergy;
import com.medcare.app.data.entity.PatientAttachment;
import com.medcare.app.data.entity.PatientHistory;
import com.medcare.app.data.entity.PatientMedication;
import com.medcare.app.data.repository.AppointmentRepository;
import com.medcare.app.data.repository.PatientExtrasRepository;
import com.medcare.app.data.repository.PatientRepository;
import com.medcare.app.utils.DateUtils;
import com.medcare.app.utils.PdfExporter;
import com.medcare.app.utils.PreferencesManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class PatientDetailFragment extends Fragment {
    private static final String ARG_PATIENT_ID = "patientId";
    private TextView nameText;
    private TextView phoneText;
    private TextView diagnosisText;
    private TextView addressText;
    private TextView notesText;
    private RecyclerView appointmentsRecycler;
    private TextView noAppointmentsText;
    private TextView vitalsText;
    private LinearLayout medicationsContainer;
    private LinearLayout allergiesContainer;
    private LinearLayout historyContainer;
    private LinearLayout attachmentsContainer;
    private PatientRepository patientRepository;
    private AppointmentRepository appointmentRepository;
    private PatientExtrasRepository extrasRepository;
    private PreferencesManager preferencesManager;
    private PatientAppointmentAdapter adapter;
    private ActivityResultLauncher<String[]> pickAttachmentLauncher;
    private File attachmentsDir;
    private List<Appointment> patientAppointments = new java.util.ArrayList<>();
    private List<PatientMedication> medsList = new ArrayList<>();
    private List<PatientAllergy> allergiesList = new ArrayList<>();
    private List<PatientHistory> historyList = new ArrayList<>();
    private List<PatientAttachment> attachmentsList = new ArrayList<>();
    private long patientId;
    private Patient patient;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientId = getArguments().getInt(ARG_PATIENT_ID, -1);
        }
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_detail, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        patientRepository = new PatientRepository(requireContext());
        appointmentRepository = new AppointmentRepository(requireContext());
        extrasRepository = new PatientExtrasRepository(requireContext());
        preferencesManager = new PreferencesManager(requireContext());
        nameText = view.findViewById(R.id.patient_name_text);
        phoneText = view.findViewById(R.id.patient_phone_text);
        diagnosisText = view.findViewById(R.id.patient_diagnosis_text);
        addressText = view.findViewById(R.id.patient_address_text);
        notesText = view.findViewById(R.id.patient_notes_text);
        vitalsText = view.findViewById(R.id.vitals_text);
        medicationsContainer = view.findViewById(R.id.medications_container);
        allergiesContainer = view.findViewById(R.id.allergies_container);
        historyContainer = view.findViewById(R.id.history_container);
        attachmentsContainer = view.findViewById(R.id.attachments_container);
        attachmentsDir = new File(requireContext().getFilesDir(), "attachments");
        if (!attachmentsDir.exists()) attachmentsDir.mkdirs();
        appointmentsRecycler = view.findViewById(R.id.appointments_recycler_view);
        noAppointmentsText = view.findViewById(R.id.no_appointments_text);
        appointmentsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        view.findViewById(R.id.back_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
        view.findViewById(R.id.edit_button).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("patientId", (int) patientId);
            Navigation.findNavController(view)
                    .navigate(R.id.action_patientDetail_to_patientForm, args);
        });
        view.findViewById(R.id.delete_button).setOnClickListener(v -> confirmDelete());
        view.findViewById(R.id.add_medication_button).setOnClickListener(v -> addMedication());
        view.findViewById(R.id.add_allergy_button).setOnClickListener(v -> addAllergy());
        view.findViewById(R.id.add_history_button).setOnClickListener(v -> addHistoryEntry());
        pickAttachmentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri != null) saveAttachment(uri);
                });
        view.findViewById(R.id.add_attachment_button).setOnClickListener(v ->
                pickAttachmentLauncher.launch(new String[]{"*/*"}));
        view.findViewById(R.id.share_pdf_button).setOnClickListener(v -> sharePdf());
        loadPatient();
    }

    private void confirmDelete() {
        if (patient == null) return;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_patient_message)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        patientRepository.delete(patient, new PatientRepository.Callback<Void>() {
                            @Override
                            public void onResult(Void result) {
                                deleteAttachmentFiles();
                                if (isAdded()) {
                                    Snackbar.make(requireView(), R.string.success_deleted, Snackbar.LENGTH_SHORT).show();
                                    Navigation.findNavController(requireView()).navigateUp();
                                }
                            }
                        }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteAttachmentFiles() {
        for (PatientAttachment att : attachmentsList) {
            if (att.getFilePath() != null) {
                File f = new File(att.getFilePath());
                if (f.exists()) f.delete();
            }
        }
    }

    private void sharePdf() {
        if (patient == null) return;
        AppDatabase.getExecutor().execute(() -> {
            try {
                String safeName = patient.getFullName().replaceAll("[^\\p{L}\\p{N}_-]", "_");
                if (safeName.isEmpty()) safeName = "patient";
                String fileName = "MedCare_Patient_" + safeName + ".pdf";

                List<String[]> info = new ArrayList<>();
                info.add(new String[]{getString(R.string.phone), nz(patient.getPhone())});
                info.add(new String[]{getString(R.string.diagnosis), nz(patient.getDiagnosis())});
                info.add(new String[]{getString(R.string.address), nz(patient.getAddress())});
                info.add(new String[]{getString(R.string.blood_type), nz(patient.getBloodType())});
                info.add(new String[]{getString(R.string.height),
                        patient.getHeightCm() != null ? patient.getHeightCm() + " cm" : ""});
                info.add(new String[]{getString(R.string.weight),
                        patient.getWeightKg() != null ? patient.getWeightKg() + " kg" : ""});
                info.add(new String[]{getString(R.string.notes), nz(patient.getNotes())});

                List<PdfExporter.Table> tables = new ArrayList<>();

                List<String[]> apptRows = new ArrayList<>();
                for (Appointment a : patientAppointments) {
                    apptRows.add(new String[]{nz(a.getDate()), nz(a.getTime()), nz(a.getName())});
                }
                if (!apptRows.isEmpty()) {
                    tables.add(new PdfExporter.Table(getString(R.string.appointments),
                            new String[]{getString(R.string.date), getString(R.string.time),
                                    getString(R.string.appointment_name)},
                            apptRows.toArray(new String[0][])));
                }

                List<String[]> medRows = new ArrayList<>();
                for (PatientMedication m : medsList) {
                    medRows.add(new String[]{nz(m.getName()), nz(m.getDosage()),
                            m.isActive() ? getString(R.string.active) : getString(R.string.inactive)});
                }
                if (!medRows.isEmpty()) {
                    tables.add(new PdfExporter.Table(getString(R.string.medications),
                            new String[]{getString(R.string.medication_name), getString(R.string.dosage),
                                    getString(R.string.status)},
                            medRows.toArray(new String[0][])));
                }

                List<String[]> allergyRows = new ArrayList<>();
                for (PatientAllergy a : allergiesList) {
                    allergyRows.add(new String[]{nz(a.getName()), nz(a.getNote())});
                }
                if (!allergyRows.isEmpty()) {
                    tables.add(new PdfExporter.Table(getString(R.string.allergies),
                            new String[]{getString(R.string.allergy_name), getString(R.string.note)},
                            allergyRows.toArray(new String[0][])));
                }

                List<String[]> histRows = new ArrayList<>();
                for (PatientHistory h : historyList) {
                    histRows.add(new String[]{nz(h.getTitle()), nz(h.getDetails())});
                }
                if (!histRows.isEmpty()) {
                    tables.add(new PdfExporter.Table(getString(R.string.medical_history),
                            new String[]{getString(R.string.history_title), getString(R.string.history_details)},
                            histRows.toArray(new String[0][])));
                }

                List<String[]> attRows = new ArrayList<>();
                for (PatientAttachment att : attachmentsList) {
                    attRows.add(new String[]{nz(att.getName()), nz(att.getNote())});
                }
                if (!attRows.isEmpty()) {
                    tables.add(new PdfExporter.Table(getString(R.string.attachments),
                            new String[]{getString(R.string.file_name), getString(R.string.note)},
                            attRows.toArray(new String[0][])));
                }

                String[][] infoArr = info.toArray(new String[0][]);
                File file = PdfExporter.writePatientSummaryPdf(requireContext(), fileName,
                        getString(R.string.patient_summary), patient.getFullName(),
                        getString(R.string.details), infoArr, tables);
                AppDatabase.runOnMainThread(() -> {
                    if (isAdded()) {
                        PdfExporter.share(requireContext(), file);
                    }
                });
            } catch (Exception ignored) {
            }
        });
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
    private void loadPatient() {
        if (patientId == -1) return;
        patientRepository.getPatientById(patientId, preferencesManager.getLoggedInUserId(), new PatientRepository.Callback<Patient>() {
            @Override
            public void onResult(Patient result) {
                if (!isAdded()) return;
                patient = result;
                if (patient == null) {
                    Navigation.findNavController(requireView()).navigateUp();
                    return;
                }
                nameText.setText(patient.getFullName());
                phoneText.setText(patient.getPhone());
                String diagnosis = patient.getDiagnosis();
                if (diagnosis != null && !diagnosis.isEmpty()) {
                    diagnosisText.setText(diagnosis);
                    diagnosisText.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                            requireContext(), com.google.android.material.R.attr.colorPrimary,
                            ContextCompat.getColor(requireContext(), R.color.primary)));
                } else {
                    diagnosisText.setText(R.string.no_diagnosis_hint);
                    diagnosisText.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                            requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant,
                            ContextCompat.getColor(requireContext(), R.color.text_secondary)));
                }
                String address = patient.getAddress();
                addressText.setText(address != null && !address.isEmpty() ? address : null);
                String notes = patient.getNotes();
                notesText.setText(notes != null && !notes.isEmpty() ? notes : null);
                updateVitals();
                loadExtras();
                appointmentRepository.getAppointmentsByPatientId(patientId, preferencesManager.getLoggedInUserId(), new AppointmentRepository.Callback<List<Appointment>>() {
                    @Override
                    public void onResult(List<Appointment> result) {
                        patientAppointments = result;
                        List<Appointment> appointments = result;
                        adapter = new PatientAppointmentAdapter(appointment -> {
                            Bundle args = new Bundle();
                            args.putInt("appointmentId", (int) appointment.getId());
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.action_patientDetail_to_appointmentDetail, args);
                        });
                        appointmentsRecycler.setAdapter(adapter);
                        adapter.setAppointments(appointments);

                        if (appointments.isEmpty()) {
                            appointmentsRecycler.setVisibility(View.GONE);
                            noAppointmentsText.setVisibility(View.VISIBLE);
                        } else {
                            appointmentsRecycler.setVisibility(View.VISIBLE);
                            noAppointmentsText.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }

    private void updateVitals() {
        if (vitalsText == null || patient == null) return;
        StringBuilder sb = new StringBuilder();
        if (patient.getBloodType() != null && !patient.getBloodType().isEmpty()) {
            sb.append(patient.getBloodType());
        }
        if (patient.getHeightCm() != null) {
            if (sb.length() > 0) sb.append(" \u00B7 ");
            sb.append(patient.getHeightCm()).append(" cm");
        }
        if (patient.getWeightKg() != null) {
            if (sb.length() > 0) sb.append(" \u00B7 ");
            sb.append(patient.getWeightKg()).append(" kg");
        }
        if (sb.length() > 0) {
            vitalsText.setText(sb.toString());
            vitalsText.setVisibility(View.VISIBLE);
        } else {
            vitalsText.setVisibility(View.GONE);
        }
    }

    private void loadExtras() {
        extrasRepository.getMedications(patientId, meds -> {
            medsList = meds;
            if (isAdded()) renderMedications(meds);
        });
        extrasRepository.getAllergies(patientId, allergies -> {
            allergiesList = allergies;
            if (isAdded()) renderAllergies(allergies);
        });
        extrasRepository.getHistory(patientId, history -> {
            historyList = history;
            if (isAdded()) renderHistory(history);
        });
        extrasRepository.getAttachments(patientId, attachments -> {
            attachmentsList = attachments;
            if (isAdded()) renderAttachments(attachments);
        });
    }

    private void renderMedications(List<PatientMedication> meds) {
        medicationsContainer.removeAllViews();
        if (meds.isEmpty()) return;
        for (PatientMedication m : meds) {
            String sub = (m.getDosage() != null && !m.getDosage().isEmpty() ? m.getDosage() : "")
                    + (m.isActive() ? "" : " \u00B7 " + getString(R.string.inactive));
            addRow(medicationsContainer, m.getName(), sub, () -> editMedication(m), () -> {
                extrasRepository.deleteMedication(m, r -> loadExtras());
            }, false);
        }
    }

    private void renderAllergies(List<PatientAllergy> allergies) {
        allergiesContainer.removeAllViews();
        if (allergies.isEmpty()) return;
        for (PatientAllergy a : allergies) {
            addRow(allergiesContainer, a.getName(), a.getNote(), () -> editAllergy(a), () -> {
                extrasRepository.deleteAllergy(a, r -> loadExtras());
            }, false);
        }
    }

    private void renderHistory(List<PatientHistory> history) {
        historyContainer.removeAllViews();
        if (history.isEmpty()) return;
        for (PatientHistory h : history) {
            addRow(historyContainer, h.getTitle(), h.getDetails(), () -> editHistory(h), () -> {
                extrasRepository.deleteHistory(h, r -> loadExtras());
            }, false);
        }
    }

    private void renderAttachments(List<PatientAttachment> attachments) {
        attachmentsContainer.removeAllViews();
        if (attachments.isEmpty()) return;
        for (PatientAttachment a : attachments) {
            addRow(attachmentsContainer, a.getName(), a.getNote(), () -> openAttachment(a), () -> {
                File f = a.getFilePath() != null ? new File(a.getFilePath()) : null;
                extrasRepository.deleteAttachment(a, r -> {
                    if (f != null) f.delete();
                    loadExtras();
                });
            }, true);
        }
    }

    private void openAttachment(PatientAttachment a) {
        File f = a.getFilePath() != null ? new File(a.getFilePath()) : null;
        if (f == null || !f.exists()) {
            Snackbar.make(requireView(), R.string.error_generic, Snackbar.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = androidx.core.content.FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", f);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeTypeFor(a));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Snackbar.make(requireView(), R.string.error_generic, Snackbar.LENGTH_SHORT).show();
        }
    }

    private String mimeTypeFor(PatientAttachment a) {
        if (a.getType() != null && !a.getType().isEmpty()) {
            return a.getType();
        }
        String name = a.getName();
        if (name == null) return "application/octet-stream";
        String lower = name.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    private void addRow(LinearLayout container, String title, String subtitle,
                        Runnable onTap, Runnable onDelete, boolean openAction) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(4), 0, dpToPx(4));
        row.setLayoutParams(lp);

        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView titleText = new TextView(requireContext());
        titleText.setText(title == null ? "" : title);
        titleText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        texts.addView(titleText);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView subText = new TextView(requireContext());
            subText.setText(subtitle);
            subText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
            subText.setTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(),
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)));
            texts.addView(subText);
        }
        if (onTap != null) {
            texts.setClickable(true);
            texts.setFocusable(true);
            texts.setOnClickListener(v -> onTap.run());
            titleText.setTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(),
                    com.google.android.material.R.attr.colorPrimary,
                    ContextCompat.getColor(requireContext(), R.color.primary)));
        }
        row.addView(texts);

        if (onTap != null) {
            ImageView actionIcon = new ImageView(requireContext());
            actionIcon.setImageResource(openAction ? R.drawable.ic_chevron_right : R.drawable.ic_edit);
            actionIcon.setColorFilter(com.google.android.material.color.MaterialColors.getColor(requireContext(),
                    com.google.android.material.R.attr.colorPrimary,
                    ContextCompat.getColor(requireContext(), R.color.primary)));
            actionIcon.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            actionIcon.setOnClickListener(v -> onTap.run());
            row.addView(actionIcon);
        }

        TextView deleteText = new TextView(requireContext());
        deleteText.setText(getString(R.string.delete));
        deleteText.setTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(),
                com.google.android.material.R.attr.colorError,
                ContextCompat.getColor(requireContext(), R.color.error)));
        deleteText.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        deleteText.setOnClickListener(v -> onDelete.run());
        row.addView(deleteText);

        container.addView(row);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void addMedication() {
        LinearLayout layout = buildDialogLayout();
        EditText name = new EditText(requireContext());
        name.setHint(R.string.medication_name);
        EditText dosage = new EditText(requireContext());
        dosage.setHint(R.string.dosage);
        com.google.android.material.switchmaterial.SwitchMaterial activeSwitch =
                new com.google.android.material.switchmaterial.SwitchMaterial(requireContext());
        activeSwitch.setText(R.string.active);
        activeSwitch.setChecked(true);
        layout.addView(name);
        layout.addView(dosage);
        layout.addView(activeSwitch);
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_medication)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) return;
                    extrasRepository.insertMedication(new PatientMedication(
                            patientId, n, dosage.getText().toString().trim(),
                            activeSwitch.isChecked(), DateUtils.getCurrentTimestamp()), r -> loadExtras());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void addAllergy() {
        LinearLayout layout = buildDialogLayout();
        EditText name = new EditText(requireContext());
        name.setHint(R.string.allergy_name);
        EditText note = new EditText(requireContext());
        note.setHint(R.string.note);
        layout.addView(name);
        layout.addView(note);
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_allergy)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) return;
                    extrasRepository.insertAllergy(new PatientAllergy(
                            patientId, n, note.getText().toString().trim(),
                            DateUtils.getCurrentTimestamp()), r -> loadExtras());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void editMedication(PatientMedication m) {
        LinearLayout layout = buildDialogLayout();
        EditText name = new EditText(requireContext());
        name.setHint(R.string.medication_name);
        name.setText(m.getName());
        EditText dosage = new EditText(requireContext());
        dosage.setHint(R.string.dosage);
        dosage.setText(m.getDosage());
        com.google.android.material.switchmaterial.SwitchMaterial activeSwitch =
                new com.google.android.material.switchmaterial.SwitchMaterial(requireContext());
        activeSwitch.setText(R.string.active);
        activeSwitch.setChecked(m.isActive());
        layout.addView(name);
        layout.addView(dosage);
        layout.addView(activeSwitch);
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_medication)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) return;
                    m.setName(n);
                    m.setDosage(dosage.getText().toString().trim());
                    m.setActive(activeSwitch.isChecked());
                    extrasRepository.updateMedication(m, r -> loadExtras());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void editAllergy(PatientAllergy a) {
        LinearLayout layout = buildDialogLayout();
        EditText name = new EditText(requireContext());
        name.setHint(R.string.allergy_name);
        name.setText(a.getName());
        EditText note = new EditText(requireContext());
        note.setHint(R.string.note);
        note.setText(a.getNote());
        layout.addView(name);
        layout.addView(note);
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_allergy)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) return;
                    a.setName(n);
                    a.setNote(note.getText().toString().trim());
                    extrasRepository.updateAllergy(a, r -> loadExtras());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void editHistory(PatientHistory h) {
        LinearLayout layout = buildDialogLayout();
        EditText title = new EditText(requireContext());
        title.setHint(R.string.history_title);
        title.setText(h.getTitle());
        EditText details = new EditText(requireContext());
        details.setHint(R.string.history_details);
        details.setText(h.getDetails());
        layout.addView(title);
        layout.addView(details);
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_history_entry)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String t = title.getText().toString().trim();
                    if (t.isEmpty()) return;
                    h.setTitle(t);
                    h.setDetails(details.getText().toString().trim());
                    extrasRepository.updateHistory(h, r -> loadExtras());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void addHistoryEntry() {
        LinearLayout layout = buildDialogLayout();
        EditText title = new EditText(requireContext());
        title.setHint(R.string.history_title);
        EditText details = new EditText(requireContext());
        details.setHint(R.string.history_details);
        layout.addView(title);
        layout.addView(details);
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_history_entry)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String t = title.getText().toString().trim();
                    if (t.isEmpty()) return;
                    String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                    extrasRepository.insertHistory(new PatientHistory(
                            patientId, t, details.getText().toString().trim(), today,
                            DateUtils.getCurrentTimestamp()), r -> loadExtras());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveAttachment(android.net.Uri uri) {
        try {
            String original = uri.getLastPathSegment();
            String safe = (original != null && !original.isEmpty())
                    ? original.replaceAll("[^a-zA-Z0-9._-]", "_") : "file_" + System.currentTimeMillis();
            if (safe.length() > 80) safe = safe.substring(safe.length() - 80);
            File dest = new File(attachmentsDir, safe);
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
            }
            extrasRepository.insertAttachment(new PatientAttachment(
                    patientId, dest.getAbsolutePath(), original == null ? safe : original,
                    "file", "", DateUtils.getCurrentTimestamp()), r -> loadExtras());
        } catch (Exception e) {
            Snackbar.make(requireView(), R.string.error_generic, Snackbar.LENGTH_LONG).show();
        }
    }

    private LinearLayout buildDialogLayout() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = getResources().getDimensionPixelSize(R.dimen.margin_large);
        layout.setPadding(pad, pad, pad, pad);
        return layout;
    }
}

package com.medcare.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.db.AppointmentDao;
import com.medcare.app.data.db.PatientDao;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.LogEntry;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.data.entity.PatientAllergy;
import com.medcare.app.data.entity.PatientAttachment;
import com.medcare.app.data.entity.PatientHistory;
import com.medcare.app.data.entity.PatientMedication;
import com.medcare.app.utils.AppointmentStatus;
import com.medcare.app.utils.AuditLogger;
import com.medcare.app.utils.PreferencesManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MockDataSeeder {

    public interface Callback {
        void onDone(boolean success);
    }

    private static final Map<String, List<String>> NAMES = new HashMap<>();
    private static final Map<String, List<String>> DIAGNOSES = new HashMap<>();
    private static final Map<String, List<String>> NOTES = new HashMap<>();
    private static final Map<String, List<String>> ADDRESSES = new HashMap<>();
    private static final Map<String, List<String>> PURPOSES = new HashMap<>();
    private static final Map<String, List<String>> MEDICATIONS = new HashMap<>();
    private static final Map<String, List<String>> DOSAGES = new HashMap<>();
    private static final Map<String, List<String>> ALLERGIES = new HashMap<>();
    private static final Map<String, List<String>> HISTORY = new HashMap<>();
    private static final String[] BLOOD_TYPES = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    static {
        NAMES.put("en", Arrays.asList(
                "Yosef Cohen", "Sarah Levi", "Mohammed Abu Hassan", "Rachel Mizrahi",
                "David Barak", "Aisha Khalil", "Daniel Peretz", "Noor Saleh",
                "Miriam Goldberg", "Omar Nasser", "Hannah Rosen", "Layla Mansour",
                "Itzik Shalom", "Yasmin Haddad"));
        NAMES.put("ar", Arrays.asList(
                "محمد أحمد", "سارة خليل", "أحمد حسن", "نور سعيد",
                "ليلى منصور", "ياسمين حداد", "خالد ناصر", "فاطمة علي",
                "عمر صالح", "مريم خضر", "حسن عباس", "آية يوسف",
                "باسم قاسم", "رنا حمدان"));
        NAMES.put("he", Arrays.asList(
                "יוסי כהן", "שרה לוי", "מוחמד אבו חסן", "רחל מזרחי",
                "דוד ברק", "אחמד חליל", "נועה פרידמן", "עומר ניסים",
                "מיכל גולברג", "יוסף נאסר", "חנה רוזן", "ליאור שהם",
                "מוסא עבדאללה", "תמר אוחיון"));

        DIAGNOSES.put("en", Arrays.asList(
                "Hypertension", "Type 2 Diabetes", "Migraine", "Asthma",
                "Seasonal allergy", "Lower back pain", "Ankle sprain",
                "Common cold", "High cholesterol", "Anxiety"));
        DIAGNOSES.put("ar", Arrays.asList(
                "ارتفاع ضغط الدم", "السكري من النوع الثاني", "صداع نصفي", "ربو",
                "حساسية موسمية", "ألم أسفل الظهر", "التواء الكاحل",
                "نزلة برد", "ارتفاع الكوليسترول", "قلق"));
        DIAGNOSES.put("he", Arrays.asList(
                "יתר לחץ דם", "סוכרת סוג 2", "מיגרנה", "אסתמה",
                "אלרגיה עונתית", "כאבי גב תחתון", "נקע בקרסול",
                "הצטננות", "כולסטרול גבוה", "חרדה"));

        NOTES.put("en", Arrays.asList(
                "Patient reports mild headache in the morning",
                "Bring previous blood test results",
                "Recheck in two weeks",
                "Allergic to penicillin",
                "Monitor blood pressure daily",
                "Started new medication last month"));
        NOTES.put("ar", Arrays.asList(
                "يشتكي المريض من صداع خفيف في الصباح",
                "يرجى إحضار نتائج فحص الدم السابقة",
                "إعادة الفحص بعد أسبوعين",
                "حساسية من البنسلين",
                "مراقبة ضغط الدم يومياً",
                "بدأ دواء جديد الشهر الماضي"));
        NOTES.put("he", Arrays.asList(
                "המטופל מדווח על כאב ראש קל בבוקר",
                "יש להביא תוצאות בדיקות דם קודמות",
                "בדיקה חוזרת בעוד שבועיים",
                "רגיש לפניצילין",
                "לעקוב אחר לחץ דם יומי",
                "התחיל תרופה חדשה בחודש שעבר"));

        ADDRESSES.put("en", Arrays.asList(
                "Jaffa St 12, Jerusalem", "Ben Yehuda St 45, Tel Aviv",
                "HaNeviim St 8, Jerusalem", "Dizengoff St 120, Tel Aviv",
                "Herzl St 33, Haifa", "King George St 22, Jerusalem",
                "Rothschild Blvd 5, Tel Aviv", "Allenby St 77, Tel Aviv"));
        ADDRESSES.put("ar", Arrays.asList(
                "شارع يافا 12، القدس", "شارع بن يهودا 45، تل أبيب",
                "شارع الأنبياء 8، القدس", "شارع ديزنغوف 120، تل أبيب",
                "شارع هرتسل 33، حيفا", "شارع الملك جورج 22، القدس",
                "شارع روتشيلد 5، تل أبيب", "شارع اللنبي 77، تل أبيب"));
        ADDRESSES.put("he", Arrays.asList(
                "רחוב יפו 12, ירושלים", "רחוב בן יהודה 45, תל אביב",
                "רחוב הנביאים 8, ירושלים", "רחוב דיזנגוף 120, תל אביב",
                "רחוב הרצל 33, חיפה", "רחוב המלך ג'ורג' 22, ירושלים",
                "שדרות רוטשילד 5, תל אביב", "רחוב אלנבי 77, תל אביב"));

        PURPOSES.put("en", Arrays.asList(
                "Follow-up", "Routine check-up", "Blood test", "Consultation",
                "Vaccination", "Physiotherapy", "Referral", "Lab results"));
        PURPOSES.put("ar", Arrays.asList(
                "متابعة", "فحص دوري", "فحص دم", "استشارة",
                "تطعيم", "علاج طبيعي", "تحويل", "نتائج مخبرية"));
        PURPOSES.put("he", Arrays.asList(
                "מעקב", "בדיקה שגרתית", "בדיקת דם", "ייעוץ",
                "חיסון", "פיזיותרפיה", "הפניה", "תוצאות מעבדה"));

        MEDICATIONS.put("en", Arrays.asList(
                "Metformin", "Lisinopril", "Amoxicillin", "Ventolin", "Paracetamol", "Omeprazole"));
        MEDICATIONS.put("ar", Arrays.asList(
                "ميتفورمين", "ليزينوبريل", "أموكسيسيلين", "فينتولين", "باراسيتامول", "أوميبرازول"));
        MEDICATIONS.put("he", Arrays.asList(
                "מטפורמין", "ליסינופריל", "אמוקסיצילין", "ונטולין", "אקמול", "אומפרזול"));

        DOSAGES.put("en", Arrays.asList(
                "500 mg daily", "10 mg once a day", "2 puffs as needed", "250 mg twice daily"));
        DOSAGES.put("ar", Arrays.asList(
                "500 ملغ يوميا", "10 ملغ مرة يوميا", "جرعتان عند الحاجة", "250 ملغ مرتين يوميا"));
        DOSAGES.put("he", Arrays.asList(
                "500 מ\"ג ליום", "10 מ\"ג פעם ביום", "2 שאיפות לפי הצורך", "250 מ\"ג פעמיים ביום"));

        ALLERGIES.put("en", Arrays.asList(
                "Penicillin", "Pollen", "Dust mites", "Nuts", "Lactose"));
        ALLERGIES.put("ar", Arrays.asList(
                "بنسلين", "حبوب اللقاح", "غبار", "مكسرات", "لاكتوز"));
        ALLERGIES.put("he", Arrays.asList(
                "פניצילין", "אבקנים", "קרדית אבק", "אגוזים", "לקטוז"));

        HISTORY.put("en", Arrays.asList(
                "Tonsillectomy (2015)", "Appendectomy (2018)", "Chronic sinusitis",
                "Broken wrist (2021)", "COVID-19 (2022)"));
        HISTORY.put("ar", Arrays.asList(
                "استئصال اللوزتين (2015)", "استئصال الزائدة (2018)", "التهاب الجيوب الأنفية",
                "كسر في الرسغ (2021)", "كوفيد-19 (2022)"));
        HISTORY.put("he", Arrays.asList(
                "כריתת שקדים (2015)", "כריתת תוספתן (2018)", "סינוסיטיס כרונית",
                "שבר בפרק כף היד (2021)", "קורונה (2022)"));
    }

    private static final String[] TIME_SLOTS = {
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00",
            "11:30", "12:00", "12:30", "13:00", "13:30", "14:00", "14:30",
            "15:00", "15:30", "16:00", "16:30", "17:00"
    };

    public static String resolveLanguage(PreferencesManager prefs) {
        String lang = prefs.getLanguage();
        if ("system".equals(lang)) {
            String sys = Locale.getDefault().getLanguage();
            if (sys.equals("ar")) return "ar";
            if (sys.equals("en")) return "en";
            return "he";
        }
        if (lang.equals("ar")) return "ar";
        if (lang.equals("en")) return "en";
        return "he";
    }

    public static void seed(Context context, long ownerId, String lang, Callback callback) {
        AppDatabase.getExecutor().execute(() -> {
            boolean success = false;
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                PatientDao patientDao = db.patientDao();
                AppointmentDao appointmentDao = db.appointmentDao();
                PreferencesManager prefs = new PreferencesManager(context);
                Random random = new Random();
                long now = System.currentTimeMillis();

                List<String> names = NAMES.get(lang);
                List<String> diagnoses = DIAGNOSES.get(lang);
                List<String> notesPool = NOTES.get(lang);
                List<String> addresses = ADDRESSES.get(lang);
                List<String> purposes = PURPOSES.get(lang);

                List<Long> patientIds = new ArrayList<>();
                Set<String> savedPatientIds = new HashSet<>();
                for (int i = 0; i < 14; i++) {
                    String fullName = names.get(random.nextInt(names.size()));
                    String phone = randomPhone(random);
                    String diagnosis = random.nextInt(10) < 8
                            ? diagnoses.get(random.nextInt(diagnoses.size())) : "";
                    String notes = random.nextInt(10) < 4
                            ? notesPool.get(random.nextInt(notesPool.size())) : "";
                    boolean hasAddress = random.nextInt(10) < 6;
                    String address = hasAddress
                            ? addresses.get(random.nextInt(addresses.size())) : "";
                    double lat = hasAddress ? 31.7600 + random.nextDouble() * 0.05 : 0.0;
                    double lng = hasAddress ? 35.1900 + random.nextDouble() * 0.06 : 0.0;
                    long createdAt = now - random.nextInt(90) * 86400000L;

                    Patient patient = new Patient(fullName, phone, diagnosis, notes, address, createdAt);
                    patient.setLatitude(lat);
                    patient.setLongitude(lng);
                    patient.setOwnerId(ownerId);
                    patient.setBloodType(random.nextInt(10) < 7
                            ? BLOOD_TYPES[random.nextInt(BLOOD_TYPES.length)] : null);
                    patient.setHeightCm(random.nextInt(10) < 8
                            ? Integer.valueOf(150 + random.nextInt(45)) : null);
                    patient.setWeightKg(random.nextInt(10) < 8
                            ? Double.valueOf(50 + random.nextInt(70)) : null);
                    long id = patientDao.insert(patient);
                    AuditLogger.log(context, ownerId, LogEntry.ACTION_CREATE, "patient", id, fullName);
                    patientIds.add(id);
                    savedPatientIds.add(String.valueOf(id));
                }

                List<String> meds = MEDICATIONS.get(lang);
                List<String> dosages = DOSAGES.get(lang);
                List<String> allergiesPool = ALLERGIES.get(lang);
                List<String> historyPool = HISTORY.get(lang);
                for (Long pid : patientIds) {
                    if (random.nextInt(10) < 5) {
                        int medCount = 1 + random.nextInt(3);
                        for (int k = 0; k < medCount; k++) {
                            String medName = meds.get(random.nextInt(meds.size()));
                            long mid = db.patientMedicationDao().insert(new PatientMedication(
                                    pid, medName,
                                    dosages.get(random.nextInt(dosages.size())),
                                    random.nextBoolean(), now));
                            AuditLogger.log(context, ownerId, LogEntry.ACTION_CREATE, "medication", mid, medName);
                        }
                    }
                    if (random.nextInt(10) < 3) {
                        int allCount = 1 + random.nextInt(2);
                        for (int k = 0; k < allCount; k++) {
                            String allergyName = allergiesPool.get(random.nextInt(allergiesPool.size()));
                            long aid = db.patientAllergyDao().insert(new PatientAllergy(
                                    pid, allergyName,
                                    random.nextInt(10) < 3 ? notesPool.get(random.nextInt(notesPool.size())) : "",
                                    now));
                            AuditLogger.log(context, ownerId, LogEntry.ACTION_CREATE, "allergy", aid, allergyName);
                        }
                    }
                    if (random.nextInt(10) < 4) {
                        int histCount = 1 + random.nextInt(2);
                        for (int k = 0; k < histCount; k++) {
                            String histTitle = historyPool.get(random.nextInt(historyPool.size()));
                            long hid = db.patientHistoryDao().insert(new PatientHistory(
                                    pid, histTitle,
                                    random.nextInt(10) < 3 ? notesPool.get(random.nextInt(notesPool.size())) : "",
                                    "", now));
                            AuditLogger.log(context, ownerId, LogEntry.ACTION_CREATE, "history", hid, histTitle);
                        }
                    }
                }
                File attDir = new File(context.getFilesDir(), "attachments");
                if (!attDir.exists()) attDir.mkdirs();
                String[][] attachmentSamples = {
                        {"blood_test_results.txt", "text/plain"},
                        {"xray_report.txt", "text/plain"},
                        {"prescription.pdf", "application/pdf"},
                        {"referral_letter.pdf", "application/pdf"},
                        {"vaccination_card.txt", "text/plain"},
                        {"lab_referral.txt", "text/plain"}
                };
                for (int i = 0; i < attachmentSamples.length; i++) {
                    long pid = patientIds.get(random.nextInt(patientIds.size()));
                    File f = new File(attDir, "sample-" + (now % 1000000) + "-" + i + ".txt");
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(f)) {
                        fos.write(("Sample attachment: " + attachmentSamples[i][0]).getBytes("UTF-8"));
                    }
                    long attId = db.patientAttachmentDao().insert(new PatientAttachment(
                            pid, f.getAbsolutePath(), attachmentSamples[i][0], attachmentSamples[i][1],
                            notesPool.get(random.nextInt(notesPool.size())), now));
                    AuditLogger.log(context, ownerId, LogEntry.ACTION_CREATE, "attachment", attId, attachmentSamples[i][0]);
                }

                int pastTarget = patientIds.size() * 2;
                int futureTarget = patientIds.size() * 2 + random.nextInt(6) - 3;
                if (futureTarget < patientIds.size()) futureTarget = patientIds.size();
                SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Set<String> savedAppointmentIds = new HashSet<>();
                List<Appointment> createdAppointments = new ArrayList<>();
                int pastPlaced = 0;
                int futurePlaced = 0;
                int cursor = random.nextInt(patientIds.size());
                Calendar nowCal = Calendar.getInstance();
                int nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE);

                for (int offset = -1; offset >= -60 && pastPlaced < pastTarget; offset--) {
                    Calendar d = Calendar.getInstance();
                    d.add(Calendar.DAY_OF_MONTH, offset);
                    double r = random.nextDouble();
                    int dayCount = r < 0.35 ? 0 : r < 0.72 ? 1 : r < 0.90 ? 2 : r < 0.97 ? 3 : 4;
                    List<String> times = pickTimes(dayCount, random);
                    int count = times.size();
                    if (pastPlaced + count > pastTarget) {
                        count = Math.max(0, pastTarget - pastPlaced);
                        if (times.size() > count) {
                            times = new ArrayList<>(times.subList(0, count));
                        }
                    }

                    String dateStr = fmt.format(d.getTime());
                    for (int k = 0; k < count; k++) {
                        long pid = patientIds.get(cursor % patientIds.size());
                        cursor++;
                        String purpose = purposes.get(random.nextInt(purposes.size()));
                        String notes = random.nextInt(10) < 4
                                ? notesPool.get(random.nextInt(notesPool.size())) : "";
                        Appointment appointment = new Appointment(
                                pid, purpose, dateStr, times.get(k), 30, notes, now);
                        appointment.setOwnerId(ownerId);
                        long aid = appointmentDao.insert(appointment);
                        AuditLogger.log(context, ownerId, LogEntry.ACTION_CREATE, "appointment", aid, purpose);
                        appointment.setId(aid);
                        createdAppointments.add(appointment);
                        savedAppointmentIds.add(String.valueOf(aid));
                        pastPlaced++;
                    }
                }

                for (int offset = 0; offset < 365 && futurePlaced < futureTarget; offset++) {
                    Calendar d = Calendar.getInstance();
                    d.add(Calendar.DAY_OF_MONTH, offset);

                    List<String> times;
                    if (offset == 0) {
                        times = todayTimes(nowMinutes, random);
                    } else {
                        double r = random.nextDouble();
                        int dayCount = r < 0.35 ? 0 : r < 0.72 ? 1 : r < 0.90 ? 2 : r < 0.97 ? 3 : 4;
                        times = pickTimes(dayCount, random);
                    }
                    int count = times.size();
                    if (futurePlaced + count > futureTarget) {
                        count = Math.max(0, futureTarget - futurePlaced);
                        if (times.size() > count) {
                            times = new ArrayList<>(times.subList(0, count));
                        }
                    }

                    String dateStr = fmt.format(d.getTime());
                    for (int k = 0; k < count; k++) {
                        long pid = patientIds.get(cursor % patientIds.size());
                        cursor++;
                        String purpose = purposes.get(random.nextInt(purposes.size()));
                        String notes = random.nextInt(10) < 4
                                ? notesPool.get(random.nextInt(notesPool.size())) : "";
                        Appointment appointment = new Appointment(
                                pid, purpose, dateStr, times.get(k), 30, notes, now);
                        appointment.setOwnerId(ownerId);
                        long aid = appointmentDao.insert(appointment);
                        AuditLogger.log(context, ownerId, LogEntry.ACTION_CREATE, "appointment", aid, purpose);
                        appointment.setId(aid);
                        createdAppointments.add(appointment);
                        savedAppointmentIds.add(String.valueOf(aid));
                        futurePlaced++;
                    }
                }

                Calendar nowRef = Calendar.getInstance();
                for (Appointment a : createdAppointments) {
                    Calendar start = parseAppointmentDate(a.getDate(), a.getTime());
                    if (start == null) {
                        a.setStatus(AppointmentStatus.SCHEDULED);
                    } else if (nowRef.after(start)) {
                        double r = random.nextDouble();
                        a.setStatus(r < 0.60 ? AppointmentStatus.COMPLETED
                                : r < 0.75 ? AppointmentStatus.NO_SHOW
                                : r < 0.90 ? AppointmentStatus.RESCHEDULED
                                : AppointmentStatus.CANCELLED);
                    } else if (random.nextInt(10) < 2) {
                        a.setStatus(AppointmentStatus.CANCELLED);
                    } else if (random.nextInt(10) < 2) {
                        a.setStatus(AppointmentStatus.RESCHEDULED);
                    } else {
                        a.setStatus(AppointmentStatus.SCHEDULED);
                    }
                    appointmentDao.update(a);
                }

                int seriesAdded = 0;
                for (Appointment a : createdAppointments) {
                    if (seriesAdded >= 3) break;
                    if (!AppointmentStatus.SCHEDULED.equals(a.getStatus())) continue;
                    Calendar start = parseAppointmentDate(a.getDate(), a.getTime());
                    if (start == null || !start.after(nowRef)) continue;
                    long groupId = System.currentTimeMillis() + seriesAdded;
                    a.setRecurrenceGroupId(groupId);
                    a.setRecurrenceRule("weekly");
                    appointmentDao.update(a);
                    for (int w = 1; w <= 3; w++) {
                        Appointment copy = new Appointment(
                                a.getPatientId(), a.getName(), addDays(a.getDate(), w * 7),
                                a.getTime(), a.getDuration(), a.getNotes(), a.getCreatedAt());
                        copy.setOwnerId(ownerId);
                        copy.setStatus(AppointmentStatus.SCHEDULED);
                        copy.setRecurrenceGroupId(groupId);
                        copy.setRecurrenceRule("weekly");
                        long copyId = appointmentDao.insert(copy);
                        AuditLogger.log(context, ownerId, LogEntry.ACTION_CREATE, "appointment", copyId, a.getName());
                        savedAppointmentIds.add(String.valueOf(copyId));
                    }
                    seriesAdded++;
                }

                Set<String> existingPatients = new HashSet<>(prefs.getMockPatientIds());
                existingPatients.addAll(savedPatientIds);
                prefs.setMockPatientIds(existingPatients);
                Set<String> existingAppointments = new HashSet<>(prefs.getMockAppointmentIds());
                existingAppointments.addAll(savedAppointmentIds);
                prefs.setMockAppointmentIds(existingAppointments);
                success = true;
            } catch (Exception ignored) {
            } finally {
                final boolean result = success;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onDone(result);
                });
            }
        });
    }

    public static void clearSampleData(Context context, Callback callback) {
        AppDatabase.getExecutor().execute(() -> {
            boolean success = false;
            try {
                PreferencesManager prefs = new PreferencesManager(context);
                PatientDao patientDao = AppDatabase.getInstance(context).patientDao();
                AppointmentDao appointmentDao = AppDatabase.getInstance(context).appointmentDao();
                long ownerId = prefs.getLoggedInUserId();

                for (String id : prefs.getMockAppointmentIds()) {
                    long parsed = Long.parseLong(id);
                    appointmentDao.deleteById(parsed);
                    AuditLogger.log(context, ownerId, LogEntry.ACTION_DELETE, "appointment", parsed, "sample data");
                }
                for (String id : prefs.getMockPatientIds()) {
                    long parsed = Long.parseLong(id);
                    patientDao.deleteById(parsed);
                    AuditLogger.log(context, ownerId, LogEntry.ACTION_DELETE, "patient", parsed, "sample data");
                }
                File attDir = new File(context.getFilesDir(), "attachments");
                File[] attFiles = attDir.listFiles();
                if (attFiles != null) {
                    for (File f : attFiles) {
                        if (f.getName().startsWith("sample-") && f.exists()) {
                            f.delete();
                        }
                    }
                }
                prefs.setMockAppointmentIds(new HashSet<String>());
                prefs.setMockPatientIds(new HashSet<String>());
                success = true;
            } catch (Exception ignored) {
            } finally {
                final boolean result = success;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onDone(result);
                });
            }
        });
    }

    private static List<String> pickTimes(int count, Random random) {
        List<String> slots = new ArrayList<>(Arrays.asList(TIME_SLOTS));
        Collections.shuffle(slots, random);
        return count >= slots.size() ? slots : new ArrayList<>(slots.subList(0, count));
    }

    private static List<String> todayTimes(int nowMinutes, Random random) {
        List<String> past = new ArrayList<>();
        List<String> future = new ArrayList<>();
        for (String slot : TIME_SLOTS) {
            int m = slotToMinutes(slot);
            if (m < nowMinutes) {
                past.add(slot);
            } else if (m > nowMinutes) {
                future.add(slot);
            }
        }
        List<String> result = new ArrayList<>();
        if (!past.isEmpty()) {
            result.add(past.get(random.nextInt(past.size())));
        }
        if (!future.isEmpty()) {
            result.add(future.get(random.nextInt(future.size())));
        }
        Collections.shuffle(future, random);
        int extra = random.nextInt(2);
        for (int i = 0; i < extra && i < future.size(); i++) {
            String s = future.get(i);
            if (!result.contains(s)) {
                result.add(s);
            }
        }
        return result;
    }

    private static int slotToMinutes(String slot) {
        try {
            String[] parts = slot.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String randomPhone(Random random) {
        char prefix = "0234589".charAt(random.nextInt(7));
        return String.format(Locale.US, "05%c%07d", prefix, random.nextInt(10000000));
    }

    private static Calendar parseAppointmentDate(String date, String time) {
        try {
            String[] dp = date.split("/");
            String[] tp = time.split(":");
            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(dp[2]), Integer.parseInt(dp[1]) - 1, Integer.parseInt(dp[0]),
                    Integer.parseInt(tp[0]), Integer.parseInt(tp[1]), 0);
            c.set(Calendar.MILLISECOND, 0);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private static String addDays(String dateStr, int days) {
        try {
            String[] dp = dateStr.split("/");
            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(dp[2]), Integer.parseInt(dp[1]) - 1, Integer.parseInt(dp[0]));
            c.add(Calendar.DAY_OF_MONTH, days);
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.getTime());
        } catch (Exception e) {
            return dateStr;
        }
    }
}
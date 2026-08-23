package com.medcare.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.db.AppointmentDao;
import com.medcare.app.data.db.PatientDao;
import com.medcare.app.data.entity.Appointment;
import com.medcare.app.data.entity.Patient;
import com.medcare.app.utils.PreferencesManager;

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
                PatientDao patientDao = AppDatabase.getInstance(context).patientDao();
                AppointmentDao appointmentDao = AppDatabase.getInstance(context).appointmentDao();
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
                    long id = patientDao.insert(patient);
                    patientIds.add(id);
                    savedPatientIds.add(String.valueOf(id));
                }

                int target = patientIds.size() * 2 + random.nextInt(6) - 3;
                if (target < patientIds.size()) target = patientIds.size();
                SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Set<String> savedAppointmentIds = new HashSet<>();
                int placed = 0;
                int cursor = random.nextInt(patientIds.size());
                Calendar nowCal = Calendar.getInstance();
                int nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE);
                for (int offset = 0; offset < 365 && placed < target; offset++) {
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
                    if (placed + count > target) {
                        count = Math.max(0, target - placed);
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
                        savedAppointmentIds.add(String.valueOf(appointmentDao.insert(appointment)));
                        placed++;
                    }
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

                for (String id : prefs.getMockAppointmentIds()) {
                    appointmentDao.deleteById(Long.parseLong(id));
                }
                for (String id : prefs.getMockPatientIds()) {
                    patientDao.deleteById(Long.parseLong(id));
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
        return String.format(Locale.US, "05%d%07d", 2 + random.nextInt(7), random.nextInt(10000000));
    }
}
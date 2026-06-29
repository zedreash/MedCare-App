package com.medcare.app.data.repository;
import android.content.Context;
import com.medcare.app.data.db.AppDatabase;
import com.medcare.app.data.db.AppointmentDao;
import com.medcare.app.data.entity.Appointment;
import java.util.List;
public class AppointmentRepository {
    private final AppointmentDao appointmentDao;
    public AppointmentRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.appointmentDao = db.appointmentDao();
    }
    public long insert(Appointment appointment) {
        return appointmentDao.insert(appointment);
    }
    public void update(Appointment appointment) {
        appointmentDao.update(appointment);
    }
    public void delete(Appointment appointment) {
        appointmentDao.delete(appointment);
    }
    public List<Appointment> getAllAppointments() {
        return appointmentDao.getAllAppointments();
    }
    public Appointment getAppointmentById(long id) {
        return appointmentDao.getAppointmentById(id);
    }
    public List<Appointment> getAppointmentsByPatientId(long patientId) {
        return appointmentDao.getAppointmentsByPatientId(patientId);
    }
    public List<Appointment> getAppointmentsByDate(String date) {
        return appointmentDao.getAppointmentsByDate(date);
    }
    public int getAppointmentCount() {
        return appointmentDao.getAppointmentCount();
    }
    public int getAppointmentCountByDate(String date) {
        return appointmentDao.getAppointmentCountByDate(date);
    }
}

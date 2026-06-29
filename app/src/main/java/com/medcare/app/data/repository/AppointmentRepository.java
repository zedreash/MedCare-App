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
    public List<Appointment> getAllAppointments(long ownerId) {
        return appointmentDao.getAllAppointments(ownerId);
    }
    public Appointment getAppointmentById(long id, long ownerId) {
        return appointmentDao.getAppointmentById(id, ownerId);
    }
    public List<Appointment> getAppointmentsByPatientId(long patientId, long ownerId) {
        return appointmentDao.getAppointmentsByPatientId(patientId, ownerId);
    }
    public List<Appointment> getAppointmentsByDate(String date, long ownerId) {
        return appointmentDao.getAppointmentsByDate(date, ownerId);
    }
    public int getAppointmentCount(long ownerId) {
        return appointmentDao.getAppointmentCount(ownerId);
    }
    public int getAppointmentCountByDate(String date, long ownerId) {
        return appointmentDao.getAppointmentCountByDate(date, ownerId);
    }
}

package com.medcare.app.utils;

import com.medcare.app.R;

public final class AppointmentStatus {
    public static final String SCHEDULED = "scheduled";
    public static final String COMPLETED = "completed";
    public static final String NO_SHOW = "no_show";
    public static final String CANCELLED = "cancelled";
    public static final String RESCHEDULED = "rescheduled";

    private AppointmentStatus() {}

    public static String[] values() {
        return new String[]{SCHEDULED, COMPLETED, NO_SHOW, CANCELLED, RESCHEDULED};
    }

    public static int labelRes(String status) {
        if (status == null) return R.string.status_scheduled;
        switch (status) {
            case COMPLETED: return R.string.status_completed;
            case NO_SHOW: return R.string.status_no_show;
            case CANCELLED: return R.string.status_cancelled;
            case RESCHEDULED: return R.string.status_rescheduled;
            default: return R.string.status_scheduled;
        }
    }
}
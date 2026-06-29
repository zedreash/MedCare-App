package com.medcare.app.utils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class DateUtils {
    private static final String DISPLAY_FORMAT = "dd/MM/yyyy";
    private static final String STORAGE_FORMAT = "yyyy-MM-dd";
    public static String getCurrentDateForStorage() {
        SimpleDateFormat sdf = new SimpleDateFormat(STORAGE_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }
    public static String getCurrentDateForDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat(DISPLAY_FORMAT, Locale.getDefault());
        return sdf.format(new Date());
    }
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
}

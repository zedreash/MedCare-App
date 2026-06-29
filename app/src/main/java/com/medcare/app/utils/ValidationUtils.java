package com.medcare.app.utils;
import android.util.Patterns;
public class ValidationUtils {
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        StringBuilder digits = new StringBuilder();
        for (char c : phone.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
        }
        String digitStr = digits.toString();
        int len = digitStr.length();
        if (len == 9 || len == 10) return true;
        if (digitStr.startsWith("972") && (len == 11 || len == 12 || len == 13)) return true;
        return false;
    }
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    public static boolean passwordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }
}

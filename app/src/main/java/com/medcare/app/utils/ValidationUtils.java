package com.medcare.app.utils;
import android.util.Patterns;
public class ValidationUtils {
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    public static boolean isValidIsraeliId(String id) {
        if (id == null) return false;
        StringBuilder digits = new StringBuilder();
        for (char c : id.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
        }
        if (digits.length() != 9) return false;
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = digits.charAt(i) - '0';
            int weight = (i % 2 == 0) ? 1 : 2;
            int product = digit * weight;
            sum += product > 9 ? product - 9 : product;
        }
        return sum % 10 == 0;
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

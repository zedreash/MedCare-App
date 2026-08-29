package com.medcare.app.utils;
import android.util.Patterns;
public class ValidationUtils {
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasLetter = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        return hasLetter && hasDigit && hasSpecial;
    }
    public static boolean isValidIsraeliId(String id) {
        if (id == null || id.length() != 9) return false;
        for (int i = 0; i < 9; i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = id.charAt(i) - '0';
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
        String d = digits.toString();
        int len = d.length();
        if (len == 9) {
            return d.charAt(0) == '0'
                    && (d.charAt(1) == '2' || d.charAt(1) == '3' || d.charAt(1) == '4'
                    || d.charAt(1) == '8' || d.charAt(1) == '9');
        }
        if (len == 10) {
            if (d.charAt(0) == '0' && d.charAt(1) == '5') {
                return d.charAt(2) == '0' || d.charAt(2) == '2' || d.charAt(2) == '3'
                        || d.charAt(2) == '4' || d.charAt(2) == '5'
                        || d.charAt(2) == '8' || d.charAt(2) == '9';
            }
            if (d.charAt(0) == '0' && d.charAt(1) == '7') {
                return true;
            }
            return false;
        }
        if (d.startsWith("972") && (len == 11 || len == 12)) {
            return d.charAt(3) == '5' || d.charAt(3) == '7'
                    || d.charAt(3) == '2' || d.charAt(3) == '3' || d.charAt(3) == '4'
                    || d.charAt(3) == '8' || d.charAt(3) == '9';
        }
        return false;
    }
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    public static boolean passwordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }
}

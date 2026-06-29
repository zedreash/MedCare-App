package com.medcare.app.utils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class PasswordUtils {
    private static final String PEPPER = "MedCare@2026!Secure#Hash";
    public static String hash(String password, String email) {
        try {
            String input = PEPPER + password + email.toLowerCase().trim();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public static boolean verify(String password, String email, String storedHash) {
        return hash(password, email).equals(storedHash);
    }
}

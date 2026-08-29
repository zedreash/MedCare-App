package com.medcare.app.utils;

import java.security.SecureRandom;

public final class PassphraseUtils {
    private PassphraseUtils() {}

    private static final char[] CHARSET =
            "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%^&*-_=+?".toCharArray();

    public static int strength(String pass) {
        if (pass == null || pass.length() < 8) return 0;
        int len = pass.length();
        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;
        for (int i = 0; i < len; i++) {
            char c = pass.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isWhitespace(c)) {
                hasSymbol = true;
            }
        }
        if (len >= 16) return 3;
        if (len >= 12 && hasLetter && (hasDigit || hasSymbol)) return 2;
        if (len >= 8 && hasLetter && hasDigit) return 1;
        return 0;
    }

    public static boolean meetsMinimum(String pass) {
        return strength(pass) >= 1;
    }

    public static String generate() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(22);
        for (int i = 0; i < 22; i++) {
            sb.append(CHARSET[random.nextInt(CHARSET.length)]);
        }
        return sb.toString();
    }
}
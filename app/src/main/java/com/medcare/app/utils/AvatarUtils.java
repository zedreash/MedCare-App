package com.medcare.app.utils;

public class AvatarUtils {
    public static String getInitials(String name) {
        if (name == null) return "?";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "?";
        String[] parts = trimmed.split("\\s+");
        String first = parts[0];
        String firstInitial = first.substring(0, 1).toUpperCase();
        if (parts.length == 1) {
            return firstInitial;
        }
        String last = parts[parts.length - 1];
        String lastInitial = last.substring(0, 1).toUpperCase();
        return firstInitial + lastInitial;
    }
}
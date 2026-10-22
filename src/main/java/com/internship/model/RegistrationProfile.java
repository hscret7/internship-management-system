package com.internship.model;

import java.math.BigDecimal;

/** Optional public-registration fields; blank strings match existing NOT NULL columns. */
public record RegistrationProfile(String phone, String university, String major, BigDecimal gpa) {
    public static RegistrationProfile parse(String phone, String university, String major, String gpa) {
        phone = text(phone, 20, "Số điện thoại");
        university = text(university, 150, "Trường");
        major = text(major, 100, "Chuyên ngành");
        if (!phone.isEmpty() && (!phone.matches("\\+?[0-9 ()-]+")
                || phone.chars().filter(Character::isDigit).count() < 7
                || phone.chars().filter(Character::isDigit).count() > 15)) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        BigDecimal value = null;
        if (gpa != null && !gpa.isBlank()) {
            String input = gpa.trim();
            if (input.length() > 16 || !input.matches("[0-9]+(\\.[0-9]{1,2})?")) {
                throw new IllegalArgumentException("GPA phải từ 0 đến 4, tối đa hai chữ số thập phân.");
            }
            value = new BigDecimal(input);
            if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("4")) > 0) {
                throw new IllegalArgumentException("GPA phải từ 0 đến 4.");
            }
        }
        return new RegistrationProfile(phone, university, major, value);
    }
    private static String text(String value, int max, String label) {
        String result = value == null ? "" : value.trim();
        if (result.length() > max || result.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(label + " vượt độ dài hoặc chứa ký tự không hợp lệ.");
        }
        return result;
    }
}

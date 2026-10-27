package com.icastar.platform.util;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    public static final String REGEX = "(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,}";
    public static final String MESSAGE =
            "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a number, and a special character.";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && PATTERN.matcher(password).matches();
    }
}

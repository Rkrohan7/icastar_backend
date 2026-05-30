package com.icastar.platform.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "Rohan@123";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("BCrypt Hash for 'Rohan@123':");
        System.out.println(encodedPassword);
    }
}

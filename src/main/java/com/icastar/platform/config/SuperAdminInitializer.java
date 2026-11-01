package com.icastar.platform.config;

import com.icastar.platform.entity.User;
import com.icastar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class SuperAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${icastar.super-admin.email:}")
    private String email;

    @Value("${icastar.super-admin.password:}")
    private String password;

    @Value("${icastar.super-admin.mobile:}")
    private String mobile;

    @Value("${icastar.super-admin.first-name:Super}")
    private String firstName;

    @Value("${icastar.super-admin.last-name:Admin}")
    private String lastName;

    @Override
    public void run(String... args) {
        createSuperAdminIfNotExists();
    }

    private void createSuperAdminIfNotExists() {
        if (email.isBlank()) {
            log.warn("Super Admin bootstrap skipped because icastar.super-admin.email is not configured.");
            return;
        }

        var existingUser = userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getRole() != User.UserRole.ADMIN || user.getAdminRole() != User.AdminRole.ADMIN) {
                user.setRole(User.UserRole.ADMIN);
                user.setAdminRole(User.AdminRole.ADMIN);
                userRepository.save(user);
            }
            log.info("Configured bootstrap admin account {}", user.getEmail());
        } else {
            if (password.isBlank() || mobile.isBlank()) {
                log.warn("Super Admin bootstrap skipped: configure email, password and mobile together.");
                return;
            }
            User superAdmin = new User();
            superAdmin.setEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
            superAdmin.setMobile(mobile.trim());
            superAdmin.setFirstName(firstName);
            superAdmin.setLastName(lastName);
            superAdmin.setPassword(passwordEncoder.encode(password));
            superAdmin.setRole(User.UserRole.ADMIN);
            superAdmin.setAdminRole(User.AdminRole.ADMIN);
            superAdmin.setStatus(User.UserStatus.ACTIVE);
            superAdmin.setAccountStatus(User.AccountStatus.ACTIVE);
            superAdmin.setIsVerified(true);
            superAdmin.setIsOnboardingComplete(true);
            superAdmin.setFailedLoginAttempts(0);
            superAdmin.setLoginAttempts(0);
            userRepository.save(superAdmin);
            log.info("Bootstrap admin account created.");
        }
    }
}

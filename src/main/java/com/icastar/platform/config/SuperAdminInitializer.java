package com.icastar.platform.config;

import com.icastar.platform.entity.User;
import com.icastar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class SuperAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createSuperAdminIfNotExists();
    }

    private void createSuperAdminIfNotExists() {
        String email = "rohanravikadam@gmail.com";
        String password = "Rohan@123";
        String mobile = "+919999999999";

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Always update password and ensure ADMIN role
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(User.UserRole.ADMIN);
            user.setStatus(User.UserStatus.ACTIVE);
            user.setAccountStatus(User.AccountStatus.ACTIVE);
            user.setIsVerified(true);
            user.setIsOnboardingComplete(true);
            userRepository.save(user);
            log.info("✅ SUPER ADMIN updated: {}", email);
            log.info("   Password reset to: {}", password);
        } else {
            // Check if mobile exists
            Optional<User> existingMobile = userRepository.findByMobile(mobile);
            if (existingMobile.isPresent()) {
                mobile = "+919999999998"; // Use alternate mobile
            }

            User superAdmin = new User();
            superAdmin.setEmail(email);
            superAdmin.setMobile(mobile);
            superAdmin.setFirstName("Rohan");
            superAdmin.setLastName("Kadam");
            superAdmin.setPassword(passwordEncoder.encode(password));
            superAdmin.setRole(User.UserRole.ADMIN);
            superAdmin.setStatus(User.UserStatus.ACTIVE);
            superAdmin.setAccountStatus(User.AccountStatus.ACTIVE);
            superAdmin.setIsVerified(true);
            superAdmin.setIsOnboardingComplete(true);
            superAdmin.setFailedLoginAttempts(0);
            superAdmin.setLoginAttempts(0);

            userRepository.save(superAdmin);
            log.info("✅ SUPER ADMIN created successfully!");
            log.info("   Email: {}", email);
            log.info("   Password: {}", password);
        }
    }
}

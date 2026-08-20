package com.icastar.platform.controller;

import com.icastar.platform.dto.auth.*;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.ArtistProfileArtistType;
import com.icastar.platform.entity.ArtistType;
import com.icastar.platform.entity.Otp;
import com.icastar.platform.entity.PasswordResetToken;
import com.icastar.platform.entity.RecruiterProfile;
import com.icastar.platform.entity.User;
import com.icastar.platform.repository.ArtistProfileRepository;
import com.icastar.platform.repository.ArtistProfileArtistTypeRepository;
import com.icastar.platform.repository.ArtistTypeRepository;
import com.icastar.platform.repository.PasswordResetTokenRepository;
import com.icastar.platform.repository.RecruiterProfileRepository;
import com.icastar.platform.repository.UserRepository;
import com.icastar.platform.security.JwtTokenProvider;
import com.icastar.platform.service.EmailService;
import com.icastar.platform.service.OtpService;
import com.icastar.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
public class AuthController {

    private final OtpService otpService;
    private final UserService userService;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistProfileArtistTypeRepository artistProfileArtistTypeRepository;
    private final ArtistTypeRepository artistTypeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;


    @Operation(
            summary = "Send OTP",
            description = "Send OTP to user's mobile number for authentication. If user exists, sends login OTP; otherwise sends registration OTP.",
            operationId = "sendOtp"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = "{\"success\": true, \"message\": \"OTP sent successfully\", \"mobile\": \"+919876543210\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or mobile number",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Error Response",
                                    value = "{\"success\": false, \"message\": \"Invalid mobile number format\"}"
                            )
                    )
            )
    })
    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @Parameter(description = "OTP request details", required = true)
            @Valid @RequestBody OtpRequestDto request) {
        try {
            // Check if user exists for login OTP
            User existingUser = userRepository.findByEmailOrMobile(request.getMobile(), request.getMobile()).orElse(null);
            
            if (existingUser != null) {
                // User exists - send login OTP
                otpService.sendOtp(request.getMobile(), existingUser.getEmail(), Otp.OtpType.LOGIN);
            } else {
                // User doesn't exist - send registration OTP
                otpService.sendOtp(request.getMobile(), null, Otp.OtpType.REGISTRATION);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP sent successfully");
            response.put("data", Map.of("mobile", request.getMobile()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending OTP", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send OTP");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    @GetMapping("/vishwa")
    public String register() {
        return "Registered!";
    }

    /**
     * Get current logged-in user details
     * GET /api/auth/me
     */
    @GetMapping("/me")
    @Operation(summary = "Get Current User", description = "Returns the currently authenticated user's details")
    @SecurityRequirement(name = "bearerAuth")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Not authenticated");
                return ResponseEntity.status(401).body(response);
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Build user data response
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("email", user.getEmail());
            userData.put("mobile", user.getMobile());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());
            userData.put("role", user.getRole());
            userData.put("status", user.getStatus());
            userData.put("isVerified", user.getIsVerified());
            userData.put("isOnboardingComplete", user.getIsOnboardingComplete());
            userData.put("lastLogin", user.getLastLogin());

            // Add profile info based on role
            if (User.UserRole.ARTIST.equals(user.getRole())) {
                artistProfileRepository.findByUserId(user.getId()).ifPresent(artistProfile -> {
                    Map<String, Object> profileData = new HashMap<>();
                    profileData.put("artistProfileId", artistProfile.getId());
                    profileData.put("stageName", artistProfile.getStageName());
                    profileData.put("isVerified", artistProfile.getIsVerifiedBadge());
                    profileData.put("isProfileComplete", artistProfile.getIsProfileComplete());

                    // Primary artist type (backward compatibility)
                    if (artistProfile.getArtistType() != null) {
                        Map<String, Object> artistTypeData = new HashMap<>();
                        artistTypeData.put("id", artistProfile.getArtistType().getId());
                        artistTypeData.put("name", artistProfile.getArtistType().getName());
                        artistTypeData.put("displayName", artistProfile.getArtistType().getDisplayName());
                        profileData.put("artistType", artistTypeData);
                    }

                    // All artist types (new field for multiple professions)
                    List<ArtistProfileArtistType> allArtistTypes = artistProfileArtistTypeRepository
                            .findByArtistProfileIdOrderBySortOrder(artistProfile.getId());

                    if (allArtistTypes != null && !allArtistTypes.isEmpty()) {
                        List<Map<String, Object>> artistTypesList = new ArrayList<>();
                        for (ArtistProfileArtistType apat : allArtistTypes) {
                            Map<String, Object> typeData = new HashMap<>();
                            typeData.put("id", apat.getArtistType().getId());
                            typeData.put("name", apat.getArtistType().getName());
                            typeData.put("displayName", apat.getArtistType().getDisplayName());
                            artistTypesList.add(typeData);
                        }
                        profileData.put("artistTypes", artistTypesList);
                    } else if (artistProfile.getArtistType() != null) {
                        // Fallback: if no entries in join table, use the primary artist type
                        List<Map<String, Object>> artistTypesList = new ArrayList<>();
                        Map<String, Object> typeData = new HashMap<>();
                        typeData.put("id", artistProfile.getArtistType().getId());
                        typeData.put("name", artistProfile.getArtistType().getName());
                        typeData.put("displayName", artistProfile.getArtistType().getDisplayName());
                        artistTypesList.add(typeData);
                        profileData.put("artistTypes", artistTypesList);
                    }

                    userData.put("artistProfile", profileData);
                });
            } else if (User.UserRole.RECRUITER.equals(user.getRole())) {
                recruiterProfileRepository.findByUserId(user.getId()).ifPresent(recruiterProfile -> {
                    Map<String, Object> profileData = new HashMap<>();
                    profileData.put("recruiterProfileId", recruiterProfile.getId());
                    profileData.put("companyName", recruiterProfile.getCompanyName());
                    profileData.put("isVerifiedCompany", recruiterProfile.getIsVerifiedCompany());
                    userData.put("recruiterProfile", profileData);
                });
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting current user", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get user details");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerificationDto request) {
        try {
            // Try login OTP first
            boolean isLoginOtp = otpService.verifyOtp(request.getMobile(), null, request.getOtp(), Otp.OtpType.LOGIN);
            
            if (isLoginOtp) {
                // User login
                User user = userRepository.findByEmailOrMobile(request.getMobile(), request.getMobile())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                
                // Update last login
                user.setLastLogin(LocalDateTime.now());
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
                userRepository.save(user);

                String token = jwtTokenProvider.generateTokenFromUsername(user.getEmail());
                AuthResponseDto authResponse = new AuthResponseDto(token, user);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("data", authResponse);

                return ResponseEntity.ok(response);
            }

            // Try registration OTP
            boolean isRegistrationOtp = otpService.verifyOtp(request.getMobile(), null, request.getOtp(), Otp.OtpType.REGISTRATION);
            
            if (isRegistrationOtp) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "OTP verified successfully. Please complete registration.");
                response.put("data", Map.of("mobile", request.getMobile(), "verified", true));

                return ResponseEntity.ok(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid OTP");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error verifying OTP", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to verify OTP");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody OtpRequestDto request) {
        try {
            User user = userRepository.findByEmailOrMobile(request.getMobile(), request.getMobile())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            otpService.sendOtp(request.getMobile(), user.getEmail(), Otp.OtpType.FORGOT_PASSWORD);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP sent for password reset");
            response.put("data", Map.of("mobile", request.getMobile()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending forgot password OTP", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send reset OTP");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody OtpVerificationDto request, 
                                                           @RequestParam String newPassword) {
        try {
            // Verify OTP
            boolean isOtpVerified = otpService.verifyOtp(request.getMobile(), null, request.getOtp(), Otp.OtpType.FORGOT_PASSWORD);
            
            if (!isOtpVerified) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid OTP");
                return ResponseEntity.badRequest().body(response);
            }

            // Update password
            User user = userRepository.findByEmailOrMobile(request.getMobile(), request.getMobile())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successful");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resetting password", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to reset password");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== EMAIL-BASED FORGOT PASSWORD ====================

    /**
     * Request password reset via email
     * POST /api/auth/forgot-password
     * Sends a reset link to the user's email
     */
    @Operation(summary = "Request password reset", description = "Send password reset link to user's email")
    @PostMapping("/forgot-password/email")
    @Transactional
    public ResponseEntity<Map<String, Object>> forgotPasswordEmail(@Valid @RequestBody ForgotPasswordRequestDto request) {
        try {
            log.info("Password reset requested for email: {}", request.getEmail());

            // Find user by email
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);

            // Always return success to prevent email enumeration attacks
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "If an account exists with this email, you will receive a password reset link shortly.");

            if (user == null) {
                log.warn("Password reset requested for non-existent email: {}", request.getEmail());
                return ResponseEntity.ok(response);
            }

            // Invalidate any existing tokens for this user
            passwordResetTokenRepository.invalidateAllUserTokens(user, LocalDateTime.now());

            // Generate new reset token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(token, user);
            passwordResetTokenRepository.save(resetToken);

            // Send reset email
            String resetLink = "https://www.icastar.com/reset-password?token=" + token;
            String emailBody = buildPasswordResetEmail(user.getFirstName(), resetLink);
            emailService.sendHtmlEmail(user.getEmail(), "Reset Your Password - iCastar", emailBody);

            log.info("Password reset email sent to: {}", request.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing forgot password request", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to process password reset request");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Verify reset token is valid
     * GET /api/auth/verify-reset-token?token=xxx
     */
    @Operation(summary = "Verify reset token", description = "Check if password reset token is valid")
    @GetMapping("/verify-reset-token")
    public ResponseEntity<Map<String, Object>> verifyResetToken(@RequestParam String token) {
        try {
            PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                    .orElse(null);

            Map<String, Object> response = new HashMap<>();

            if (resetToken == null) {
                response.put("success", false);
                response.put("valid", false);
                response.put("message", "Invalid reset token");
                return ResponseEntity.badRequest().body(response);
            }

            if (!resetToken.isValid()) {
                response.put("success", false);
                response.put("valid", false);
                response.put("message", resetToken.isExpired() ? "Reset token has expired" : "Reset token has already been used");
                return ResponseEntity.badRequest().body(response);
            }

            response.put("success", true);
            response.put("valid", true);
            response.put("email", resetToken.getUser().getEmail());
            response.put("message", "Token is valid");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error verifying reset token", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to verify token");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reset password using token
     * POST /api/auth/reset-password/email
     */
    @Operation(summary = "Reset password with token", description = "Reset password using the token received via email")
    @PostMapping("/reset-password/email")
    @Transactional
    public ResponseEntity<Map<String, Object>> resetPasswordWithToken(@Valid @RequestBody ResetPasswordRequestDto request) {
        try {
            // Validate passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Passwords do not match");
                return ResponseEntity.badRequest().body(response);
            }

            // Find and validate token
            PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                    .orElseThrow(() -> new RuntimeException("Invalid reset token"));

            if (!resetToken.isValid()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", resetToken.isExpired() ? "Reset token has expired" : "Reset token has already been used");
                return ResponseEntity.badRequest().body(response);
            }

            // Update user password
            User user = resetToken.getUser();
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);

            // Mark token as used
            resetToken.markAsUsed();
            passwordResetTokenRepository.save(resetToken);

            log.info("Password reset successful for user: {}", user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successful. You can now login with your new password.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error resetting password with token", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to reset password: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Build HTML email body for password reset
     */
    private String buildPasswordResetEmail(String firstName, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f59e0b; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9fafb; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #f59e0b; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>iCastar</h1>
                    </div>
                    <div class="content">
                        <h2>Password Reset Request</h2>
                        <p>Hi %s,</p>
                        <p>We received a request to reset your password. Click the button below to create a new password:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Reset Password</a>
                        </p>
                        <p>This link will expire in 30 minutes.</p>
                        <p>If you didn't request this password reset, you can safely ignore this email. Your password will remain unchanged.</p>
                        <p>Best regards,<br>The iCastar Team</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated email. Please do not reply.</p>
                        <p>&copy; 2024 iCastar. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName != null ? firstName : "User", resetLink);
    }

    // Email-based authentication endpoints (without OTP verification)

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> emailLogin(@Valid @RequestBody EmailLoginRequestDto request) {
        try {
            // Find user by email
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid email or password"));

            // Check if user is active
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Account is not active. Please contact support.");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if account is locked
            if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Account is temporarily locked. Please try again later.");
                return ResponseEntity.badRequest().body(response);
            }

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                // Increment failed login attempts
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                
                // Lock account after 5 failed attempts for 30 minutes
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
                }
                userRepository.save(user);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid email or password");
                return ResponseEntity.badRequest().body(response);
            }

            // Reset failed login attempts and update last login
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT token
            String token = jwtTokenProvider.generateTokenFromUsername(user.getEmail());
            AuthResponseDto authResponse = new AuthResponseDto(token, user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("data", authResponse);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during email login", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Login failed");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> emailRegister(@Valid @RequestBody EmailRegisterRequestDto request) {
        try {
            // Check if user already exists
            if (userRepository.findByEmailOrMobile(request.getEmail(), request.getMobile()).isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User already exists with this email or mobile");
                return ResponseEntity.badRequest().body(response);
            }

            // Create user
            User user = new User();
            user.setEmail(request.getEmail());
            user.setMobile(request.getMobile());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getUserRole());
            user.setStatus(User.UserStatus.ACTIVE);
            user.setIsVerified(true); // Auto-verify for email registration
            user.setFailedLoginAttempts(0);
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());

            User savedUser = userRepository.save(user);

           if(User.UserRole.RECRUITER == request.getUserRole()){
               RecruiterProfile recruiterProfile= new RecruiterProfile();
               recruiterProfile.setUser(user);
               recruiterProfile.setIsVerifiedCompany(Boolean.FALSE);
               recruiterProfile.setCompanyName("");
               recruiterProfile.setContactPersonName(request.getFirstName() + " " + request.getLastName());
               recruiterProfileRepository.save(recruiterProfile);
           } else if(User.UserRole.ARTIST == request.getUserRole()){
               // Get default artist type (OTHER) for initial profile creation
               ArtistType defaultArtistType = artistTypeRepository.findByName("OTHER")
                       .orElseGet(() -> artistTypeRepository.findAll().stream().findFirst()
                               .orElseThrow(() -> new RuntimeException("No artist types found in database")));

               ArtistProfile artistProfile = new ArtistProfile();
               artistProfile.setUser(savedUser);
               artistProfile.setArtistType(defaultArtistType);
               artistProfile.setFirstName(request.getFirstName());
               artistProfile.setLastName(request.getLastName());
               artistProfile.setIsVerifiedBadge(false);
               artistProfile.setIsProfileComplete(false);
               artistProfile.setTotalApplications(0);
               artistProfile.setSuccessfulHires(0);
               artistProfileRepository.save(artistProfile);
           }

            // Email sending disabled temporarily
            // try {
            //     emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getEmail());
            // } catch (Exception e) {
            //     log.warn("Failed to send welcome email: {}", e.getMessage());
            // }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("data", Map.of(
                "id", savedUser.getId(),
                "email", savedUser.getEmail(),
                "mobile", savedUser.getMobile(),
                "role", savedUser.getRole(),
                "status", savedUser.getStatus(),
                "isVerified", savedUser.getIsVerified()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during email registration", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Registration failed");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

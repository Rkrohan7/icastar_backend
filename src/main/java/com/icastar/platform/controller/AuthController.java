package com.icastar.platform.controller;

import com.icastar.platform.dto.auth.*;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.ArtistProfileArtistType;
import com.icastar.platform.entity.Otp;
import com.icastar.platform.entity.PasswordResetToken;
import com.icastar.platform.entity.RecruiterProfile;
import com.icastar.platform.entity.User;
import com.icastar.platform.exception.AuthFlowException;
import com.icastar.platform.repository.ArtistProfileRepository;
import com.icastar.platform.repository.ArtistProfileArtistTypeRepository;
import com.icastar.platform.repository.PasswordResetTokenRepository;
import com.icastar.platform.repository.RecruiterProfileRepository;
import com.icastar.platform.repository.UserRepository;
import com.icastar.platform.security.JwtTokenProvider;
import com.icastar.platform.service.EmailService;
import com.icastar.platform.service.AuthVerificationService;
import com.icastar.platform.service.OtpService;
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
import org.springframework.http.HttpStatus;
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
    private final AuthVerificationService authVerificationService;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistProfileArtistTypeRepository artistProfileArtistTypeRepository;
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
        return legacyAuthFlowDisabled();
    }

    @PostMapping("/register/verification/request")
    public ResponseEntity<Map<String, Object>> requestRegistrationVerification(
            @Valid @RequestBody EmailRegisterRequestDto request) {
        try {
            String challengeId = authVerificationService.requestRegistration(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("challengeId", challengeId)));
        } catch (AuthFlowException e) {
            return authFlowError(e);
        } catch (Exception e) {
            log.error("Failed to start registration verification", e);
            return serviceUnavailable("Unable to send verification codes. Please try again.");
        }
    }

    @PostMapping("/register/verification/verify")
    public ResponseEntity<Map<String, Object>> verifyRegistration(
            @Valid @RequestBody com.icastar.platform.dto.auth.AuthVerificationDtos.RegistrationVerifyRequest request) {
        try {
            User user = authVerificationService.verifyRegistration(
                    request.getChallengeId(), request.getEmailOtp()).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid verification code or maximum attempts reached."));
            }
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("email", user.getEmail());
            data.put("mobile", user.getMobile());
            data.put("role", user.getRole());
            data.put("status", user.getStatus());
            data.put("isVerified", user.getIsVerified());
            data.put("emailVerified", user.getEmailVerified());
            data.put("mobileVerified", user.getMobileVerified());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Registration completed successfully.",
                    "data", data));
        } catch (AuthFlowException e) {
            return authFlowError(e);
        } catch (Exception e) {
            log.error("Failed to verify registration", e);
            return serviceUnavailable("Unable to complete registration. Please try again.");
        }
    }

    @PostMapping("/register/verification/resend")
    public ResponseEntity<Map<String, Object>> resendRegistrationVerification(
            @Valid @RequestBody com.icastar.platform.dto.auth.AuthVerificationDtos.RegistrationResendRequest request) {
        try {
            authVerificationService.resendRegistration(request.getChallengeId(), request.getChannel());
            return ResponseEntity.ok(Map.of("success", true, "message", "Verification code resent."));
        } catch (AuthFlowException e) {
            return authFlowError(e);
        } catch (Exception e) {
            log.error("Failed to resend registration verification code", e);
            return serviceUnavailable("Unable to resend verification code. Please try again.");
        }
    }

    @PostMapping("/login/otp/request")
    public ResponseEntity<Map<String, Object>> requestLoginOtp(
            @Valid @RequestBody com.icastar.platform.dto.auth.AuthVerificationDtos.LoginOtpRequest request) {
        try {
            String challengeId = authVerificationService.requestLogin(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("challengeId", challengeId)));
        } catch (AuthFlowException e) {
            return authFlowError(e);
        } catch (Exception e) {
            log.error("Failed to start login OTP verification", e);
            return serviceUnavailable("Unable to send login verification code. Please try again.");
        }
    }

    @PostMapping("/login/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyLoginOtp(
            @Valid @RequestBody com.icastar.platform.dto.auth.AuthVerificationDtos.LoginVerifyRequest request) {
        try {
            User user = authVerificationService.verifyLogin(request.getChallengeId(), request.getOtp()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Invalid verification code or maximum attempts reached."));
            }
            String token = jwtTokenProvider.generateTokenFromUsername(user.getEmail());
            AuthResponseDto authResponse = new AuthResponseDto(token, user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Login successful",
                    "data", authResponse));
        } catch (AuthFlowException e) {
            return authFlowError(e);
        } catch (Exception e) {
            log.error("Failed to verify login OTP", e);
            return serviceUnavailable("Unable to complete login. Please try again.");
        }
    }

    @PostMapping("/login/otp/resend")
    public ResponseEntity<Map<String, Object>> resendLoginOtp(
            @Valid @RequestBody com.icastar.platform.dto.auth.AuthVerificationDtos.LoginResendRequest request) {
        try {
            authVerificationService.resendLogin(request.getChallengeId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Login verification code resent."));
        } catch (AuthFlowException e) {
            return authFlowError(e);
        } catch (Exception e) {
            log.error("Failed to resend login OTP", e);
            return serviceUnavailable("Unable to resend login verification code. Please try again.");
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
                    profileData.put("isVerified", artistProfile.hasApprovedFaceAndIdProof());
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
                    profileData.put("isVerified", recruiterProfile.hasApprovedFaceAndIdProof());
                    profileData.put("faceVerificationStatus", recruiterProfile.getFaceVerificationStatus().name());
                    profileData.put("isFaceVerified", recruiterProfile.getIsFaceVerified());
                    profileData.put("idProofVerificationStatus", recruiterProfile.getIdProofVerificationStatus().name());
                    profileData.put("idProofVerified", recruiterProfile.getIdProofVerified());
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
        return legacyAuthFlowDisabled();
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

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> emailLogin(@Valid @RequestBody EmailLoginRequestDto request) {
        return legacyAuthFlowDisabled();
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> emailRegister(@Valid @RequestBody EmailRegisterRequestDto request) {
        return legacyAuthFlowDisabled();
    }

    private ResponseEntity<Map<String, Object>> legacyAuthFlowDisabled() {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                "success", false,
                "errorCode", "OTP_VERIFICATION_REQUIRED",
                "message", "Use the OTP verification request and verify endpoints."));
    }

    private ResponseEntity<Map<String, Object>> authFlowError(AuthFlowException exception) {
        return ResponseEntity.status(exception.getStatus()).body(Map.of(
                "success", false,
                "message", exception.getMessage()));
    }

    private ResponseEntity<Map<String, Object>> serviceUnavailable(String message) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "message", message));
    }
}

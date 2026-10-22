package com.icastar.platform.controller;

import com.icastar.platform.dto.user.ChangePasswordDto;
import com.icastar.platform.dto.user.UpdateUserProfileDto;
import com.icastar.platform.dto.user.UserProfileDto;
import com.icastar.platform.entity.User;
import com.icastar.platform.exception.BusinessException;
import com.icastar.platform.service.UserService;
import com.icastar.platform.service.ArtistService;
import com.icastar.platform.service.RecruiterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final ArtistService artistService;
    private final RecruiterService recruiterService;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getCurrentUserProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserProfileDto userProfile = new UserProfileDto(user);
            
            // Add role-specific profile data
            if (user.getRole() == User.UserRole.ARTIST && user.getArtistProfile() != null) {
                userProfile.setFirstName(user.getFirstName());
                userProfile.setLastName(user.getLastName());
                userProfile.setBio(user.getArtistProfile().getBio());
                userProfile.setLocation(user.getArtistProfile().getLocation());
                // Note: Profile images are now handled by Document entity
            } else if (user.getRole() == User.UserRole.RECRUITER && user.getRecruiterProfile() != null) {
                userProfile.setFirstName(user.getRecruiterProfile().getContactPersonName());
                userProfile.setLastName(""); // Recruiters don't have last name in contact person
                userProfile.setBio(user.getRecruiterProfile().getCompanyDescription());
                userProfile.setLocation(user.getRecruiterProfile().getLocation());
                // Note: Company logos are now handled by Document entity
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userProfile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting user profile", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get user profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateUserProfile(@Valid @RequestBody UpdateUserProfileDto updateDto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update basic user information
            if (updateDto.getEmail() != null && !updateDto.getEmail().equals(user.getEmail())) {
                // Check if email is already taken
                if (userService.findByEmail(updateDto.getEmail()).isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Email already exists");
                    return ResponseEntity.badRequest().body(response);
                }
                user.setEmail(updateDto.getEmail());
            }

            if (updateDto.getMobile() != null && !updateDto.getMobile().equals(user.getMobile())) {
                // Check if mobile is already taken
                if (userService.findByMobile(updateDto.getMobile()).isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Mobile number already exists");
                    return ResponseEntity.badRequest().body(response);
                }
                user.setMobile(updateDto.getMobile());
            }

            userService.save(user);

            // Update role-specific profile
            if (user.getRole() == User.UserRole.ARTIST) {
                artistService.updateBasicProfile(user.getId(), updateDto);
            } else if (user.getRole() == User.UserRole.RECRUITER) {
                recruiterService.updateBasicProfile(user.getId(), updateDto);
            }

            UserProfileDto updatedProfile = new UserProfileDto(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("data", updatedProfile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating user profile", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserProfileDto userProfile = new UserProfileDto(user);
            
            // Add role-specific profile data
            if (user.getRole() == User.UserRole.ARTIST && user.getArtistProfile() != null) {
                userProfile.setFirstName(user.getFirstName());
                userProfile.setLastName(user.getLastName());
                userProfile.setBio(user.getArtistProfile().getBio());
                userProfile.setLocation(user.getArtistProfile().getLocation());
                // Note: Profile images are now handled by Document entity
            } else if (user.getRole() == User.UserRole.RECRUITER && user.getRecruiterProfile() != null) {
                userProfile.setFirstName(user.getRecruiterProfile().getContactPersonName());
                userProfile.setLastName("");
                userProfile.setBio(user.getRecruiterProfile().getCompanyDescription());
                userProfile.setLocation(user.getRecruiterProfile().getLocation());
                // Note: Company logos are now handled by Document entity
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userProfile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting user profile", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get user profile");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam String email) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User user = userService.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.getEmail().equals(email)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email does not match user account");
                return ResponseEntity.badRequest().body(response);
            }

            // TODO: Send email verification
            // For now, just mark as verified
            user.setIsVerified(true);
            userService.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email verification sent");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending email verification", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send email verification");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordDto changePasswordDto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Call service to change password with all validations
            userService.changePassword(
                user.getId(),
                changePasswordDto.getCurrentPassword(),
                changePasswordDto.getNewPassword(),
                changePasswordDto.getConfirmPassword()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password changed successfully");

            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            log.warn("Password change failed: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error changing password", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to change password");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getUserDashboard() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("user", new UserProfileDto(user));
            dashboard.put("lastLogin", user.getLastLogin());
            dashboard.put("isVerified", user.getIsVerified());
            dashboard.put("status", user.getStatus());

            // Add role-specific dashboard data
            if (user.getRole() == User.UserRole.ARTIST && user.getArtistProfile() != null) {
                Map<String, Object> artistStats = new HashMap<>();
                artistStats.put("totalApplications", user.getArtistProfile().getTotalApplications());
                artistStats.put("successfulHires", user.getArtistProfile().getSuccessfulHires());
                artistStats.put("isVerifiedBadge", user.getArtistProfile().hasApprovedFaceAndIdProof());
                artistStats.put("faceVerificationStatus", user.getArtistProfile().getFaceVerificationStatus().name());
                artistStats.put("isFaceVerified", user.getArtistProfile().getIsFaceVerified());
                artistStats.put("idProofVerified", user.getArtistProfile().getIdProofVerified());
                artistStats.put("idProofVerificationStatus",
                        user.getArtistProfile().getIdProofVerificationStatus().name());
                dashboard.put("artistStats", artistStats);
            } else if (user.getRole() == User.UserRole.RECRUITER && user.getRecruiterProfile() != null) {
                Map<String, Object> recruiterStats = new HashMap<>();
                recruiterStats.put("totalJobsPosted", user.getRecruiterProfile().getTotalJobsPosted());
                recruiterStats.put("successfulHires", user.getRecruiterProfile().getSuccessfulHires());
                recruiterStats.put("chatCredits", user.getRecruiterProfile().getChatCredits());
                recruiterStats.put("isVerifiedCompany", user.getRecruiterProfile().getIsVerifiedCompany());
                dashboard.put("recruiterStats", recruiterStats);
            }

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error fetching user dashboard", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch dashboard");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/request-verification")
    public ResponseEntity<Map<String, Object>> requestVerification() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // TODO: Implement verification request logic
            // This would typically send a notification to admins
            // and set a verification request timestamp

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification request submitted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error requesting verification", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to submit verification request");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/activity")
    public ResponseEntity<Map<String, Object>> getUserActivity() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> activity = new HashMap<>();
            activity.put("lastLogin", user.getLastLogin());
            activity.put("createdAt", user.getCreatedAt());
            activity.put("updatedAt", user.getUpdatedAt());
            activity.put("failedLoginAttempts", user.getFailedLoginAttempts());
            activity.put("accountLockedUntil", user.getAccountLockedUntil());

            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            log.error("Error fetching user activity", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch user activity");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateAccount() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Soft delete - set as inactive
            user.setStatus(User.UserStatus.INACTIVE);
            user.setIsActive(false);
            userService.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account deactivated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deactivating account", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to deactivate account");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

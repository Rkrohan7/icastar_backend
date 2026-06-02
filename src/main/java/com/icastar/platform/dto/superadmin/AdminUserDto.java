package com.icastar.platform.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private String role;
    private String status;
    private String accountStatus;
    private Boolean isVerified;
    private Boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Request DTOs for admin operations

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAdminRequest {
        @NotBlank(message = "First name is required")
        private String firstName;

        @NotBlank(message = "Last name is required")
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        private String email;

        @NotBlank(message = "Mobile is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Valid mobile number is required")
        private String mobile;

        @NotBlank(message = "Password is required")
        private String password;

        private String role; // Default to ADMIN if not specified
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAdminRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String mobile;
        private String password; // Optional - only update if provided
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeStatusRequest {
        @NotBlank(message = "Status is required")
        private String status; // ACTIVE, INACTIVE, SUSPENDED, BANNED

        private String reason; // Optional reason for status change
    }
}

package com.icastar.platform.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApprovalDto {
    private Long id;
    private String title;
    private String description;
    private String requirements;
    private String location;
    private String jobType;
    private String experienceLevel;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String currency;
    private Integer durationDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate applicationDeadline;
    private Boolean isRemote;
    private Boolean isUrgent;
    private Boolean isFeatured;
    private String status;
    private String tags;
    private String skillsRequired;
    private String benefits;
    private String contactEmail;
    private String contactPhone;

    // Recruiter info
    private Long recruiterId;
    private String recruiterName;
    private String recruiterEmail;
    private String companyName;

    // Approval info
    private LocalDateTime approvedAt;
    private String approvedByName;
    private LocalDateTime rejectedAt;
    private String rejectedByName;
    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Request DTOs

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproveJobRequest {
        private String notes; // Optional admin notes
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectJobRequest {
        @NotBlank(message = "Rejection reason is required")
        private String reason;
    }
}
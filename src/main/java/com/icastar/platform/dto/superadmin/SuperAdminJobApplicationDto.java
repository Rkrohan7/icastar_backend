package com.icastar.platform.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminJobApplicationDto {

    private Long id;
    private String status;
    private String coverLetter;
    private Double expectedSalary;
    private LocalDate availabilityDate;
    private String portfolioUrl;
    private String resumeUrl;
    private String demoReelUrl;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime interviewScheduledAt;
    private String interviewNotes;
    private String rejectionReason;
    private String feedback;
    private Integer rating;
    private Boolean isShortlisted;
    private Boolean isHired;
    private LocalDateTime hiredAt;
    private Double offeredSalary;

    // Job details
    private Long jobId;
    private String jobTitle;
    private String jobLocation;
    private String jobType;
    private String jobStatus;

    // Artist details
    private Long artistId;
    private String artistName;
    private String artistEmail;
    private String artistMobile;
    private String artistProfileImage;
    private String artistType;

    // Recruiter details
    private Long recruiterId;
    private String recruiterName;
    private String recruiterEmail;
    private String companyName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        private String status;
        private String feedback;
        private String rejectionReason;
    }
}
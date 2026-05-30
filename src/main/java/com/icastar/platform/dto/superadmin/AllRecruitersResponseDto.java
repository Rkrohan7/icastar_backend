package com.icastar.platform.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllRecruitersResponseDto {

    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private String companyName;
    private String recruiterCategory; // PRODUCTION_HOUSE, CASTING_DIRECTOR, INDIVIDUAL
    private String designation;
    private String location;
    private String city;
    private String state;
    private String country;
    private String profileImage;
    private String companyLogo;
    private String website;
    private String linkedIn;
    private String bio;

    // Verification Status
    private Boolean isVerified;
    private Boolean isEmailVerified;
    private Boolean isMobileVerified;
    private Boolean isDocumentVerified;

    // Account Status
    private String accountStatus; // ACTIVE, INACTIVE, SUSPENDED, BANNED
    private Boolean isActive;
    private Boolean isOnboardingComplete;

    // Statistics
    private Long totalJobsPosted;
    private Long activeJobs;
    private Long closedJobs;
    private Long totalApplicationsReceived;
    private Long totalHires;
    private Long totalCastingCalls;
    private Long totalAuditionsScheduled;
    private Long totalHireRequestsSent;

    // Subscription Info
    private String subscriptionPlan;
    private String subscriptionStatus;
    private LocalDateTime subscriptionExpiresAt;

    // Dates
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastActivityAt;

    // Recent Jobs
    private List<RecentJobDto> recentJobs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentJobDto {
        private Long id;
        private String title;
        private String status;
        private Long applicationCount;
        private LocalDateTime createdAt;
    }
}

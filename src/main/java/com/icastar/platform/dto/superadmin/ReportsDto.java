package com.icastar.platform.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReportsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserReportDto {
        private LocalDate startDate;
        private LocalDate endDate;
        private Long totalNewUsers;
        private Long newArtists;
        private Long newRecruiters;
        private Long activeUsers;
        private Long inactiveUsers;
        private Long verifiedUsers;
        private Long suspendedUsers;
        private Long bannedUsers;
        private Long deletedUsers;
        private Map<String, Long> dailyRegistrations; // date -> count
        private Map<String, Long> usersByLocation; // city -> count
        private Map<String, Long> usersByArtistType; // artistType -> count
        private Map<String, Long> usersByRecruiterCategory; // category -> count
        private Double averageProfileCompletionRate;
        private LocalDateTime generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobReportDto {
        private LocalDate startDate;
        private LocalDate endDate;
        private Long totalJobsPosted;
        private Long activeJobs;
        private Long closedJobs;
        private Long expiredJobs;
        private Long featuredJobs;
        private Long totalApplicationsReceived;
        private Double averageApplicationsPerJob;
        private Long totalHiresMade;
        private Double conversionRate; // applications to hires
        private Map<String, Long> dailyJobPostings; // date -> count
        private Map<String, Long> jobsByType; // jobType -> count
        private Map<String, Long> jobsByLocation; // city -> count
        private Map<String, Long> jobsByCategory; // category -> count
        private Map<String, Long> applicationsByStatus; // status -> count
        private List<TopPerformingJobDto> topPerformingJobs;
        private LocalDateTime generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPerformingJobDto {
        private Long jobId;
        private String jobTitle;
        private String recruiterName;
        private Long applicationCount;
        private Long viewCount;
        private Long hireCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueReportDto {
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal totalRevenue;
        private BigDecimal subscriptionRevenue;
        private BigDecimal boostRevenue;
        private BigDecimal featuredListingRevenue;
        private BigDecimal otherRevenue;
        private Long totalTransactions;
        private Long successfulTransactions;
        private Long failedTransactions;
        private Long refundedTransactions;
        private BigDecimal refundAmount;
        private Map<String, BigDecimal> dailyRevenue; // date -> amount
        private Map<String, BigDecimal> revenueByPlan; // planName -> amount
        private Map<String, Long> subscriptionsByPlan; // planName -> count
        private List<TopPayingUserDto> topPayingUsers;
        private LocalDateTime generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPayingUserDto {
        private Long userId;
        private String userName;
        private String email;
        private String userType;
        private BigDecimal totalSpent;
        private Integer transactionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityReportDto {
        private LocalDate startDate;
        private LocalDate endDate;
        private Long totalLogins;
        private Long uniqueLogins;
        private Long totalProfileViews;
        private Long totalJobViews;
        private Long totalSearches;
        private Long totalMessagesExchanged;
        private Long totalFileUploads;
        private Double averageSessionDuration;
        private Map<String, Long> dailyActiveUsers; // date -> count
        private Map<String, Long> activityByHour; // hour -> count
        private Map<String, Long> loginsByDevice; // device -> count
        private Map<String, Long> loginsByPlatform; // platform -> count
        private LocalDateTime generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditionReportDto {
        private LocalDate startDate;
        private LocalDate endDate;
        private Long totalAuditionsScheduled;
        private Long completedAuditions;
        private Long cancelledAuditions;
        private Long noShowAuditions;
        private Long pendingAuditions;
        private Double completionRate;
        private Double cancellationRate;
        private Map<String, Long> auditionsByStatus; // status -> count
        private Map<String, Long> auditionsByType; // type -> count
        private Map<String, Long> dailyAuditions; // date -> count
        private LocalDateTime generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CastingCallReportDto {
        private LocalDate startDate;
        private LocalDate endDate;
        private Long totalCastingCalls;
        private Long activeCastingCalls;
        private Long closedCastingCalls;
        private Long totalRolesPosted;
        private Long totalApplicationsReceived;
        private Long totalCandidatesShortlisted;
        private Long totalCandidatesSelected;
        private Map<String, Long> castingCallsByCategory; // category -> count
        private Map<String, Long> dailyCastingCalls; // date -> count
        private LocalDateTime generatedAt;
    }
}

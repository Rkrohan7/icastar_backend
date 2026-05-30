package com.icastar.platform.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminDashboardDto {

    // User Statistics
    private Long totalUsers;
    private Long totalArtists;
    private Long totalRecruiters;
    private Long totalAdmins;
    private Long activeUsers;
    private Long inactiveUsers;
    private Long suspendedUsers;
    private Long bannedUsers;
    private Long verifiedUsers;
    private Long unverifiedUsers;
    private Long newUsersToday;
    private Long newUsersThisWeek;
    private Long newUsersThisMonth;

    // Job Statistics
    private Long totalJobs;
    private Long activeJobs;
    private Long closedJobs;
    private Long draftJobs;
    private Long expiredJobs;
    private Long featuredJobs;
    private Long newJobsToday;
    private Long newJobsThisWeek;
    private Long newJobsThisMonth;

    // Application Statistics
    private Long totalApplications;
    private Long pendingApplications;
    private Long acceptedApplications;
    private Long rejectedApplications;
    private Long shortlistedApplications;
    private Long newApplicationsToday;
    private Long newApplicationsThisWeek;

    // Audition Statistics
    private Long totalAuditions;
    private Long scheduledAuditions;
    private Long completedAuditions;
    private Long cancelledAuditions;

    // Hire Request Statistics
    private Long totalHireRequests;
    private Long pendingHireRequests;
    private Long acceptedHireRequests;
    private Long rejectedHireRequests;

    // Casting Call Statistics
    private Long totalCastingCalls;
    private Long activeCastingCalls;
    private Long closedCastingCalls;

    // Platform Activity
    private Long totalProfileViews;
    private Long totalJobViews;
    private Long totalSearches;

    // Top Performers
    private List<TopRecruiterDto> topRecruiters;
    private List<TopArtistDto> topArtists;
    private List<TopJobDto> topJobs;

    // Recent Activity
    private List<RecentActivityDto> recentActivities;

    // Trends (for charts)
    private Map<String, Long> userRegistrationTrend; // date -> count
    private Map<String, Long> jobPostingTrend; // date -> count
    private Map<String, Long> applicationTrend; // date -> count

    // Artist Type Distribution
    private Map<String, Long> artistTypeDistribution; // artistType -> count

    // Job Type Distribution
    private Map<String, Long> jobTypeDistribution; // jobType -> count

    // Location Distribution
    private Map<String, Long> locationDistribution; // city -> count

    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopRecruiterDto {
        private Long id;
        private String name;
        private String companyName;
        private String email;
        private Long totalJobsPosted;
        private Long totalHires;
        private String profileImage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopArtistDto {
        private Long id;
        private String name;
        private String artistType;
        private String email;
        private Long totalApplications;
        private Long totalHires;
        private Long profileViews;
        private String profileImage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopJobDto {
        private Long id;
        private String title;
        private String recruiterName;
        private Long applicationCount;
        private Long viewCount;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityDto {
        private String activityType; // USER_REGISTERED, JOB_POSTED, APPLICATION_SUBMITTED, etc.
        private String description;
        private String userName;
        private Long userId;
        private LocalDateTime timestamp;
    }
}
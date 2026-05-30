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
public class AllArtistsResponseDto {

    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private String stageName;
    private String artistTypeName;
    private Long artistTypeId;
    private String gender;
    private String dateOfBirth;
    private Integer age;
    private String location;
    private String city;
    private String state;
    private String country;
    private String bio;
    private String profileImage;
    private String coverImage;

    // Physical Attributes (if applicable)
    private String height;
    private String weight;
    private String bodyType;
    private String hairColor;
    private String eyeColor;
    private String skinTone;

    // Skills & Experience
    private List<String> skills;
    private List<String> languages;
    private String experienceLevel;
    private Integer yearsOfExperience;

    // Verification Status
    private Boolean isVerified;
    private Boolean isEmailVerified;
    private Boolean isMobileVerified;
    private Boolean isProfileComplete;

    // Account Status
    private String accountStatus; // ACTIVE, INACTIVE, SUSPENDED, BANNED
    private Boolean isActive;
    private Boolean isOnboardingComplete;
    private Boolean isAvailableForWork;

    // Statistics
    private Long totalApplications;
    private Long pendingApplications;
    private Long acceptedApplications;
    private Long rejectedApplications;
    private Long totalAuditions;
    private Long completedAuditions;
    private Long totalHireRequests;
    private Long acceptedHireRequests;
    private Long profileViews;
    private Long bookmarkedCount;

    // Subscription Info
    private String subscriptionPlan;
    private String subscriptionStatus;
    private LocalDateTime subscriptionExpiresAt;

    // Portfolio
    private List<String> portfolioImages;
    private List<String> portfolioVideos;
    private String demoReel;

    // Social Links
    private String instagram;
    private String youtube;
    private String linkedIn;
    private String imdb;
    private String website;

    // Dynamic Fields (type-specific)
    private Map<String, Object> customFields;

    // Dates
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastActivityAt;

    // Recent Applications
    private List<RecentApplicationDto> recentApplications;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentApplicationDto {
        private Long id;
        private String jobTitle;
        private String recruiterName;
        private String status;
        private LocalDateTime appliedAt;
    }
}

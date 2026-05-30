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
public class SystemConfigDto {

    // General Settings
    private String platformName;
    private String platformEmail;
    private String supportEmail;
    private String supportPhone;
    private String platformUrl;
    private String logoUrl;
    private String faviconUrl;
    private Boolean maintenanceMode;
    private String maintenanceMessage;

    // Registration Settings
    private Boolean allowNewRegistrations;
    private Boolean requireEmailVerification;
    private Boolean requireMobileVerification;
    private Boolean requireProfileApproval;
    private Integer otpExpirationMinutes;
    private Integer otpLength;
    private Integer maxLoginAttempts;
    private Integer accountLockDurationMinutes;

    // Job Settings
    private Integer maxJobsPerRecruiter;
    private Integer jobExpirationDays;
    private Integer maxApplicationsPerArtist;
    private Boolean allowJobBoost;
    private Boolean allowFeaturedJobs;
    private Integer maxActiveJobsPerRecruiter;

    // File Upload Settings
    private Long maxFileSize; // in bytes
    private Long maxImageSize;
    private Long maxVideoSize;
    private List<String> allowedImageTypes;
    private List<String> allowedVideoTypes;
    private List<String> allowedDocumentTypes;
    private Integer maxPortfolioImages;
    private Integer maxPortfolioVideos;

    // Subscription Settings
    private Boolean subscriptionEnabled;
    private Integer freeTrialDays;
    private Boolean autoRenewEnabled;
    private Integer gracePeriodDays;

    // Notification Settings
    private Boolean emailNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Boolean pushNotificationsEnabled;
    private Boolean inAppNotificationsEnabled;

    // Communication Settings
    private Integer maxMessagesPerDay;
    private Boolean allowFileSharing;
    private Boolean chatEnabled;
    private Boolean videoCallEnabled;

    // Rate Limiting
    private Integer apiRateLimitPerMinute;
    private Integer searchRateLimitPerMinute;
    private Integer uploadRateLimitPerHour;

    // Feature Flags
    private Map<String, Boolean> featureFlags;

    // Third Party Integrations
    private IntegrationConfigDto razorpayConfig;
    private IntegrationConfigDto stripeConfig;
    private IntegrationConfigDto awsConfig;
    private IntegrationConfigDto firebaseConfig;
    private IntegrationConfigDto smtpConfig;
    private IntegrationConfigDto smsConfig;

    // SEO Settings
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private String googleAnalyticsId;
    private String facebookPixelId;

    // Social Links
    private String facebookUrl;
    private String twitterUrl;
    private String instagramUrl;
    private String linkedInUrl;
    private String youtubeUrl;

    private LocalDateTime lastUpdated;
    private String lastUpdatedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntegrationConfigDto {
        private String name;
        private Boolean enabled;
        private Boolean configured;
        private String status; // ACTIVE, INACTIVE, ERROR
        private String lastError;
        private LocalDateTime lastChecked;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateConfigRequest {
        private String key;
        private String value;
        private String category;
    }
}

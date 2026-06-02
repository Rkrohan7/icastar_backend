package com.icastar.platform.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminArtistPortfolioDto {

    private Long id;
    private Long userId;

    // Basic Info
    private String firstName;
    private String lastName;
    private String stageName;
    private String email;
    private String mobile;
    private String bio;
    private LocalDate dateOfBirth;
    private String gender;
    private String location;
    private String maritalStatus;

    // Artist Type
    private Long artistTypeId;
    private String artistTypeName;

    // Physical Attributes
    private Double weight;
    private Double height;
    private String hairColor;
    private String hairLength;
    private Boolean hasTattoo;
    private Boolean hasMole;
    private String shoeSize;
    private String eyeColor;
    private String complexion;

    // Experience & Skills
    private Integer experienceYears;
    private List<String> skills;
    private List<String> languagesSpoken;
    private List<String> comfortableAreas;

    // Portfolio Media
    private String profileUrl;
    private String coverPhotoUrl;
    private String photoUrl;
    private String videoUrl;
    private String danceShowreelUrl;
    private List<String> portfolioUrls;

    // Projects
    private List<ProjectDto> projectsWorked;

    // Documents
    private String idProofUrl;
    private String faceVerificationUrl;
    private Boolean idProofVerified;
    private LocalDate idProofUploadedAt;

    // Travel & Availability
    private Boolean hasPassport;
    private List<String> travelCities;
    private Double hourlyRate;

    // Verification
    private Boolean isVerifiedBadge;
    private LocalDate verificationRequestedAt;
    private LocalDate verificationApprovedAt;

    // Stats
    private Integer totalApplications;
    private Integer successfulHires;
    private Boolean isProfileComplete;

    // Account Info
    private String accountStatus;
    private Boolean isActive;
    private Boolean isOnboardingComplete;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectDto {
        private String name;
        private String url;
        private String description;
    }
}
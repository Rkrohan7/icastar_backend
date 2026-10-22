package com.icastar.platform.dto.recruiter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistSuggestionDto {
    
    private Long artistId;
    private String artistName;
    private String artistEmail;
    private String artistCategory;
    private String artistType;
    private String location;
    private String bio;
    private String profilePhoto;
    
    // Match Score and Reasons
    private Double matchScore;
    private List<String> matchReasons;
    private List<String> skills;
    private List<String> genres;
    private List<String> languages;
    
    // Experience and Portfolio
    private Integer experienceYears;
    private String experienceLevel;
    private List<String> portfolioItems;
    private List<String> achievements;
    private List<String> certifications;
    
    // Availability and Preferences
    private String availability;
    private String preferredJobType;
    private BigDecimal expectedSalaryMin;
    private BigDecimal expectedSalaryMax;
    private String currency;
    private String workLocation;
    private String workSchedule;
    
    // Contact and Social
    private String phone;
    private String website;
    private List<String> socialLinks;
    private String contactPreference;
    
    // Platform Activity
    private LocalDateTime lastActive;
    private Integer totalApplications;
    private Integer totalHires;
    private Double hireRate;
    private String verificationStatus;
    private Boolean isVerified;
    private String faceVerificationStatus;
    private Boolean isFaceVerified;
    private Boolean idProofVerified;
    private String idProofVerificationStatus;
    private Boolean isPremium;

    // Profile Completeness
    private Integer profileCompletionPercentage; // 0-100

    // Physical Attributes
    private Double hourlyRate;
    private Double weight;
    private Double height;
    private String hairColor;
    private String hairLength;
    private String eyeColor;
    private String complexion;
    private Boolean hasTattoo;
    private Boolean hasMole;
    private String shoeSize;
    private Boolean hasPassport;
    private String gender;
    private String maritalStatus;
    private String dateOfBirth;

    // Additional Fields
    private List<String> comfortableAreas;
    private List<String> travelCities;
    private List<String> portfolioUrls;
    private String videoUrl;
    private String coverPhotoUrl;
    private List<String> projectsWorked;

    // Quick Actions
    private Boolean canViewProfile;
    private Boolean canContact;
    private Boolean canShortlist;
    private Boolean canInvite;
    private Boolean canMessage;
}

package com.icastar.platform.dto.artist;

import com.icastar.platform.dto.ArtistProfileFieldDto;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.Document;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArtistProfileCompleteDto {
    
    // User fields
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String city;
    private Boolean isActive;
    private Boolean isOnboardingComplete;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Artist Profile fields
    private Long artistProfileId;

    // Primary artist type (backward compatibility)
    private Long artistTypeId;
    private String artistTypeName;
    private String artistTypeDisplayName;

    // All artist types for this profile (new field for multiple professions)
    // First element is always the primary type
    private List<ArtistTypeDto> artistTypes;

    // List of artist type IDs for update operations
    private List<Long> artistTypeIds;

    private String stageName;
    private String bio;
    private LocalDate dateOfBirth;
    private ArtistProfile.Gender gender;
    private String location;
    private ArtistProfile.MaritalStatus maritalStatus;
    private String languagesSpoken; // JSON string
    private String comfortableAreas; // JSON string
    private String projectsWorked; // JSON string
    private String skills; // JSON string
    private Integer experienceYears;
    private Double weight;
    private Double height;
    private String hairColor;
    private String hairLength;
    private Boolean hasTattoo;
    private Boolean hasMole;
    private String shoeSize;
    private String eyeColor;
    private String complexion;
    private Boolean hasPassport;
    private String travelCities; // JSON string
    private Double hourlyRate;
    private String photoUrl; // S3 URL for portfolio photo
    private String videoUrl; // S3 URL for portfolio video
    private String portfolioUrls; // JSON string of portfolio URLs
    private String profileUrl; // S3 URL for profile image
    private String coverPhotoUrl; // S3 URL for cover photo
    private String idProofUrl; // S3 URL for ID proof document
    private String faceVerificationUrl; // S3 URL for face verification image
    private Boolean idProofVerified;
    private LocalDate idProofUploadedAt;
    private Boolean isVerifiedBadge;
    private LocalDate verificationRequestedAt;
    private LocalDate verificationApprovedAt;
    private Integer totalApplications;
    private Integer successfulHires;
    private Boolean isProfileComplete;
    
    // Documents
    private List<DocumentDto> documents;
    
    // Dynamic fields
    private List<ArtistProfileFieldDto> dynamicFields;
    
    @Data
    public static class DocumentDto {
        private Long id;
        private Document.DocumentType documentType;
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private String mimeType;
        private LocalDateTime uploadedAt;
        private Boolean isVerified;
        private LocalDateTime verifiedAt;
        private Long verifiedBy;
        private String verificationNotes;
    }
}

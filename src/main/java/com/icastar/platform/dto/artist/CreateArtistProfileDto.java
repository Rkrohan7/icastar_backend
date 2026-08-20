package com.icastar.platform.dto.artist;

import com.icastar.platform.dto.ArtistProfileFieldDto;
import com.icastar.platform.entity.ArtistProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateArtistProfileDto {

    // Primary artist type ID (backward compatibility)
    // If artistTypeIds is provided, this will be set to the first element
    private Long artistTypeId;

    // List of artist type IDs - first one is primary
    // At least one artist type is required
    private List<Long> artistTypeIds;
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @Size(max = 100, message = "Stage name must not exceed 100 characters")
    private String stageName;
    
    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;
    
    private LocalDate dateOfBirth;
    
    private ArtistProfile.Gender gender;
    
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;
    
    private List<String> skills;
    
    private Integer experienceYears;
    
    private Double hourlyRate;

    // Portfolio URLs (S3)
    private String photoUrl; // S3 URL for artist's portfolio photo
    private String videoUrl; // S3 URL for artist's portfolio video
    private String profileUrl; // S3 URL for artist's profile image
    private List<String> portfolioUrls; // List of portfolio URLs

    private List<ArtistProfileFieldDto> dynamicFields;
    
    // Actor-specific fields
    private String city;
    private ArtistProfile.MaritalStatus maritalStatus;
    private String phone;
    private List<String> languagesSpoken;
    // Document URLs are now handled by Document entity, not stored directly here
    private List<String> comfortableAreas;
    private List<ProjectDto> projectsWorked;
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
    private List<String> travelCities;
    
    // Nested DTOs
    @Data
    public static class ProfilePictureDto {
        private String url; // S3 URL for profile picture
        private ProfilePictureType type;
        
        public enum ProfilePictureType {
            LEFT_PROFILE, RIGHT_PROFILE, FRONT_PROFILE
        }
    }
    
    @Data
    public static class ProjectDto {
        private String name;
        private String url; // S3 URL for project reference
    }
}

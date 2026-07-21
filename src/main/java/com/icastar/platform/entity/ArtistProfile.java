package com.icastar.platform.entity;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "artist_profiles")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"jobApplications", "bookmarkedJobs", "dynamicFields"})
@ToString(exclude = {"jobApplications", "bookmarkedJobs", "dynamicFields"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArtistProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_type_id", nullable = false)
    private ArtistType artistType;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "stage_name")
    private String stageName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "location")
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    private MaritalStatus maritalStatus;

    @Column(name = "languages_spoken", columnDefinition = "JSON")
    private String languagesSpoken; // JSON array of languages

    @Column(name = "comfortable_areas", columnDefinition = "JSON")
    private String comfortableAreas; // JSON array of comfortable shooting areas

    @Column(name = "projects_worked", columnDefinition = "JSON")
    private String projectsWorked; // JSON array of projects with names and URLs

    @Column(name = "skills", columnDefinition = "JSON")
    private String skills; // JSON array of skills

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "height")
    private Double height;

    @Column(name = "hair_color")
    private String hairColor;

    @Column(name = "hair_length")
    private String hairLength;

    @Column(name = "has_tattoo")
    private Boolean hasTattoo = false;

    @Column(name = "has_mole")
    private Boolean hasMole = false;

    @Column(name = "shoe_size")
    private String shoeSize;

    @Column(name = "eye_color")
    private String eyeColor;

    @Column(name = "complexion")
    private String complexion;

    @Column(name = "has_passport")
    private Boolean hasPassport = false;

    @Column(name = "travel_cities", columnDefinition = "JSON")
    private String travelCities; // JSON array of cities where can travel for shoots

    @Column(name = "hourly_rate")
    private Double hourlyRate;

    @Column(name = "photo_url")
    private String photoUrl; // S3 URL for artist's portfolio photo

    @Column(name = "video_url")
    private String videoUrl; // S3 URL for artist's portfolio video

    @Column(name = "portfolio_urls", columnDefinition = "JSON")
    private String portfolioUrls; // JSON array of portfolio URLs

    @Column(name = "dance_showreel_url")
    private String danceShowreelUrl; // S3 URL for artist's dance showreel video

    @Column(name = "profile_url")
    private String profileUrl; // S3 URL for artist's profile image

    @Column(name = "cover_photo_url")
    private String coverPhotoUrl; // S3 URL for artist's cover photo

    @Column(name = "id_proof_url")
    private String idProofUrl; // S3 URL for artist's ID proof document

    @Column(name = "face_verification_url")
    private String faceVerificationUrl; // S3 URL for artist's face verification image

    @Column(name = "id_proof_verified", nullable = false)
    private Boolean idProofVerified = false;

    @Column(name = "id_proof_uploaded_at")
    private LocalDate idProofUploadedAt;

    @Column(name = "is_verified_badge", nullable = false)
    private Boolean isVerifiedBadge = false;

    @Column(name = "verification_requested_at")
    private LocalDate verificationRequestedAt;

    @Column(name = "verification_approved_at")
    private LocalDate verificationApprovedAt;

    @Column(name = "total_applications")
    private Integer totalApplications = 0;

    @Column(name = "successful_hires")
    private Integer successfulHires = 0;

    @Column(name = "is_profile_complete", nullable = false)
    private Boolean isProfileComplete = false;

    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<JobApplication> jobApplications;

    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<BookmarkedJob> bookmarkedJobs;

    // Messages are handled at User level, not ArtistProfile level
    // @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Message> sentMessages;

    // @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Message> receivedMessages;

    // Dynamic fields for artist type specific data
    @OneToMany(mappedBy = "artistProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ArtistProfileField> dynamicFields;

    // Documents are linked through User entity, not directly to ArtistProfile
    // @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Document> documents;

    public enum Gender {
        MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    }

    public enum MaritalStatus {
        SINGLE, MARRIED, DIVORCED, WIDOWED, PREFER_NOT_TO_SAY
    }
}

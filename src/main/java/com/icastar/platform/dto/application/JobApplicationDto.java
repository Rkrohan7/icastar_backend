package com.icastar.platform.dto.application;

import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.JobApplication;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class JobApplicationDto {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long artistId;
    private String artistName;
    private String coverLetter;
    private BigDecimal proposedRate;
    private JobApplication.ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    // Artist profile information
    private String artistBio;
    private String artistLocation;
    private String artistExperience;
    private Boolean isArtistVerified;
    private Integer artistProfileCompletionPercentage; // 0-100

    // Job information
    private String jobDescription;
    private String jobLocation;
    private Boolean isJobRemote;
    private BigDecimal jobBudgetMin;
    private BigDecimal jobBudgetMax;

    // Recruiter information
    private Long recruiterId;
    private String recruiterName;
    private String companyName;

    // Guest applicant information (for PUBLIC_LINK applications)
    private String guestFullName;
    private String guestEmail;
    private String guestPhone;
    private String guestAddress;
    private Integer guestExperienceYears;
    private String applicationSource; // PLATFORM or PUBLIC_LINK
    private Boolean isGuestApplication;

    public JobApplicationDto() {
        // Default constructor
    }

    public JobApplicationDto(JobApplication application) {
        this.id = application.getId();
        this.coverLetter = application.getCoverLetter();
        this.proposedRate = application.getExpectedSalary() != null ?
            BigDecimal.valueOf(application.getExpectedSalary()) : null;
        this.status = application.getStatus();
        this.appliedAt = application.getAppliedAt();
        this.updatedAt = application.getUpdatedAt();

        // Set application source
        this.applicationSource = application.getSource() != null ?
            application.getSource().name() : "PLATFORM";
        this.isGuestApplication = application.getArtist() == null;

        // Handle job information
        if (application.getJob() != null) {
            this.jobId = application.getJob().getId();
            this.jobTitle = application.getJob().getTitle();
            this.jobDescription = application.getJob().getDescription();
            this.jobLocation = application.getJob().getLocation();
            this.isJobRemote = application.getJob().getIsRemote();
            this.jobBudgetMin = application.getJob().getBudgetMin();
            this.jobBudgetMax = application.getJob().getBudgetMax();

            if (application.getJob().getRecruiter() != null) {
                this.recruiterId = application.getJob().getRecruiter().getId();
                this.recruiterName = application.getJob().getRecruiter().getFirstName() + " " +
                                   application.getJob().getRecruiter().getLastName();
                if (application.getJob().getRecruiter().getRecruiterProfile() != null) {
                    this.companyName = application.getJob().getRecruiter().getRecruiterProfile().getCompanyName();
                }
            }
        }

        // Handle artist information (logged-in user application)
        if (application.getArtist() != null) {
            this.artistId = application.getArtist().getId();
            this.artistName = application.getArtist().getFirstName() + " " +
                            application.getArtist().getLastName();
            this.artistBio = application.getArtist().getBio();
            this.artistLocation = application.getArtist().getLocation();
            this.artistExperience = application.getArtist().getExperienceYears() != null ?
                application.getArtist().getExperienceYears().toString() + " years" : "Not specified";
            this.isArtistVerified = application.getArtist().getUser() != null ?
                application.getArtist().getUser().getIsVerified() : false;
            this.artistProfileCompletionPercentage = calculateProfileCompletion(application.getArtist());
        } else {
            // Handle guest applicant information (PUBLIC_LINK application)
            this.guestFullName = application.getGuestFullName();
            this.guestEmail = application.getGuestEmail();
            this.guestPhone = application.getGuestPhone();
            this.guestAddress = application.getGuestAddress();
            this.guestExperienceYears = application.getGuestExperienceYears();

            // Use guest info for artist fields for backward compatibility
            this.artistName = application.getGuestFullName();
            this.artistLocation = application.getGuestAddress();
            this.artistExperience = application.getGuestExperienceYears() != null ?
                application.getGuestExperienceYears().toString() + " years" : "Not specified";
            this.isArtistVerified = false;
            this.artistProfileCompletionPercentage = 0;
        }
    }

    /**
     * Calculate profile completion percentage for an artist profile
     * This uses the same logic as ArtistService.calculateProfileCompletionPercentage
     */
    private int calculateProfileCompletion(ArtistProfile profile) {
        if (profile == null) {
            return 0;
        }

        int completedFields = 0;
        int totalFields = 11; // Total number of fields to check

        // Check required fields
        if (profile.getFirstName() != null && !profile.getFirstName().trim().isEmpty()) completedFields++;
        if (profile.getLastName() != null && !profile.getLastName().trim().isEmpty()) completedFields++;
        if (profile.getStageName() != null && !profile.getStageName().trim().isEmpty()) completedFields++;
        if (profile.getBio() != null && !profile.getBio().trim().isEmpty()) completedFields++;
        if (profile.getLocation() != null && !profile.getLocation().trim().isEmpty()) completedFields++;
        if (profile.getSkills() != null && !profile.getSkills().trim().isEmpty()) completedFields++;
        if (profile.getExperienceYears() != null) completedFields++;
        if (profile.getHourlyRate() != null) completedFields++;
        if (profile.getWeight() != null) completedFields++;
        if (profile.getHeight() != null) completedFields++;
        if (profile.getLanguagesSpoken() != null && !profile.getLanguagesSpoken().trim().isEmpty()) completedFields++;

        return totalFields > 0 ? (completedFields * 100) / totalFields : 0;
    }
}
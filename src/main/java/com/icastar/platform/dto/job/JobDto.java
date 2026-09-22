package com.icastar.platform.dto.job;

import com.icastar.platform.entity.CastingCharacter;
import com.icastar.platform.entity.CastingProject;
import com.icastar.platform.entity.Job;
import com.icastar.platform.entity.JobApplication;
import com.icastar.platform.dto.casting.SelectedArtistDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class JobDto {

    private Long id;
    private Long recruiterId;
    private String recruiterName;
    private String recruiterEmail;
    private String companyName;
    private String companyWebsite;
    private String companyLogoUrl;
    private String title;
    private String description;
    private String requirements;
    private String location;
    private Job.JobType jobType;
    private Job.ExperienceLevel experienceLevel;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String currency;
    private Integer durationDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate applicationDeadline;
    private Boolean isRemote;
    private Boolean isUrgent;
    private Boolean isFeatured;
    private Job.JobStatus status;
    private Integer viewsCount;
    private Integer applicationsCount;
    private List<String> tags;
    private List<String> skillsRequired;
    private String benefits;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime publishedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields for candidate view
    private Boolean isBookmarked;
    private Boolean hasApplied;
    private Long applicationId;
    private JobApplicationDto applicationStatus;

    // Casting project fields
    private Long projectId;
    private String projectName;
    private CastingProject.ProjectType projectType;
    private Long characterId;
    private String characterName;
    private CastingCharacter.RoleType roleType;

    // Selected artists (hired applicants)
    private List<SelectedArtistDto> selectedArtists;

    // Default constructor
    public JobDto() {}

    // Constructor that takes a Job entity
    public JobDto(Job job) {
        this.id = job.getId();
        this.recruiterId = job.getRecruiter().getId();
        this.recruiterName = job.getRecruiter().getFirstName() + " " + job.getRecruiter().getLastName();
        this.recruiterEmail = job.getRecruiter().getEmail();

        // Get company details from recruiter profile
        if (job.getRecruiter().getRecruiterProfile() != null) {
            this.companyName = job.getRecruiter().getRecruiterProfile().getCompanyName();
            this.companyWebsite = job.getRecruiter().getRecruiterProfile().getCompanyWebsite();
            this.companyLogoUrl = job.getRecruiter().getRecruiterProfile().getCompanyLogoUrl();
        }

        this.title = job.getTitle();
        this.description = job.getDescription();
        this.requirements = job.getRequirements();
        this.location = job.getLocation();
        this.jobType = job.getJobType();
        this.experienceLevel = job.getExperienceLevel();
        this.budgetMin = job.getBudgetMin();
        this.budgetMax = job.getBudgetMax();
        this.currency = job.getCurrency();
        this.durationDays = job.getDurationDays();
        this.startDate = job.getStartDate();
        this.endDate = job.getEndDate();
        this.applicationDeadline = job.getApplicationDeadline();
        this.isRemote = job.getIsRemote();
        this.isUrgent = job.getIsUrgent();
        this.isFeatured = job.getIsFeatured();
        this.status = job.getStatus();
        this.viewsCount = job.getViewsCount();
        this.applicationsCount = job.getApplicationsCount();
        // Parse JSON tags string to List<String> if not null
        if (job.getTags() != null && !job.getTags().isEmpty()) {
            try {
                // Simple JSON array parsing - assuming format like ["tag1","tag2"]
                String tagsStr = job.getTags().trim();
                if (tagsStr.startsWith("[") && tagsStr.endsWith("]")) {
                    tagsStr = tagsStr.substring(1, tagsStr.length() - 1);
                    this.tags = java.util.Arrays.asList(tagsStr.split(","));
                } else {
                    this.tags = java.util.Arrays.asList(tagsStr.split(","));
                }
            } catch (Exception e) {
                this.tags = java.util.Arrays.asList(job.getTags());
            }
        } else {
            this.tags = new java.util.ArrayList<>();
        }
        // Parse JSON skillsRequired string to List<String> if not null
        if (job.getSkillsRequired() != null && !job.getSkillsRequired().isEmpty()) {
            try {
                // Simple JSON array parsing - assuming format like ["skill1","skill2"]
                String skillsStr = job.getSkillsRequired().trim();
                if (skillsStr.startsWith("[") && skillsStr.endsWith("]")) {
                    skillsStr = skillsStr.substring(1, skillsStr.length() - 1);
                    this.skillsRequired = java.util.Arrays.asList(skillsStr.split(","));
                } else {
                    this.skillsRequired = java.util.Arrays.asList(skillsStr.split(","));
                }
            } catch (Exception e) {
                this.skillsRequired = java.util.Arrays.asList(job.getSkillsRequired());
            }
        } else {
            this.skillsRequired = new java.util.ArrayList<>();
        }
        this.benefits = job.getBenefits();
        this.contactEmail = job.getContactEmail();
        this.contactPhone = job.getContactPhone();
        this.publishedAt = job.getPublishedAt();
        this.closedAt = job.getClosedAt();
        this.createdAt = job.getCreatedAt();
        this.updatedAt = job.getUpdatedAt();

        // Casting project fields
        if (job.getProject() != null) {
            this.projectId = job.getProject().getId();
            this.projectName = job.getProject().getName();
            this.projectType = job.getProject().getProjectType();
        }

        // Casting character fields
        if (job.getCharacter() != null) {
            this.characterId = job.getCharacter().getId();
            this.characterName = job.getCharacter().getName();
        }

        // Role type
        this.roleType = job.getRoleType();

        // Selected artists (hired applicants)
        if (job.getApplications() != null) {
            this.selectedArtists = job.getApplications().stream()
                    .filter(app -> app.getStatus() == JobApplication.ApplicationStatus.HIRED)
                    .map(SelectedArtistDto::new)
                    .collect(Collectors.toList());
        } else {
            this.selectedArtists = new ArrayList<>();
        }
    }
}

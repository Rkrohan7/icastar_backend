package com.icastar.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"applications", "bookmarkedJobs"})
@ToString(exclude = {"applications", "bookmarkedJobs"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Job extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "location")
    private String location;

    @Column(name = "job_type")
    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Column(name = "experience_level")
    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    @Column(name = "budget_min")
    private BigDecimal budgetMin;

    @Column(name = "budget_max")
    private BigDecimal budgetMax;

    @Column(name = "currency", length = 3)
    private String currency = "INR";

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "is_remote")
    private Boolean isRemote = false;

    @Column(name = "is_urgent")
    private Boolean isUrgent = false;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "views_count")
    private Integer viewsCount = 0;

    @Column(name = "applications_count")
    private Integer applicationsCount = 0;

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags; // JSON array of tags

    @Column(name = "skills_required", columnDefinition = "JSON")
    private String skillsRequired; // JSON array of required skills

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<JobApplication> applications;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookmarkedJob> bookmarkedJobs;

    public enum JobType {
        FULL_TIME, PART_TIME, CONTRACT, FREELANCE, INTERNSHIP, PROJECT_BASED
    }

    public enum ExperienceLevel {
        ENTRY_LEVEL, MID_LEVEL, SENIOR_LEVEL, EXPERT_LEVEL
    }

    public enum JobStatus {
        DRAFT, PENDING_APPROVAL, ACTIVE, INACTIVE, PAUSED, CLOSED, CANCELLED, REJECTED, DELETED
    }
}

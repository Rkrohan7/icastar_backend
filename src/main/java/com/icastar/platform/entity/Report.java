package com.icastar.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Column(name = "entity_type")
    private String entityType; // JOB, ARTIST_PROFILE, RECRUITER_PROFILE, AUDITION, etc.

    @Column(name = "entity_id")
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ReportReason reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_urls", columnDefinition = "JSON")
    private String evidenceUrls; // JSON array of evidence URLs

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    private ReportPriority priority = ReportPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken")
    private ActionTaken actionTaken;

    public enum ReportType {
        USER, CONTENT, JOB, AUDITION, MESSAGE, PROFILE
    }

    public enum ReportReason {
        SPAM,
        HARASSMENT,
        INAPPROPRIATE_CONTENT,
        FAKE_PROFILE,
        FRAUD,
        MISLEADING_INFO,
        COPYRIGHT_VIOLATION,
        HATE_SPEECH,
        VIOLENCE,
        IMPERSONATION,
        OTHER
    }

    public enum ReportStatus {
        PENDING, UNDER_REVIEW, RESOLVED, DISMISSED, ESCALATED
    }

    public enum ReportPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum ActionTaken {
        NONE,
        WARNING_ISSUED,
        CONTENT_REMOVED,
        ACCOUNT_SUSPENDED,
        ACCOUNT_BANNED,
        REFERRED_TO_AUTHORITIES
    }
}

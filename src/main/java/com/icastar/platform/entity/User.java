package com.icastar.platform.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "mobile", unique = true, nullable = false)
    private String mobile;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "is_verified", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isVerified = false;

    @Column(name = "is_onboarding_complete", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isOnboardingComplete = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    // Account Management Fields
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deactivated_by")
    private User deactivatedBy;

    @Column(name = "deactivation_reason", columnDefinition = "TEXT")
    private String deactivationReason;

    @Column(name = "reactivated_at")
    private LocalDateTime reactivatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reactivated_by")
    private User reactivatedBy;

    @Column(name = "reactivation_reason", columnDefinition = "TEXT")
    private String reactivationReason;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "login_attempts")
    private Integer loginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private ArtistProfile artistProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private RecruiterProfile recruiterProfile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Subscription> subscriptions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<AuditLog> auditLogs;

    public enum UserRole {
        ADMIN, ARTIST, RECRUITER
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, BANNED, SUSPENDED
    }

    public enum AccountStatus {
        ACTIVE, INACTIVE, SUSPENDED, BANNED, PENDING_VERIFICATION
    }
}

package com.icastar.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_verification_challenges")
@Data
public class AuthVerificationChallenge {

    @Id
    @Column(name = "challenge_id", length = 36)
    private String challengeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "intent", nullable = false, length = 16)
    private Intent intent;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "mobile", nullable = false, length = 20)
    private String mobile;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "password_policy_validated", nullable = false)
    private boolean passwordPolicyValidated;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private User.UserRole role;

    @Column(name = "accepted_terms")
    private Boolean acceptedTerms;

    @Column(name = "terms_version", length = 20)
    private String termsVersion;

    @Column(name = "email_otp_hash", nullable = false, length = 100)
    private String emailOtpHash;

    @Column(name = "mobile_otp_hash", length = 100)
    private String mobileOtpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "resend_count", nullable = false)
    private int resendCount;

    @Column(name = "last_resend_at")
    private LocalDateTime lastResendAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Intent {
        REGISTER, LOGIN
    }
}

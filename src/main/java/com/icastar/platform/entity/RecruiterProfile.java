package com.icastar.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Entity
@Table(name = "recruiter_profiles")
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RecruiterProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment primary key
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "contact_person_name", nullable = false)
    private String contactPersonName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "designation")
    private String designation;

    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;

    @Column(name = "company_website")
    private String companyWebsite;

    @Column(name = "company_logo_url")
    private String companyLogoUrl;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "industry")
    private String industry;

    @Column(name = "company_size")
    private String companySize;

    @Column(name = "location")
    private String location;

    @Column(name = "is_verified_company", nullable = false)
    private Boolean isVerifiedCompany = false;

    @Column(name = "total_jobs_posted")
    private Integer totalJobsPosted = 0;

    @Column(name = "successful_hires")
    private Integer successfulHires = 0;

    @Column(name = "chat_credits")
    private Integer chatCredits = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_category_id")
    private RecruiterCategory recruiterCategory;

    // Note: Jobs are accessed through the User entity, not directly from RecruiterProfile
    // This relationship is handled through the User.recruiterProfile relationship
    @Transient
    private List<Job> jobs;

    @OneToMany(mappedBy = "recruiterProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<RecruiterProfileField> dynamicFields;

    // Messages are handled at User level, not RecruiterProfile level
    // @OneToMany(mappedBy = "recruiter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Message> sentMessages;

    // @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Message> receivedMessages;
}

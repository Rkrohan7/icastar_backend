package com.icastar.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "artist_experiences")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"artistProfile", "artistType"})
@ToString(exclude = {"artistProfile", "artistType"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArtistExperience extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    @JsonIgnore
    private ArtistProfile artistProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_type_id")
    private ArtistType artistType;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "project_type", length = 50)
    private String projectType;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 20)
    private EmploymentType employmentType;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = false;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "description", length = 1000)
    private String description;

    public enum EmploymentType {
        FULL_TIME,
        PART_TIME,
        FREELANCE,
        CONTRACT,
        INTERNSHIP
    }
}
package com.icastar.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "casting_projects")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"characters", "jobs"})
@ToString(exclude = {"characters", "jobs"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CastingProject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "project_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectType projectType;

    @Column(name = "production_house")
    private String productionHouse;

    @Column(name = "director")
    private String director;

    @Column(name = "language")
    private String language;

    @Column(name = "shoot_location")
    private String shootLocation;

    @Column(name = "shoot_start_date")
    private LocalDate shootStartDate;

    @Column(name = "shoot_end_date")
    private LocalDate shootEndDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CastingCharacter> characters;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Job> jobs;

    public enum ProjectType {
        FEATURE_FILM,
        TV_SERIES,
        WEB_SERIES,
        SHORT_FILM,
        COMMERCIAL,
        MUSIC_VIDEO,
        THEATER
    }

    public enum ProjectStatus {
        ACTIVE,
        COMPLETED,
        ON_HOLD
    }
}

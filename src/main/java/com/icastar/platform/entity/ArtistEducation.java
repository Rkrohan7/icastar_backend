package com.icastar.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "artist_educations")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"artistProfile"})
@ToString(exclude = {"artistProfile"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArtistEducation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    @JsonIgnore
    private ArtistProfile artistProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", nullable = false)
    private EducationLevel educationLevel;

    @Column(name = "course_name", nullable = false, length = 150)
    private String courseName;

    @Column(name = "specialization", length = 100)
    private String specialization;

    @Column(name = "institution", nullable = false, length = 150)
    private String institution;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", length = 20)
    private CourseType courseType;

    @Column(name = "is_pursuing", nullable = false)
    private Boolean isPursuing = false;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "end_year")
    private Integer endYear;

    @Column(name = "grade", length = 30)
    private String grade;

    @Column(name = "description", length = 500)
    private String description;

    public enum EducationLevel {
        SCHOOL_10TH,
        HIGHER_SECONDARY_12TH,
        DIPLOMA,
        GRADUATION,
        POST_GRADUATION,
        DOCTORATE,
        CERTIFICATION,
        OTHER
    }

    public enum CourseType {
        FULL_TIME,
        PART_TIME,
        DISTANCE
    }
}
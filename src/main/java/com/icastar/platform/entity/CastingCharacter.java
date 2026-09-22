package com.icastar.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "casting_characters")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"project", "jobs"})
@ToString(exclude = {"project", "jobs"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CastingCharacter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CastingProject project;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "role_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender = Gender.ANY;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "required_count")
    private Integer requiredCount = 1;

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Job> jobs;

    public enum RoleType {
        LEAD,
        SUPPORTING,
        BACKGROUND,
        EXTRA
    }

    public enum Gender {
        MALE,
        FEMALE,
        NON_BINARY,
        ANY
    }
}

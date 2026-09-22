package com.icastar.platform.dto.job;

import com.icastar.platform.entity.CastingCharacter;
import com.icastar.platform.entity.Job;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateJobDto {

    // Casting project fields (optional)
    private Long projectId;
    private Long characterId;
    private CastingCharacter.RoleType roleType;

    private String title;
    private String description;
    private String requirements;
    private Job.JobType jobType;
    private Job.ExperienceLevel experienceLevel;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String currency;
    private Integer durationDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate applicationDeadline;
    private String location;
    private Boolean isRemote;
    private Boolean isUrgent;
    private Boolean isFeatured;
    private String tags; // JSON string
    private String skillsRequired; // JSON string
    private String benefits;
    private String contactEmail;
    private String contactPhone;
    private Job.JobStatus status;
}

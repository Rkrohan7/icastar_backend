package com.icastar.platform.dto.job;

import com.icastar.platform.entity.CastingCharacter;
import com.icastar.platform.entity.Job;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateJobDto {

    // Casting project fields (optional)
    private Long projectId;
    private Long characterId;
    private CastingCharacter.RoleType roleType;

    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private String requirements;
    
    @NotNull(message = "Job type is required")
    private Job.JobType jobType;
    
    @NotNull(message = "Experience level is required")
    private Job.ExperienceLevel experienceLevel;
    
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String currency = "INR";
    private Integer durationDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate applicationDeadline;
    private String location;
    private Boolean isRemote = false;
    private Boolean isUrgent = false;
    private Boolean isFeatured = false;
    private String tags; // JSON string
    private String skillsRequired; // JSON string
    private String benefits;
    private String contactEmail;
    private String contactPhone;
}
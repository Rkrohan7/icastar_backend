package com.icastar.platform.dto.job;

import com.icastar.platform.entity.Job;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DTO for public job details API.
 * This DTO is designed to be safely cached in Redis without Hibernate entities.
 *
 * Contains NO:
 * - Hibernate entities (User, JobApplication, BookmarkedJob)
 * - Lazy proxies
 * - Bidirectional relationships
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicJobDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private String requirements;
    private String responsibilities;
    private String location;
    private Boolean isRemote;
    private String jobType;
    private String experienceLevel;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String currency;
    private Integer durationDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate applicationDeadline;
    private Boolean isUrgent;
    private Boolean isFeatured;
    private String benefits;
    private List<String> skills;
    private String companyName;
    private String companyLogoUrl;
    private String status;

    /**
     * Create PublicJobDto from Job entity.
     * This constructor extracts all required data from the entity and its relationships
     * so that the DTO can be safely cached without any Hibernate proxies.
     */
    public static PublicJobDto fromEntity(Job job) {
        if (job == null) {
            return null;
        }

        PublicJobDto dto = new PublicJobDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setRequirements(job.getRequirements());
        dto.setResponsibilities(job.getRequirements()); // Using requirements as responsibilities
        dto.setLocation(job.getLocation());
        dto.setIsRemote(job.getIsRemote());
        dto.setJobType(job.getJobType() != null ? job.getJobType().name() : null);
        dto.setExperienceLevel(job.getExperienceLevel() != null ? job.getExperienceLevel().name() : null);
        dto.setBudgetMin(job.getBudgetMin());
        dto.setBudgetMax(job.getBudgetMax());
        dto.setCurrency(job.getCurrency() != null ? job.getCurrency() : "INR");
        dto.setDurationDays(job.getDurationDays());
        dto.setStartDate(job.getStartDate());
        dto.setEndDate(job.getEndDate());
        dto.setApplicationDeadline(job.getApplicationDeadline());
        dto.setIsUrgent(job.getIsUrgent());
        dto.setIsFeatured(job.getIsFeatured());
        dto.setBenefits(job.getBenefits());
        dto.setStatus(job.getStatus() != null ? job.getStatus().name() : null);

        // Parse skills from JSON string
        dto.setSkills(parseJsonArray(job.getSkillsRequired()));

        // Extract company info from recruiter profile (accessing lazy relationships HERE, not in cache)
        if (job.getRecruiter() != null && job.getRecruiter().getRecruiterProfile() != null) {
            dto.setCompanyName(job.getRecruiter().getRecruiterProfile().getCompanyName());
            dto.setCompanyLogoUrl(job.getRecruiter().getRecruiterProfile().getCompanyLogoUrl());
        }

        return dto;
    }

    /**
     * Parse JSON array string to List<String>
     */
    private static List<String> parseJsonArray(String jsonArrayStr) {
        if (jsonArrayStr == null || jsonArrayStr.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            String str = jsonArrayStr.trim();
            if (str.startsWith("[") && str.endsWith("]")) {
                str = str.substring(1, str.length() - 1);
            }
            if (str.isEmpty()) {
                return new ArrayList<>();
            }
            String[] parts = str.split(",");
            List<String> result = new ArrayList<>();
            for (String part : parts) {
                String cleaned = part.trim().replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
                if (!cleaned.isEmpty()) {
                    result.add(cleaned);
                }
            }
            return result;
        } catch (Exception e) {
            return Arrays.asList(jsonArrayStr);
        }
    }

    /**
     * Check if the job is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }
}
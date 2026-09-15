package com.icastar.platform.dto.artist;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.icastar.platform.entity.ArtistExperience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceDto {

    private Long id;

    private Long artistTypeId;

    private String artistTypeName;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be at most 100 characters")
    private String title;

    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name must be at most 150 characters")
    private String companyName;

    @Size(max = 50, message = "Project type must be at most 50 characters")
    private String projectType;

    private String employmentType;

    @Size(max = 100, message = "Location must be at most 100 characters")
    private String location;

    @JsonProperty("isCurrent")
    private Boolean isCurrent = false;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    // Helper method to convert entity to DTO
    public static ExperienceDto fromEntity(ArtistExperience entity) {
        ExperienceDto dto = new ExperienceDto();
        dto.setId(entity.getId());
        if (entity.getArtistType() != null) {
            dto.setArtistTypeId(entity.getArtistType().getId());
            // Use displayName for better UI readability (e.g., "Dancer" instead of "DANCER")
            dto.setArtistTypeName(entity.getArtistType().getDisplayName());
        }
        dto.setTitle(entity.getTitle());
        dto.setCompanyName(entity.getCompanyName());
        dto.setProjectType(entity.getProjectType());
        if (entity.getEmploymentType() != null) {
            dto.setEmploymentType(entity.getEmploymentType().name());
        }
        dto.setLocation(entity.getLocation());
        dto.setIsCurrent(entity.getIsCurrent());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setDescription(entity.getDescription());
        return dto;
    }
}
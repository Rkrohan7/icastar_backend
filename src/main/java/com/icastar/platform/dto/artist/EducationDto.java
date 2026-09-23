package com.icastar.platform.dto.artist;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.icastar.platform.entity.ArtistEducation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EducationDto {

    private Long id;

    @NotNull(message = "Education level is required")
    private String educationLevel;

    @NotBlank(message = "Course name is required")
    @Size(max = 150, message = "Course name must be at most 150 characters")
    private String courseName;

    @Size(max = 100, message = "Specialization must be at most 100 characters")
    private String specialization;

    @NotBlank(message = "Institution is required")
    @Size(max = 150, message = "Institution must be at most 150 characters")
    private String institution;

    private String courseType;

    @JsonProperty("isPursuing")
    private Boolean isPursuing = false;

    private Integer startYear;

    private Integer endYear;

    @Size(max = 30, message = "Grade must be at most 30 characters")
    private String grade;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    // Helper method to convert entity to DTO
    public static EducationDto fromEntity(ArtistEducation entity) {
        EducationDto dto = new EducationDto();
        dto.setId(entity.getId());
        if (entity.getEducationLevel() != null) {
            dto.setEducationLevel(entity.getEducationLevel().name());
        }
        dto.setCourseName(entity.getCourseName());
        dto.setSpecialization(entity.getSpecialization());
        dto.setInstitution(entity.getInstitution());
        if (entity.getCourseType() != null) {
            dto.setCourseType(entity.getCourseType().name());
        }
        dto.setIsPursuing(entity.getIsPursuing());
        dto.setStartYear(entity.getStartYear());
        dto.setEndYear(entity.getEndYear());
        dto.setGrade(entity.getGrade());
        dto.setDescription(entity.getDescription());
        return dto;
    }
}
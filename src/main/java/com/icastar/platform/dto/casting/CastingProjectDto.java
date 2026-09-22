package com.icastar.platform.dto.casting;

import com.icastar.platform.entity.CastingProject;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CastingProjectDto {
    private Long id;
    private Long recruiterId;
    private String recruiterName;
    private String name;
    private CastingProject.ProjectType projectType;
    private String productionHouse;
    private String director;
    private String language;
    private String shootLocation;
    private LocalDate shootStartDate;
    private LocalDate shootEndDate;
    private String description;
    private CastingProject.ProjectStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Characters list (optional, included when requested)
    private List<CastingCharacterDto> characters;

    public CastingProjectDto() {}

    public CastingProjectDto(CastingProject project) {
        this.id = project.getId();
        this.recruiterId = project.getRecruiter().getId();
        this.recruiterName = project.getRecruiter().getFirstName() + " " + project.getRecruiter().getLastName();
        this.name = project.getName();
        this.projectType = project.getProjectType();
        this.productionHouse = project.getProductionHouse();
        this.director = project.getDirector();
        this.language = project.getLanguage();
        this.shootLocation = project.getShootLocation();
        this.shootStartDate = project.getShootStartDate();
        this.shootEndDate = project.getShootEndDate();
        this.description = project.getDescription();
        this.status = project.getStatus();
        this.createdAt = project.getCreatedAt();
        this.updatedAt = project.getUpdatedAt();
    }

    public CastingProjectDto(CastingProject project, boolean includeCharacters) {
        this(project);
        if (includeCharacters && project.getCharacters() != null) {
            this.characters = project.getCharacters().stream()
                    .map(CastingCharacterDto::new)
                    .collect(Collectors.toList());
        }
    }
}
package com.icastar.platform.dto.casting;

import com.icastar.platform.entity.CastingProject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateCastingProjectDto {

    @NotBlank(message = "Project name is required")
    private String name;

    @NotNull(message = "Project type is required")
    private CastingProject.ProjectType projectType;

    private String productionHouse;
    private String director;
    private String language;
    private String shootLocation;
    private LocalDate shootStartDate;
    private LocalDate shootEndDate;
    private String description;
}
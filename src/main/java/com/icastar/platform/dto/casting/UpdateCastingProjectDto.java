package com.icastar.platform.dto.casting;

import com.icastar.platform.entity.CastingProject;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateCastingProjectDto {
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
}

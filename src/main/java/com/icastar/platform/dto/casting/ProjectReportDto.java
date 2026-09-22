package com.icastar.platform.dto.casting;

import com.icastar.platform.entity.CastingProject;
import lombok.Data;

import java.util.List;

@Data
public class ProjectReportDto {
    private Long projectId;
    private String projectName;
    private CastingProject.ProjectType projectType;
    private Integer totalCharacters;
    private Integer castCharacters;
    private Integer openJobs;
    private Integer totalJobs;
    private Integer totalApplications;
    private Integer hiredCount;
    private List<CharacterReportDto> characters;

    @Data
    public static class CharacterReportDto {
        private Long characterId;
        private String characterName;
        private String roleType;
        private Long jobId;
        private String jobStatus;
        private Integer applications;
        private Integer requiredCount;
        private List<SelectedArtistDto> selectedArtists;
    }
}

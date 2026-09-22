package com.icastar.platform.service;

import com.icastar.platform.dto.casting.*;
import com.icastar.platform.entity.*;
import com.icastar.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CastingProjectService {

    private final CastingProjectRepository projectRepository;
    private final CastingCharacterRepository characterRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    // ============ PROJECT METHODS ============

    @Transactional(readOnly = true)
    public Page<CastingProjectDto> getProjectsByRecruiter(Long recruiterId, Pageable pageable) {
        log.info("Fetching projects for recruiter: {}", recruiterId);
        Page<CastingProject> projects = projectRepository.findByRecruiterId(recruiterId, pageable);
        return projects.map(project -> new CastingProjectDto(project, true));
    }

    @Transactional(readOnly = true)
    public Optional<CastingProject> findById(Long projectId) {
        return projectRepository.findById(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<CastingProject> findByIdWithCharacters(Long projectId) {
        return projectRepository.findByIdWithCharacters(projectId);
    }

    public CastingProject createProject(Long recruiterId, CreateCastingProjectDto dto) {
        log.info("Creating new project for recruiter: {}", recruiterId);

        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));

        CastingProject project = new CastingProject();
        project.setRecruiter(recruiter);
        project.setName(dto.getName());
        project.setProjectType(dto.getProjectType());
        project.setProductionHouse(dto.getProductionHouse());
        project.setDirector(dto.getDirector());
        project.setLanguage(dto.getLanguage());
        project.setShootLocation(dto.getShootLocation());
        project.setShootStartDate(dto.getShootStartDate());
        project.setShootEndDate(dto.getShootEndDate());
        project.setDescription(dto.getDescription());
        project.setStatus(CastingProject.ProjectStatus.ACTIVE);

        return projectRepository.save(project);
    }

    public CastingProject updateProject(Long projectId, Long recruiterId, UpdateCastingProjectDto dto) {
        log.info("Updating project {} for recruiter: {}", projectId, recruiterId);

        CastingProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify ownership
        if (!project.getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("You don't have permission to update this project");
        }

        if (dto.getName() != null) {
            project.setName(dto.getName());
        }
        if (dto.getProjectType() != null) {
            project.setProjectType(dto.getProjectType());
        }
        if (dto.getProductionHouse() != null) {
            project.setProductionHouse(dto.getProductionHouse());
        }
        if (dto.getDirector() != null) {
            project.setDirector(dto.getDirector());
        }
        if (dto.getLanguage() != null) {
            project.setLanguage(dto.getLanguage());
        }
        if (dto.getShootLocation() != null) {
            project.setShootLocation(dto.getShootLocation());
        }
        if (dto.getShootStartDate() != null) {
            project.setShootStartDate(dto.getShootStartDate());
        }
        if (dto.getShootEndDate() != null) {
            project.setShootEndDate(dto.getShootEndDate());
        }
        if (dto.getDescription() != null) {
            project.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            project.setStatus(dto.getStatus());
        }

        project.setUpdatedAt(LocalDateTime.now());
        return projectRepository.save(project);
    }

    public void deleteProject(Long projectId, Long recruiterId) {
        log.info("Deleting project {} for recruiter: {}", projectId, recruiterId);

        CastingProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify ownership
        if (!project.getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("You don't have permission to delete this project");
        }

        projectRepository.delete(project);
    }

    // ============ CHARACTER METHODS ============

    @Transactional(readOnly = true)
    public List<CastingCharacterDto> getCharactersByProject(Long projectId, Long recruiterId) {
        log.info("Fetching characters for project: {}", projectId);

        // Verify project ownership
        if (!projectRepository.existsByIdAndRecruiterId(projectId, recruiterId)) {
            throw new RuntimeException("Project not found or you don't have access");
        }

        List<CastingCharacter> characters = characterRepository.findByProjectId(projectId);
        return characters.stream()
                .map(CastingCharacterDto::new)
                .collect(Collectors.toList());
    }

    public CastingCharacter createCharacter(Long projectId, Long recruiterId, CreateCastingCharacterDto dto) {
        log.info("Creating character for project: {}", projectId);

        CastingProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify ownership
        if (!project.getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("You don't have permission to add characters to this project");
        }

        CastingCharacter character = new CastingCharacter();
        character.setProject(project);
        character.setName(dto.getName());
        character.setRoleType(dto.getRoleType());
        character.setGender(dto.getGender() != null ? dto.getGender() : CastingCharacter.Gender.ANY);
        character.setAgeMin(dto.getAgeMin());
        character.setAgeMax(dto.getAgeMax());
        character.setDescription(dto.getDescription());
        character.setRequiredCount(dto.getRequiredCount() != null ? dto.getRequiredCount() : 1);

        return characterRepository.save(character);
    }

    public CastingCharacter updateCharacter(Long characterId, Long recruiterId, CreateCastingCharacterDto dto) {
        log.info("Updating character: {}", characterId);

        CastingCharacter character = characterRepository.findByIdWithProject(characterId)
                .orElseThrow(() -> new RuntimeException("Character not found"));

        // Verify ownership
        if (!character.getProject().getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("You don't have permission to update this character");
        }

        if (dto.getName() != null) {
            character.setName(dto.getName());
        }
        if (dto.getRoleType() != null) {
            character.setRoleType(dto.getRoleType());
        }
        if (dto.getGender() != null) {
            character.setGender(dto.getGender());
        }
        if (dto.getAgeMin() != null) {
            character.setAgeMin(dto.getAgeMin());
        }
        if (dto.getAgeMax() != null) {
            character.setAgeMax(dto.getAgeMax());
        }
        if (dto.getDescription() != null) {
            character.setDescription(dto.getDescription());
        }
        if (dto.getRequiredCount() != null) {
            character.setRequiredCount(dto.getRequiredCount());
        }

        character.setUpdatedAt(LocalDateTime.now());
        return characterRepository.save(character);
    }

    public void deleteCharacter(Long characterId, Long recruiterId) {
        log.info("Deleting character: {}", characterId);

        CastingCharacter character = characterRepository.findByIdWithProject(characterId)
                .orElseThrow(() -> new RuntimeException("Character not found"));

        // Verify ownership
        if (!character.getProject().getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("You don't have permission to delete this character");
        }

        characterRepository.delete(character);
    }

    // ============ VALIDATION METHODS ============

    public boolean validateCharacterBelongsToProject(Long characterId, Long projectId) {
        return characterRepository.existsByIdAndProjectId(characterId, projectId);
    }

    public boolean validateProjectBelongsToRecruiter(Long projectId, Long recruiterId) {
        return projectRepository.existsByIdAndRecruiterId(projectId, recruiterId);
    }

    public boolean validateCharacterBelongsToRecruiter(Long characterId, Long recruiterId) {
        return characterRepository.existsByIdAndRecruiterId(characterId, recruiterId);
    }

    // ============ PROJECT REPORT METHODS ============

    @Transactional(readOnly = true)
    public List<ProjectReportDto> getProjectReport(Long recruiterId) {
        log.info("Generating project report for recruiter: {}", recruiterId);

        List<CastingProject> projects = projectRepository.findByRecruiterId(recruiterId);
        List<ProjectReportDto> reports = new ArrayList<>();

        for (CastingProject project : projects) {
            ProjectReportDto report = new ProjectReportDto();
            report.setProjectId(project.getId());
            report.setProjectName(project.getName());
            report.setProjectType(project.getProjectType());

            // Get characters for this project
            List<CastingCharacter> characters = characterRepository.findByProjectId(project.getId());
            report.setTotalCharacters(characters.size());

            // Get all jobs for this project
            List<Job> projectJobs = jobRepository.findByProjectId(project.getId());
            report.setTotalJobs(projectJobs.size());

            int openJobs = 0;
            int totalApplications = 0;
            int hiredCount = 0;
            int castCharacters = 0;

            List<ProjectReportDto.CharacterReportDto> characterReports = new ArrayList<>();

            for (CastingCharacter character : characters) {
                ProjectReportDto.CharacterReportDto charReport = new ProjectReportDto.CharacterReportDto();
                charReport.setCharacterId(character.getId());
                charReport.setCharacterName(character.getName());
                charReport.setRoleType(character.getRoleType().name());
                charReport.setRequiredCount(character.getRequiredCount());

                // Find job for this character
                List<Job> charJobs = jobRepository.findByCharacterId(character.getId());
                if (!charJobs.isEmpty()) {
                    Job job = charJobs.get(0); // Assume one job per character
                    charReport.setJobId(job.getId());
                    charReport.setJobStatus(job.getStatus().name());

                    if (job.getStatus() == Job.JobStatus.ACTIVE) {
                        openJobs++;
                    }

                    // Count applications
                    int jobApplications = job.getApplicationsCount() != null ? job.getApplicationsCount() : 0;
                    charReport.setApplications(jobApplications);
                    totalApplications += jobApplications;

                    // Get hired artists
                    List<JobApplication> hiredApps = applicationRepository
                            .findByJobIdAndStatus(job.getId(), JobApplication.ApplicationStatus.HIRED);

                    List<SelectedArtistDto> selectedArtists = hiredApps.stream()
                            .map(SelectedArtistDto::new)
                            .collect(Collectors.toList());
                    charReport.setSelectedArtists(selectedArtists);

                    hiredCount += selectedArtists.size();

                    if (!selectedArtists.isEmpty()) {
                        castCharacters++;
                    }
                }

                characterReports.add(charReport);
            }

            report.setCastCharacters(castCharacters);
            report.setOpenJobs(openJobs);
            report.setTotalApplications(totalApplications);
            report.setHiredCount(hiredCount);
            report.setCharacters(characterReports);

            reports.add(report);
        }

        return reports;
    }
}

package com.icastar.platform.controller;

import com.icastar.platform.dto.casting.*;
import com.icastar.platform.entity.CastingCharacter;
import com.icastar.platform.entity.CastingProject;
import com.icastar.platform.entity.User;
import com.icastar.platform.service.CastingProjectService;
import com.icastar.platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recruiter/projects")
@RequiredArgsConstructor
@Slf4j
public class RecruiterProjectController {

    private final CastingProjectService castingProjectService;
    private final UserService userService;

    /**
     * Get all projects for the logged-in recruiter
     * GET /recruiter/projects?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            User recruiter = getAuthenticatedRecruiter();

            log.info("Fetching projects for recruiter: {}", recruiter.getEmail());

            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<CastingProjectDto> projects = castingProjectService.getProjectsByRecruiter(recruiter.getId(), pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", projects.getContent());
            response.put("totalElements", projects.getTotalElements());
            response.put("totalPages", projects.getTotalPages());
            response.put("currentPage", projects.getNumber());
            response.put("size", projects.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching projects", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Create a new project
     * POST /recruiter/projects
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(
            @Valid @RequestBody CreateCastingProjectDto createDto) {
        try {
            User recruiter = getAuthenticatedRecruiter();

            log.info("Creating project for recruiter: {}", recruiter.getEmail());

            CastingProject project = castingProjectService.createProject(recruiter.getId(), createDto);
            CastingProjectDto projectDto = new CastingProjectDto(project, false);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project created successfully");
            response.put("data", projectDto);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating project", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update a project
     * PUT /recruiter/projects/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCastingProjectDto updateDto) {
        try {
            User recruiter = getAuthenticatedRecruiter();

            log.info("Updating project {} for recruiter: {}", id, recruiter.getEmail());

            CastingProject project = castingProjectService.updateProject(id, recruiter.getId(), updateDto);
            CastingProjectDto projectDto = new CastingProjectDto(project, true);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project updated successfully");
            response.put("data", projectDto);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating project", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get characters for a project
     * GET /recruiter/projects/{id}/characters
     */
    @GetMapping("/{id}/characters")
    public ResponseEntity<Map<String, Object>> getCharacters(@PathVariable Long id) {
        try {
            User recruiter = getAuthenticatedRecruiter();

            log.info("Fetching characters for project {} by recruiter: {}", id, recruiter.getEmail());

            List<CastingCharacterDto> characters = castingProjectService.getCharactersByProject(id, recruiter.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", characters);
            response.put("count", characters.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching characters", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Create a character for a project
     * POST /recruiter/projects/{id}/characters
     */
    @PostMapping("/{id}/characters")
    public ResponseEntity<Map<String, Object>> createCharacter(
            @PathVariable Long id,
            @Valid @RequestBody CreateCastingCharacterDto createDto) {
        try {
            User recruiter = getAuthenticatedRecruiter();

            log.info("Creating character for project {} by recruiter: {}", id, recruiter.getEmail());

            CastingCharacter character = castingProjectService.createCharacter(id, recruiter.getId(), createDto);
            CastingCharacterDto characterDto = new CastingCharacterDto(character);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Character created successfully");
            response.put("data", characterDto);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating character", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update a character
     * PUT /recruiter/projects/{projectId}/characters/{characterId}
     */
    @PutMapping("/{projectId}/characters/{characterId}")
    public ResponseEntity<Map<String, Object>> updateCharacter(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @Valid @RequestBody CreateCastingCharacterDto updateDto) {
        try {
            User recruiter = getAuthenticatedRecruiter();

            log.info("Updating character {} for project {} by recruiter: {}", characterId, projectId, recruiter.getEmail());

            CastingCharacter character = castingProjectService.updateCharacter(characterId, recruiter.getId(), updateDto);
            CastingCharacterDto characterDto = new CastingCharacterDto(character);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Character updated successfully");
            response.put("data", characterDto);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating character", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete a character
     * DELETE /recruiter/projects/{projectId}/characters/{characterId}
     */
    @DeleteMapping("/{projectId}/characters/{characterId}")
    public ResponseEntity<Map<String, Object>> deleteCharacter(
            @PathVariable Long projectId,
            @PathVariable Long characterId) {
        try {
            User recruiter = getAuthenticatedRecruiter();

            log.info("Deleting character {} for project {} by recruiter: {}", characterId, projectId, recruiter.getEmail());

            castingProjectService.deleteCharacter(characterId, recruiter.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Character deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting character", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete a project
     * DELETE /recruiter/projects/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProject(@PathVariable Long id) {
        try {
            User recruiter = getAuthenticatedRecruiter();

            log.info("Deleting project {} by recruiter: {}", id, recruiter.getEmail());

            castingProjectService.deleteProject(id, recruiter.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting project", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // ============ HELPER METHODS ============

    private User getAuthenticatedRecruiter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != User.UserRole.RECRUITER) {
            throw new RuntimeException("Only recruiters can access this resource");
        }

        return user;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}

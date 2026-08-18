package com.icastar.platform.controller;

import com.icastar.platform.dto.job.PublicApplyRequestDto;
import com.icastar.platform.dto.job.PublicJobDto;
import com.icastar.platform.entity.JobApplication;
import com.icastar.platform.service.JobApplicationService;
import com.icastar.platform.service.PublicJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/public/jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public Jobs", description = "Public job endpoints - No authentication required")
public class PublicJobController {

    private final PublicJobService publicJobService;
    private final JobApplicationService jobApplicationService;

    /**
     * Get public job details by job ID
     * GET /api/public/jobs/{jobId}
     * No authentication required
     *
     * Uses PublicJobDto which is cached in Redis without Hibernate entities.
     */
    @Operation(summary = "Get public job details", description = "Get job details by ID - No authentication required")
    @GetMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobDetails(@PathVariable Long jobId) {
        try {
            log.info("Fetching public job details for job ID: {}", jobId);

            // Get cached DTO (no Hibernate entities)
            PublicJobDto jobDto = publicJobService.findPublicJobById(jobId);

            // Check if job exists and is active
            if (jobDto == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Job not found or no longer available");
                return ResponseEntity.status(404).body(response);
            }

            // Build response data from DTO
            Map<String, Object> data = new HashMap<>();
            data.put("id", jobDto.getId());
            data.put("title", jobDto.getTitle());
            data.put("description", jobDto.getDescription());
            data.put("requirements", jobDto.getRequirements());
            data.put("responsibilities", jobDto.getResponsibilities());
            data.put("location", jobDto.getLocation());
            data.put("isRemote", jobDto.getIsRemote());
            data.put("jobType", jobDto.getJobType());
            data.put("experienceLevel", jobDto.getExperienceLevel());
            data.put("budgetMin", jobDto.getBudgetMin());
            data.put("budgetMax", jobDto.getBudgetMax());
            data.put("currency", jobDto.getCurrency());
            data.put("applicationDeadline", jobDto.getApplicationDeadline());
            data.put("startDate", jobDto.getStartDate());
            data.put("endDate", jobDto.getEndDate());
            data.put("durationDays", jobDto.getDurationDays());
            data.put("isUrgent", jobDto.getIsUrgent());
            data.put("isFeatured", jobDto.getIsFeatured());
            data.put("benefits", jobDto.getBenefits());
            data.put("skills", jobDto.getSkills());
            data.put("companyName", jobDto.getCompanyName());
            data.put("companyLogoUrl", jobDto.getCompanyLogoUrl());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);

            // Increment view count asynchronously (does not affect cached DTO)
            try {
                publicJobService.incrementViews(jobId);
            } catch (Exception e) {
                // Log but don't fail the request if view increment fails
                log.warn("Failed to increment views for job {}: {}", jobId, e.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching public job details for job ID {}: {}", jobId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Apply for a job publicly (no login required)
     * POST /api/public/jobs/{jobId}/apply
     * No authentication required
     */
    @Operation(summary = "Apply for job publicly", description = "Submit a job application without login - No authentication required")
    @PostMapping("/{jobId}/apply")
    public ResponseEntity<Map<String, Object>> applyForJob(
            @PathVariable Long jobId,
            @Valid @RequestBody PublicApplyRequestDto applyDto) {
        try {
            log.info("Public job application for job ID: {} from email: {}", jobId, applyDto.getEmail());

            JobApplication application = jobApplicationService.createPublicApplication(jobId, applyDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application submitted successfully");
            response.put("applicationId", application.getId());

            return ResponseEntity.ok(response);

        } catch (BadRequestException e) {
            log.warn("Bad request for public job application - job ID {}: {}", jobId, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            log.error("Error in public job application for job ID {}: {}", jobId, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            if (e.getMessage().contains("not found")) {
                response.put("message", "Job not found");
                return ResponseEntity.status(404).body(response);
            }
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error in public job application for job ID {}: {}", jobId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(500).body(response);
        }
    }
}
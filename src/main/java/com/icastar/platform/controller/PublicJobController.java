package com.icastar.platform.controller;

import com.icastar.platform.dto.job.PublicApplyRequestDto;
import com.icastar.platform.entity.Job;
import com.icastar.platform.entity.JobApplication;
import com.icastar.platform.service.JobApplicationService;
import com.icastar.platform.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public Jobs", description = "Public job endpoints - No authentication required")
public class PublicJobController {

    private final JobService jobService;
    private final JobApplicationService jobApplicationService;

    /**
     * Get public job details by job ID
     * GET /api/public/jobs/{jobId}
     * No authentication required
     */
    @Operation(summary = "Get public job details", description = "Get job details by ID - No authentication required")
    @GetMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobDetails(@PathVariable Long jobId) {
        try {
            log.info("Fetching public job details for job ID: {}", jobId);

            Job job = jobService.findById(jobId)
                    .orElse(null);

            // Check if job exists
            if (job == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Job not found");
                return ResponseEntity.status(404).body(response);
            }

            // Check if job is active (not closed, deleted, etc.)
            if (job.getStatus() != Job.JobStatus.ACTIVE) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Job is no longer available");
                return ResponseEntity.status(404).body(response);
            }

            // Build response data
            Map<String, Object> data = new HashMap<>();
            data.put("id", job.getId());
            data.put("title", job.getTitle());
            data.put("description", job.getDescription());
            data.put("requirements", job.getRequirements());
            data.put("location", job.getLocation());
            data.put("isRemote", job.getIsRemote());
            data.put("jobType", job.getJobType());
            data.put("experienceLevel", job.getExperienceLevel());
            data.put("budgetMin", job.getBudgetMin());
            data.put("budgetMax", job.getBudgetMax());
            data.put("currency", job.getCurrency() != null ? job.getCurrency() : "INR");
            data.put("applicationDeadline", job.getApplicationDeadline());
            data.put("startDate", job.getStartDate());
            data.put("endDate", job.getEndDate());
            data.put("durationDays", job.getDurationDays());
            data.put("isUrgent", job.getIsUrgent());
            data.put("isFeatured", job.getIsFeatured());
            data.put("benefits", job.getBenefits());

            // Get company name from recruiter profile
            if (job.getRecruiter() != null && job.getRecruiter().getRecruiterProfile() != null) {
                data.put("companyName", job.getRecruiter().getRecruiterProfile().getCompanyName());
                data.put("companyLogoUrl", job.getRecruiter().getRecruiterProfile().getCompanyLogoUrl());
            } else {
                data.put("companyName", null);
                data.put("companyLogoUrl", null);
            }

            // Parse skills from JSON string
            data.put("skills", parseJsonArray(job.getSkillsRequired()));

            // Parse responsibilities from requirements (or use a separate field if exists)
            data.put("responsibilities", job.getRequirements());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);

            // Increment view count
            jobService.incrementViews(jobId);

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

    /**
     * Parse JSON array string to List<String>
     */
    private List<String> parseJsonArray(String jsonArrayStr) {
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
            // Split by comma and clean up quotes
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
}
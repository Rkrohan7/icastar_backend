package com.icastar.platform.controller;

import com.icastar.platform.dto.job.CreateJobDto;
import com.icastar.platform.dto.job.BulkUploadResult;
import com.icastar.platform.dto.job.JobDto;
import com.icastar.platform.dto.job.UpdateJobDto;
import com.icastar.platform.dto.job.UpdateJobStatusDto;
import com.icastar.platform.entity.Job;
import com.icastar.platform.entity.User;
import com.icastar.platform.service.JobService;
import com.icastar.platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recruiter/jobs")
@RequiredArgsConstructor
@Slf4j
public class RecruiterJobController {

    private final JobService jobService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<JobDto> createJob(@Valid @RequestBody CreateJobDto createDto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User recruiter = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (recruiter.getRole() != User.UserRole.RECRUITER) {
                throw new BadRequestException("Only recruiters can create jobs");
            }

            log.info("Creating job for recruiter: {}", email);

            Job job = jobService.createJob(recruiter.getId(), createDto);
            JobDto jobDto = new JobDto(job);

            return ResponseEntity.ok(jobDto);
        } catch (Exception e) {
            log.error("Error creating job", e);
            throw new RuntimeException("Failed to create job: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Page<JobDto>> getMyJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long projectId) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User recruiter = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (recruiter.getRole() != User.UserRole.RECRUITER) {
                throw new RuntimeException("Only recruiters can view jobs");
            }

            log.info("Fetching jobs for recruiter: {} with projectId filter: {}", email, projectId);

            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Job> jobs = jobService.findByRecruiterWithProjectFilter(recruiter.getId(), projectId, pageable);
            Page<JobDto> jobDtos = jobs.map(JobDto::new);

            return ResponseEntity.ok(jobDtos);
        } catch (Exception e) {
            log.error("Error fetching jobs", e);
            throw new RuntimeException("Failed to fetch jobs: " + e.getMessage());
        }
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobDto> getMyJob(@PathVariable Long jobId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User recruiter = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (recruiter.getRole() != User.UserRole.RECRUITER) {
                throw new RuntimeException("Only recruiters can view jobs");
            }

            log.info("Fetching job {} for recruiter: {}", jobId, email);

            Job job = jobService.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));

            // Verify ownership
            if (!job.getRecruiter().getId().equals(recruiter.getId())) {
                throw new RuntimeException("You don't have permission to view this job");
            }

            JobDto jobDto = new JobDto(job);

            return ResponseEntity.ok(jobDto);
        } catch (Exception e) {
            log.error("Error fetching job", e);
            throw new RuntimeException("Failed to fetch job: " + e.getMessage());
        }
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<JobDto> updateJob(
            @PathVariable Long jobId,
            @Valid @RequestBody UpdateJobDto updateDto) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User recruiter = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (recruiter.getRole() != User.UserRole.RECRUITER) {
                throw new RuntimeException("Only recruiters can update jobs");
            }

            log.info("Updating job {} for recruiter: {}", jobId, email);

            Job job = jobService.updateJob(jobId, recruiter.getId(), updateDto);
            JobDto jobDto = new JobDto(job);

            return ResponseEntity.ok(jobDto);
        } catch (Exception e) {
            log.error("Error updating job", e);
            throw new RuntimeException("Failed to update job: " + e.getMessage());
        }
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Map<String, String>> deleteJob(@PathVariable Long jobId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User recruiter = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (recruiter.getRole() != User.UserRole.RECRUITER) {
                throw new RuntimeException("Only recruiters can delete jobs");
            }

            log.info("Deleting job {} for recruiter: {}", jobId, email);

            jobService.deleteJob(jobId, recruiter.getId());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Job deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting job", e);
            throw new RuntimeException("Failed to delete job: " + e.getMessage());
        }
    }

    @PostMapping("/{jobId}/toggle-visibility")
    public ResponseEntity<JobDto> toggleJobVisibility(@PathVariable Long jobId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User recruiter = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (recruiter.getRole() != User.UserRole.RECRUITER) {
                throw new RuntimeException("Only recruiters can modify jobs");
            }

            log.info("Toggling visibility for job {} for recruiter: {}", jobId, email);

            Job job = jobService.toggleJobVisibility(jobId, recruiter.getId());
            JobDto jobDto = new JobDto(job);

            return ResponseEntity.ok(jobDto);
        } catch (Exception e) {
            log.error("Error toggling job visibility", e);
            throw new RuntimeException("Failed to toggle job visibility: " + e.getMessage());
        }
    }

    @PostMapping("/{jobId}/status")
    public ResponseEntity<Map<String, Object>> updateJobStatus(
            @PathVariable Long jobId,
            @Valid @RequestBody UpdateJobStatusDto updateStatusDto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User recruiter = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (recruiter.getRole() != User.UserRole.RECRUITER) {
                throw new RuntimeException("Only recruiters can update job status");
            }

            log.info("Updating status for job {} to {} by recruiter: {}",
                    jobId, updateStatusDto.getStatus(), email);

            Job job = jobService.updateJobStatus(
                    jobId,
                    recruiter.getId(),
                    updateStatusDto.getStatus(),
                    updateStatusDto.getReason()
            );

            JobDto jobDto = new JobDto(job);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job status updated successfully");
            response.put("data", jobDto);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error updating job status", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error updating job status", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update job status");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMyJobStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User recruiter = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (recruiter.getRole() != User.UserRole.RECRUITER) {
                throw new RuntimeException("Only recruiters can view job stats");
            }

            log.info("Fetching job stats for recruiter: {}", email);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalJobs", jobService.getJobsCountByRecruiter(recruiter.getId()));

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching job stats", e);
            throw new RuntimeException("Failed to fetch job stats: " + e.getMessage());
        }
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<Map<String, Object>> bulkUploadJobs(
            @RequestParam("file") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User recruiter = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (recruiter.getRole() != User.UserRole.RECRUITER) {
                throw new BadRequestException("Only recruiters can upload jobs in bulk");
            }

            if (file.isEmpty()) {
                throw new BadRequestException("File cannot be empty");
            }

            log.info("Bulk uploading jobs for recruiter: {}", email);

            BulkUploadResult result = jobService.bulkUploadJobs(recruiter.getId(), file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bulk job upload completed");
            response.put("summary", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in bulk job upload", e);
            throw new RuntimeException("Failed to process bulk upload: " + e.getMessage());
        }
    }



}

package com.icastar.platform.controller;

import com.icastar.platform.dto.recruiter.*;
import com.icastar.platform.dto.recruiter.UpdateRecruiterProfileDto;
import com.icastar.platform.dto.application.JobApplicationDto;
import com.icastar.platform.entity.User;
import com.icastar.platform.service.RecruiterDashboardService;
import com.icastar.platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recruiter/dashboard")
@RequiredArgsConstructor
@Slf4j
public class RecruiterDashboardController {

    private final RecruiterDashboardService recruiterDashboardService;
    private final UserService userService;
    private final com.icastar.platform.service.CastingProjectService castingProjectService;

    /**
     * Get recruiter's profile
     * @param authentication Authentication object containing user details
     * @return Recruiter's profile information
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getRecruiterProfile(Authentication authentication) {
        try {
            String email = authentication.getName();
            RecruiterProfileDto profile = recruiterDashboardService.getRecruiterProfile(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", profile);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting recruiter profile: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get recruiter profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    

    /**
     * Get all applicants for a specific job
     * @param jobId The ID of the job
     * @param authentication The authentication object containing user details
     * @return List of job applications with applicant details
     */
    @GetMapping("/jobs/{jobId}/applicants")
    public ResponseEntity<Map<String, Object>> getJobApplicants(
            @PathVariable Long jobId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            List<JobApplicationDto> applicants = recruiterDashboardService.getJobApplicants(jobId, email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "jobId", jobId,
                "totalApplicants", applicants.size(),
                "applicants", applicants
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting job applicants: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get job applicants: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get recruiter dashboard overview
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            RecruiterDashboardDto dashboard = recruiterDashboardService.getDashboard(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dashboard);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting recruiter dashboard: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get dashboard: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    
    /**
     * View artists who applied to a job
     */
    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<Map<String, Object>> getJobApplications(
            @PathVariable Long jobId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String artistCategory,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String experienceLevel,
            Pageable pageable,
            Authentication authentication) {
        try {
            User recruiter = (User) authentication.getPrincipal();
            Page<RecentApplicationDto> applications = recruiterDashboardService.getJobApplications(
                    jobId, status, artistCategory, location, experienceLevel, pageable, recruiter);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", applications.getContent());
            response.put("totalElements", applications.getTotalElements());
            response.put("totalPages", applications.getTotalPages());
            response.put("currentPage", applications.getNumber());
            response.put("size", applications.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting job applications: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get applications: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Browse artist profiles
     */
    @GetMapping("/artists")
    public ResponseEntity<Map<String, Object>> browseArtists(
            @RequestParam(required = false) String artistCategory,
            @RequestParam(required = false) String artistType,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String genres,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(required = false) Boolean isPremium,
            Pageable pageable,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            Page<ArtistSuggestionDto> artists = recruiterDashboardService.browseArtists(
                    artistCategory, artistType, location, skills, genres, experienceLevel, 
                    availability, isVerified, isPremium, pageable, email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", artists.getContent());
            response.put("totalElements", artists.getTotalElements());
            response.put("totalPages", artists.getTotalPages());
            response.put("currentPage", artists.getNumber());
            response.put("size", artists.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error browsing artists: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to browse artists: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get artist suggestions based on job criteria
     */
    @GetMapping("/suggestions")
    public ResponseEntity<Map<String, Object>> getArtistSuggestions(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String artistCategory,
            @RequestParam(required = false) String artistType,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String genres,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(required = false) Boolean isPremium,
            @RequestParam(defaultValue = "10") Integer limit,
            Authentication authentication) {
        try {
            User recruiter = (User) authentication.getPrincipal();
            List<ArtistSuggestionDto> suggestions = recruiterDashboardService.getArtistSuggestions(
                    jobId, artistCategory, artistType, location, skills, genres, 
                    experienceLevel, availability, isVerified, isPremium, limit, recruiter);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", suggestions);
            response.put("count", suggestions.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting artist suggestions: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get suggestions: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    /**
     * Get recruiter statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            Map<String, Object> statistics = recruiterDashboardService.getStatistics(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting recruiter statistics: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get statistics: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }




    /**
     * Get subscription status and features
     */
    @GetMapping("/subscription")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus(Authentication authentication) {
        try {
            User recruiter = (User) authentication.getPrincipal();
            Map<String, Object> subscription = recruiterDashboardService.getSubscriptionStatus(recruiter);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", subscription);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting subscription status: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get subscription status: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Update job status
     */
    @PutMapping("/jobs/{jobId}/status")
    public ResponseEntity<Map<String, Object>> updateJobStatus(
            @PathVariable Long jobId,
            @RequestParam String status,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        try {
            User recruiter = (User) authentication.getPrincipal();
            Map<String, Object> result = recruiterDashboardService.updateJobStatus(jobId, status, reason, recruiter);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job status updated successfully");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating job status: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update job status: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Boost job visibility
     */
    @PostMapping("/jobs/{jobId}/boost")
    public ResponseEntity<Map<String, Object>> boostJob(
            @PathVariable Long jobId,
            @RequestParam(required = false) Integer days,
            Authentication authentication) {
        try {
            User recruiter = (User) authentication.getPrincipal();
            Map<String, Object> result = recruiterDashboardService.boostJob(jobId, days, recruiter);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job boosted successfully");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error boosting job: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to boost job: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get all hired applicants for the currently logged-in recruiter
     * Security: Only returns hires belonging to the authenticated recruiter
     * - recruiterId is derived from JWT/SecurityContext, NOT from request params
     * - Validates recruiter ownership for all returned data
     * - Optional filtering by jobId (only within recruiter's own jobs)
     *
     * @param jobId Optional filter for specific job (must belong to logged-in recruiter)
     * @param pageable Pagination parameters (default: 20 items, sorted by hiredAt DESC)
     * @param authentication Spring Security authentication context
     * @return Map containing hires data with pagination metadata
     */
    @GetMapping("/hires")
    public ResponseEntity<Map<String, Object>> getHires(
            @RequestParam(required = false) Long jobId,
            @PageableDefault(size = 20, sort = "hiredAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        try {
            // SECURITY: Extract user from authentication context (JWT/SecurityContext)
            // Never accept recruiterId from request params to prevent data leakage
            if (authentication == null || authentication.getName() == null) {
                log.warn("Unauthorized access attempt to /hires - no authentication");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // SECURITY: Validate user has RECRUITER role
            boolean isRecruiter = User.UserRole.RECRUITER.equals(user.getRole());

            if (!isRecruiter) {
                log.warn("Forbidden access attempt to /hires by non-recruiter user: {} (role: {})",
                        email, user.getRole());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Access forbidden: Recruiter role required");
                return ResponseEntity.status(403).body(response);
            }

            log.info("Fetching hired applicants for recruiter: {} (jobId filter: {})", email, jobId);

            // Service layer enforces additional ownership validation
            Map<String, Object> hires = recruiterDashboardService.getHiredApplicants(user, jobId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", hires);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // Handle specific error cases with appropriate HTTP status codes
            String errorMessage = e.getMessage();

            if (errorMessage != null && errorMessage.contains("not found")) {
                log.error("Resource not found for /hires: {}", errorMessage);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", errorMessage);
                return ResponseEntity.status(404).body(response);
            } else if (errorMessage != null && errorMessage.contains("Unauthorized")) {
                log.warn("Unauthorized access attempt to /hires: {}", errorMessage);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", errorMessage);
                return ResponseEntity.status(403).body(response);
            } else {
                log.error("Error getting hired applicants: {}", errorMessage, e);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to get hired applicants: " + errorMessage);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Unexpected error getting hired applicants: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * View all applicants for a specific job
     * GET /recruiter/dashboard/view-applicants/{jobId}
     */
    @GetMapping("/view-applicants/{jobId}")
    public ResponseEntity<Map<String, Object>> viewApplicantsByJobId(
            @PathVariable Long jobId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isShortlisted,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) Integer maxExperience,
            @PageableDefault(size = 20, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Fetching applicants for job ID: {} by recruiter: {}", jobId, email);

            // Verify the job belongs to this recruiter
            Map<String, Object> applicants = recruiterDashboardService.getApplicantsByJobId(
                    user, jobId, status, isShortlisted, minExperience, maxExperience, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", applicants);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting applicants for job {}: {}", jobId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get applicants: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Submit interview result (HIRED or REJECTED)
     * POST /recruiter/dashboard/interview-result
     */
    @PostMapping("/interview-result")
    public ResponseEntity<Map<String, Object>> submitInterviewResult(
            @Valid @RequestBody InterviewResultDto resultDto,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Submitting interview result for application ID: {} by recruiter: {}",
                    resultDto.getApplicationId(), email);

            Map<String, Object> result = recruiterDashboardService.submitInterviewResult(user, resultDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Interview result submitted successfully");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error submitting interview result: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to submit interview result: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Schedule interview for an applicant
     * POST /recruiter/dashboard/schedule-interview
     */
    @PostMapping("/schedule-interview")
    public ResponseEntity<Map<String, Object>> scheduleInterview(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Validate and extract required fields
            if (request.get("applicationId") == null) {
                throw new RuntimeException("applicationId is required");
            }
            if (request.get("interviewDateTime") == null) {
                throw new RuntimeException("interviewDateTime is required");
            }
            if (request.get("interviewType") == null) {
                throw new RuntimeException("interviewType is required");
            }

            Long applicationId = Long.valueOf(request.get("applicationId").toString());
            String interviewDateTime = request.get("interviewDateTime").toString();
            String interviewType = request.get("interviewType").toString();

            // Handle optional fields with null checks
            String interviewLocation = "";
            if (request.get("interviewLocation") != null) {
                interviewLocation = request.get("interviewLocation").toString();
            }

            String meetingLink = "";
            if (request.get("meetingLink") != null) {
                meetingLink = request.get("meetingLink").toString();
            }

            String notes = "";
            if (request.get("notes") != null) {
                notes = request.get("notes").toString();
            }

            log.info("Scheduling interview for application ID: {} by recruiter: {}", applicationId, email);

            Map<String, Object> result = recruiterDashboardService.scheduleInterview(
                    user, applicationId, interviewDateTime, interviewType,
                    interviewLocation, meetingLink, notes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Interview scheduled successfully and email sent to applicant");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error scheduling interview: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to schedule interview: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get dashboard metrics - KPIs with trends
     * GET /recruiter/dashboard/metrics
     *
     * SECURITY: Returns data only for logged-in recruiter (from JWT)
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Fetching dashboard metrics for recruiter: {}", email);
            Map<String, Object> metrics = recruiterDashboardService.getDashboardMetrics(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", metrics);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching dashboard metrics: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch metrics: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get latest applicants for dashboard table
     * GET /recruiter/dashboard/latest-applicants?limit=10
     *
     * SECURITY: Returns data only for logged-in recruiter
     */
    @GetMapping("/latest-applicants")
    public ResponseEntity<Map<String, Object>> getLatestApplicants(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Fetching latest {} applicants for recruiter: {}", limit, email);
            List<Map<String, Object>> applicants = recruiterDashboardService.getLatestApplicants(user, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", applicants);
            response.put("count", applicants.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching latest applicants: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch applicants: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get applications trend for line chart (7 months)
     * GET /recruiter/dashboard/applications-trend
     *
     * SECURITY: Returns data only for logged-in recruiter
     */
    @GetMapping("/applications-trend")
    public ResponseEntity<Map<String, Object>> getApplicationsTrend(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Fetching applications trend for recruiter: {}", email);
            Map<String, Object> trend = recruiterDashboardService.getApplicationsTrend(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", trend);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching applications trend: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch trend data: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get application status breakdown for donut chart
     * GET /recruiter/dashboard/application-status
     *
     * SECURITY: Returns data only for logged-in recruiter
     */
    @GetMapping("/application-status")
    public ResponseEntity<Map<String, Object>> getApplicationStatus(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Fetching application status breakdown for recruiter: {}", email);
            Map<String, Object> status = recruiterDashboardService.getApplicationStatus(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", status);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching application status: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch status data: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get interview outcomes for bar chart
     * GET /recruiter/dashboard/interview-outcomes
     *
     * SECURITY: Returns data only for logged-in recruiter
     */
    @GetMapping("/interview-outcomes")
    public ResponseEntity<Map<String, Object>> getInterviewOutcomes(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Fetching interview outcomes for recruiter: {}", email);
            Map<String, Object> outcomes = recruiterDashboardService.getInterviewOutcomes(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", outcomes);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching interview outcomes: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch outcomes data: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update recruiter's profile
     * PUT /recruiter/dashboard/profile
     *
     * @param updateDto The profile fields to update
     * @param authentication Authentication object containing user details
     * @return Updated recruiter profile
     */
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateRecruiterProfile(
            @Valid @RequestBody UpdateRecruiterProfileDto updateDto,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String email = authentication.getName();
            log.info("Updating recruiter profile for user: {}", email);

            RecruiterProfileDto updatedProfile = recruiterDashboardService.updateRecruiterProfile(email, updateDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("data", updatedProfile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating recruiter profile: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get project-wise report with characters, jobs, and hired artists
     * GET /recruiter/dashboard/project-report
     *
     * SECURITY: Returns data only for logged-in recruiter's projects
     */
    @GetMapping("/project-report")
    public ResponseEntity<Map<String, Object>> getProjectReport(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getRole() != User.UserRole.RECRUITER) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Access forbidden: Recruiter role required");
                return ResponseEntity.status(403).body(response);
            }

            log.info("Fetching project report for recruiter: {}", email);
            List<com.icastar.platform.dto.casting.ProjectReportDto> reports = castingProjectService.getProjectReport(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reports);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching project report: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch project report: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

package com.icastar.platform.controller;

import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.JobApplication;
import com.icastar.platform.entity.User;
import com.icastar.platform.service.ArtistService;
import com.icastar.platform.service.JobApplicationService;
import com.icastar.platform.service.BookmarkedJobService;
import com.icastar.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/candidate/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Candidate Dashboard", description = "APIs for candidate dashboard functionality")
public class CandidateDashboardController {

    private final ArtistService artistService;
    private final JobApplicationService jobApplicationService;
    private final BookmarkedJobService bookmarkedJobService;
    private final UserService userService;

    @Operation(summary = "Get dashboard overview", description = "Get comprehensive dashboard data for the candidate")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Get application statistics
            Long totalApplications = jobApplicationService.countApplicationsByArtist(artistProfile);
            Long shortlistedApplications = jobApplicationService.countApplicationsByRecruiterAndStatus(
                    artistProfile.getId(), JobApplication.ApplicationStatus.SHORTLISTED);
            Long hiredApplications = jobApplicationService.countApplicationsByRecruiterAndStatus(
                    artistProfile.getId(), JobApplication.ApplicationStatus.HIRED);

            // Get recent applications
            List<JobApplication> recentApplications = jobApplicationService.findByArtist(artistProfile)
                    .stream()
                    .limit(5)
                    .toList();

            // Get upcoming interviews
            List<JobApplication> upcomingInterviews = jobApplicationService.findUpcomingInterviews()
                    .stream()
                    .filter(app -> app.getArtist().getId().equals(artistProfile.getId()))
                    .limit(3)
                    .toList();

            // Get bookmarks count
            Long totalBookmarks = bookmarkedJobService.countBookmarksByArtist(artistProfile);

            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("profile", Map.of(
                    "id", artistProfile.getId(),
                    "stageName", artistProfile.getStageName(),
                    "isVerified", artistProfile.hasApprovedFaceAndIdProof(),
                    "faceVerificationStatus", artistProfile.getFaceVerificationStatus().name(),
                    "isFaceVerified", artistProfile.getIsFaceVerified(),
                    "idProofVerified", artistProfile.getIdProofVerified(),
                    "idProofVerificationStatus", artistProfile.getIdProofVerificationStatus().name(),
                    "totalApplications", artistProfile.getTotalApplications(),
                    "successfulHires", artistProfile.getSuccessfulHires()
            ));

            dashboardData.put("statistics", Map.of(
                    "totalApplications", totalApplications,
                    "shortlistedApplications", shortlistedApplications,
                    "hiredApplications", hiredApplications,
                    "totalBookmarks", totalBookmarks
            ));

            dashboardData.put("recentApplications", recentApplications);
            dashboardData.put("upcomingInterviews", upcomingInterviews);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dashboardData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving dashboard overview", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve dashboard data");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Get application statistics", description = "Get detailed statistics about job applications")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/applications/stats")
    public ResponseEntity<Map<String, Object>> getApplicationStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Get applications by status
            Long appliedCount = jobApplicationService.countApplicationsByRecruiterAndStatus(
                    artistProfile.getId(), JobApplication.ApplicationStatus.APPLIED);
            Long underReviewCount = jobApplicationService.countApplicationsByRecruiterAndStatus(
                    artistProfile.getId(), JobApplication.ApplicationStatus.UNDER_REVIEW);
            Long shortlistedCount = jobApplicationService.countApplicationsByRecruiterAndStatus(
                    artistProfile.getId(), JobApplication.ApplicationStatus.SHORTLISTED);
            Long interviewedCount = jobApplicationService.countApplicationsByRecruiterAndStatus(
                    artistProfile.getId(), JobApplication.ApplicationStatus.INTERVIEWED);
            Long selectedCount = jobApplicationService.countApplicationsByRecruiterAndStatus(
                    artistProfile.getId(), JobApplication.ApplicationStatus.SELECTED);
            Long rejectedCount = jobApplicationService.countApplicationsByRecruiterAndStatus(
                    artistProfile.getId(), JobApplication.ApplicationStatus.REJECTED);
            Long hiredCount = jobApplicationService.countApplicationsByRecruiterAndStatus(
                    artistProfile.getId(), JobApplication.ApplicationStatus.HIRED);

            Map<String, Object> stats = new HashMap<>();
            stats.put("applied", appliedCount);
            stats.put("underReview", underReviewCount);
            stats.put("shortlisted", shortlistedCount);
            stats.put("interviewed", interviewedCount);
            stats.put("selected", selectedCount);
            stats.put("rejected", rejectedCount);
            stats.put("hired", hiredCount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving application stats", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve application statistics");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Get recent activity", description = "Get recent activity including applications and interviews")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/activity")
    public ResponseEntity<Map<String, Object>> getRecentActivity(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
            Page<JobApplication> applications = jobApplicationService.findByArtist(artistProfile, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", applications.getContent());
            response.put("totalElements", applications.getTotalElements());
            response.put("totalPages", applications.getTotalPages());
            response.put("currentPage", applications.getNumber());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving recent activity", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve recent activity");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Get upcoming interviews", description = "Get scheduled interviews")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/interviews")
    public ResponseEntity<Map<String, Object>> getUpcomingInterviews() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            List<JobApplication> interviews = jobApplicationService.findUpcomingInterviews()
                    .stream()
                    .filter(app -> app.getArtist().getId().equals(artistProfile.getId()))
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", interviews);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving upcoming interviews", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve upcoming interviews");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Get bookmarked jobs", description = "Get bookmarked jobs for the candidate")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/bookmarks")
    public ResponseEntity<Map<String, Object>> getBookmarkedJobs(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bookmarkedAt"));
            Page<com.icastar.platform.entity.BookmarkedJob> bookmarks = 
                    bookmarkedJobService.findActiveBookmarksByArtist(artistProfile, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bookmarks.getContent());
            response.put("totalElements", bookmarks.getTotalElements());
            response.put("totalPages", bookmarks.getTotalPages());
            response.put("currentPage", bookmarks.getNumber());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving bookmarked jobs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve bookmarked jobs");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Get profile completion status", description = "Get profile completion percentage and missing fields")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/profile/completion")
    public ResponseEntity<Map<String, Object>> getProfileCompletionStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ArtistProfile artistProfile = artistService.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Calculate completion percentage
            int completedFields = 0;
            int totalFields = 10; // Adjust based on your requirements

            if (artistProfile.getStageName() != null && !artistProfile.getStageName().trim().isEmpty()) completedFields++;
            if (artistProfile.getBio() != null && !artistProfile.getBio().trim().isEmpty()) completedFields++;
            if (artistProfile.getLocation() != null && !artistProfile.getLocation().trim().isEmpty()) completedFields++;
            if (artistProfile.getSkills() != null && !artistProfile.getSkills().trim().isEmpty()) completedFields++;
            if (artistProfile.getExperienceYears() != null) completedFields++;
            if (artistProfile.getHourlyRate() != null) completedFields++;
            if (artistProfile.getWeight() != null) completedFields++;
            if (artistProfile.getHeight() != null) completedFields++;
            if (artistProfile.getHairColor() != null && !artistProfile.getHairColor().trim().isEmpty()) completedFields++;
            if (artistProfile.getLanguagesSpoken() != null && !artistProfile.getLanguagesSpoken().trim().isEmpty()) completedFields++;

            int completionPercentage = (completedFields * 100) / totalFields;

            Map<String, Object> completionData = new HashMap<>();
            completionData.put("completionPercentage", completionPercentage);
            completionData.put("completedFields", completedFields);
            completionData.put("totalFields", totalFields);
            completionData.put("isProfileComplete", completionPercentage >= 80);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", completionData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving profile completion status", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve profile completion status");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

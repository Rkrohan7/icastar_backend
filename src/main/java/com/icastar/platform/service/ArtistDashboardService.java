package com.icastar.platform.service;

import com.icastar.platform.config.CacheNames;
import com.icastar.platform.entity.*;
import com.icastar.platform.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtistDashboardService {

    private final ArtistProfileRepository artistProfileRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final PaymentRepository paymentRepository;
    private final UsageTrackingRepository usageTrackingRepository;
    private final NotificationRepository notificationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * ENDPOINT 1: Get dashboard metrics (6 KPIs with trends)
     * GET /api/artist/dashboard/metrics
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.DASHBOARD_ARTIST, key = "#artist.id")
    public Map<String, Object> getDashboardMetrics(User artist) {
        try {
            log.debug("Cache MISS: Fetching dashboard metrics for artist: {}", artist.getEmail());

            // Validate artist profile exists
            ArtistProfile artistProfile = artistProfileRepository.findByUserId(artist.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastMonthStart = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime lastMonthEnd = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
            LocalDateTime currentMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

            // 1. Profile Views
            long totalProfileViews = usageTrackingRepository.countProfileViewsByUserId(artist.getId());
            long lastMonthProfileViews = usageTrackingRepository.countProfileViewsByUserIdAndMonth(artist.getId(), lastMonthStart, lastMonthEnd);
            long currentMonthProfileViews = usageTrackingRepository.countProfileViewsByUserIdAndMonth(artist.getId(), currentMonthStart, now);
            double profileViewsTrend = calculateTrend(currentMonthProfileViews, lastMonthProfileViews);

            // 2. Job Invitations
            long totalJobInvitations = notificationRepository.countJobInvitationsByUserId(artist.getId());
            double jobInvitationsTrend = 0.0; // TODO: Add monthly tracking

            // 3. Applications Sent
            long totalApplications = jobApplicationRepository.countByArtistId(artistProfile.getId());
            long lastMonthApplications = jobApplicationRepository.countByArtistIdAndCreatedAtBetween(artistProfile.getId(), lastMonthStart, lastMonthEnd);
            long currentMonthApplications = jobApplicationRepository.countByArtistIdAndCreatedAtBetween(artistProfile.getId(), currentMonthStart, now);
            double applicationsTrend = calculateTrend(currentMonthApplications, lastMonthApplications);

            // 4. Interviews Scheduled
            long totalInterviews = jobApplicationRepository.countByArtistIdWithInterview(artistProfile.getId());
            long lastMonthInterviews = jobApplicationRepository.countByArtistIdAndInterviewBetween(artistProfile.getId(), lastMonthStart, lastMonthEnd);
            long currentMonthInterviews = jobApplicationRepository.countByArtistIdAndInterviewBetween(artistProfile.getId(), currentMonthStart, now);
            double interviewsTrend = calculateTrend(currentMonthInterviews, lastMonthInterviews);

            // 5. Projects Completed
            long totalProjectsCompleted = jobApplicationRepository.countByArtistIdAndIsHired(artistProfile.getId());
            long lastMonthProjects = jobApplicationRepository.countByArtistIdAndHiredBetween(artistProfile.getId(), lastMonthStart, lastMonthEnd);
            long currentMonthProjects = jobApplicationRepository.countByArtistIdAndHiredBetween(artistProfile.getId(), currentMonthStart, now);
            double projectsTrend = calculateTrend(currentMonthProjects, lastMonthProjects);

            // 6. Credits Balance (Earnings) - with null-safe handling
            BigDecimal totalEarnings = paymentRepository.sumEarningsByUserId(artist.getId());
            BigDecimal lastMonthEarnings = paymentRepository.sumEarningsByUserIdAndMonth(artist.getId(), lastMonthStart, lastMonthEnd);
            BigDecimal currentMonthEarnings = paymentRepository.sumEarningsByUserIdAndMonth(artist.getId(), currentMonthStart, now);

            // Safely handle null BigDecimal values
            totalEarnings = totalEarnings != null ? totalEarnings : BigDecimal.ZERO;
            lastMonthEarnings = lastMonthEarnings != null ? lastMonthEarnings : BigDecimal.ZERO;
            currentMonthEarnings = currentMonthEarnings != null ? currentMonthEarnings : BigDecimal.ZERO;

            double earningsTrend = calculateTrendBigDecimal(currentMonthEarnings, lastMonthEarnings);

            // Build response
            Map<String, Object> metrics = new HashMap<>();

            metrics.put("profileViews", Map.of(
                    "value", totalProfileViews,
                    "trend", profileViewsTrend,
                    "label", "Profile Views"
            ));

            metrics.put("jobInvitations", Map.of(
                    "value", totalJobInvitations,
                    "trend", jobInvitationsTrend,
                    "label", "Job Invitations"
            ));

            metrics.put("applicationsSent", Map.of(
                    "value", totalApplications,
                    "trend", applicationsTrend,
                    "label", "Applications Sent"
            ));

            metrics.put("interviewsScheduled", Map.of(
                    "value", totalInterviews,
                    "trend", interviewsTrend,
                    "label", "Interviews Scheduled"
            ));

            metrics.put("projectsCompleted", Map.of(
                    "value", totalProjectsCompleted,
                    "trend", projectsTrend,
                    "label", "Projects Completed"
            ));

            metrics.put("creditsBalance", Map.of(
                    "value", totalEarnings.doubleValue(),
                    "trend", earningsTrend,
                    "label", "Credits Balance",
                    "currency", "INR"
            ));

            log.info("Dashboard metrics fetched successfully for artist: {}", artist.getEmail());
            return metrics;

        } catch (Exception e) {
            log.error("Error fetching dashboard metrics for artist {}: {}", artist.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to fetch dashboard metrics: " + e.getMessage());
        }
    }

    /**
     * ENDPOINT 2: Get AI-matched job opportunities
     * GET /api/artist/dashboard/job-opportunities?limit=10
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getJobOpportunities(User artist, int limit) {
        try {
            log.info("Fetching job opportunities for artist: {} (limit={})", artist.getEmail(), limit);

            // Validate artist profile exists
            ArtistProfile artistProfile = artistProfileRepository.findByUserId(artist.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            // Get active jobs
            List<Job> activeJobs = jobRepository.findActiveJobsForMatching(LocalDate.now());

            List<Map<String, Object>> matchedJobs = new ArrayList<>();

            for (Job job : activeJobs) {
                // Check if artist already applied
                boolean alreadyApplied = jobRepository.hasArtistAppliedToJob(artistProfile.getId(), job.getId());
                if (alreadyApplied) {
                    continue; // Skip already applied jobs
                }

                // Calculate match score
                double matchScore = calculateJobMatchScore(artistProfile, job);

                // Filter out low matches
                if (matchScore < 30.0) {
                    continue;
                }

                // Build match reasons
                List<String> matchReasons = buildMatchReasons(artistProfile, job, matchScore);

                Map<String, Object> jobMatch = new HashMap<>();
                jobMatch.put("jobId", job.getId());
                jobMatch.put("title", job.getTitle());
                jobMatch.put("company", "Company"); // TODO: Add company field to Job or eager load recruiter profile
                jobMatch.put("location", job.getLocation());
                jobMatch.put("jobType", job.getJobType() != null ? job.getJobType().name() : "NOT_SPECIFIED");
                jobMatch.put("budgetMin", job.getBudgetMin() != null ? job.getBudgetMin().doubleValue() : 0);
                jobMatch.put("budgetMax", job.getBudgetMax() != null ? job.getBudgetMax().doubleValue() : 0);
                jobMatch.put("currency", job.getCurrency() != null ? job.getCurrency() : "INR");
                jobMatch.put("matchScore", Math.round(matchScore * 10.0) / 10.0);
                jobMatch.put("matchReasons", matchReasons);
                jobMatch.put("postedAt", job.getPublishedAt());
                jobMatch.put("applicationDeadline", job.getApplicationDeadline());

                matchedJobs.add(jobMatch);
            }

            // Sort by match score descending
            matchedJobs.sort((a, b) -> Double.compare((Double) b.get("matchScore"), (Double) a.get("matchScore")));

            // Limit results
            List<Map<String, Object>> limitedJobs = matchedJobs.stream().limit(limit).collect(Collectors.toList());

            log.info("Found {} matched jobs for artist: {}", limitedJobs.size(), artist.getEmail());
            return limitedJobs;

        } catch (Exception e) {
            log.error("Error fetching job opportunities for artist {}: {}", artist.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to fetch job opportunities: " + e.getMessage());
        }
    }

    /**
     * ENDPOINT 3: Get profile completion status
     * GET /api/artist/profile/completion
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.DASHBOARD_ARTIST, key = "'completion-' + #artist.id")
    public Map<String, Object> getProfileCompletion(User artist) {
        try {
            log.debug("Cache MISS: Fetching profile completion for artist: {}", artist.getEmail());

            // Validate artist profile exists
            ArtistProfile artistProfile = artistProfileRepository.findByUserId(artist.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            List<String> missingFields = new ArrayList<>();
            int totalFields = 12;
            int completedFields = 0;

            // Check required fields
            if (artistProfile.getFirstName() != null && !artistProfile.getFirstName().trim().isEmpty()) completedFields++; else missingFields.add("First Name");
            if (artistProfile.getLastName() != null && !artistProfile.getLastName().trim().isEmpty()) completedFields++; else missingFields.add("Last Name");
            if (artistProfile.getBio() != null && !artistProfile.getBio().trim().isEmpty()) completedFields++; else missingFields.add("Bio");
            if (artistProfile.getLocation() != null && !artistProfile.getLocation().trim().isEmpty()) completedFields++; else missingFields.add("Location");
            if (artistProfile.getSkills() != null && !artistProfile.getSkills().trim().isEmpty()) completedFields++; else missingFields.add("Skills");
            if (artistProfile.getExperienceYears() != null) completedFields++; else missingFields.add("Experience Years");
            if (artistProfile.getHourlyRate() != null) completedFields++; else missingFields.add("Hourly Rate");
            if (artistProfile.getWeight() != null) completedFields++; else missingFields.add("Weight");
            if (artistProfile.getHeight() != null) completedFields++; else missingFields.add("Height");
            if (artistProfile.getLanguagesSpoken() != null && !artistProfile.getLanguagesSpoken().trim().isEmpty()) completedFields++; else missingFields.add("Languages Spoken");
            if (artistProfile.getPortfolioUrls() != null && !artistProfile.getPortfolioUrls().trim().isEmpty()) completedFields++; else missingFields.add("Portfolio URLs");
            if (artistProfile.getComfortableAreas() != null && !artistProfile.getComfortableAreas().trim().isEmpty()) completedFields++; else missingFields.add("Comfortable Areas");

            int completionPercentage = (completedFields * 100) / totalFields;
            boolean isProfileComplete = completionPercentage >= 80;

            // Build recommendations
            List<String> recommendations = new ArrayList<>();
            if (!missingFields.isEmpty()) {
                if (missingFields.contains("Bio")) {
                    recommendations.add("Write a compelling bio to attract recruiters");
                }
                if (missingFields.contains("Skills")) {
                    recommendations.add("Add your skills to improve job matching");
                }
                if (missingFields.contains("Weight") || missingFields.contains("Height")) {
                    recommendations.add("Complete physical attributes to improve job matching");
                }
                if (missingFields.contains("Portfolio URLs")) {
                    recommendations.add("Add portfolio links to showcase your work");
                }
                if (missingFields.contains("Comfortable Areas")) {
                    recommendations.add("Add comfortable areas to improve job matching");
                }
            } else {
                recommendations.add("Profile complete! Keep it updated for best results.");
            }

            Map<String, Object> completion = new HashMap<>();
            completion.put("completionPercentage", completionPercentage);
            completion.put("isProfileComplete", isProfileComplete);
            completion.put("totalFields", totalFields);
            completion.put("completedFields", completedFields);
            completion.put("missingFields", missingFields);
            completion.put("profileStrength", completionPercentage >= 80 ? "Excellent" : completionPercentage >= 60 ? "Good" : "Needs Improvement");
            completion.put("recommendations", recommendations);

            log.info("Profile completion: {}% for artist: {}", completionPercentage, artist.getEmail());
            return completion;

        } catch (Exception e) {
            log.error("Error fetching profile completion for artist {}: {}", artist.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to fetch profile completion: " + e.getMessage());
        }
    }

    /**
     * ENDPOINT 4: Get profile views trend (7 months)
     * GET /api/artist/dashboard/profile-views-trend
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProfileViewsTrend(User artist) {
        try {
            log.info("Fetching profile views trend for artist: {}", artist.getEmail());

            List<String> months = new ArrayList<>();
            List<Long> views = new ArrayList<>();
            long totalViews = 0;

            LocalDateTime now = LocalDateTime.now();

            // Get 7 months of data
            for (int i = 6; i >= 0; i--) {
                LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime monthEnd = monthStart.plusMonths(1).minusDays(1).withHour(23).withMinute(59).withSecond(59);

                long monthViews = usageTrackingRepository.countProfileViewsByUserIdAndMonth(artist.getId(), monthStart, monthEnd);

                String monthName = monthStart.getMonth().toString().substring(0, 3);
                months.add(monthName);
                views.add(monthViews);
                totalViews += monthViews;
            }

            double averageViewsPerMonth = totalViews / 7.0;
            double growthRate = views.size() >= 2 ? calculateTrend(views.get(6), views.get(0)) : 0.0;

            Map<String, Object> trend = new HashMap<>();
            trend.put("months", months);
            trend.put("views", views);
            trend.put("totalViews", totalViews);
            trend.put("averageViewsPerMonth", Math.round(averageViewsPerMonth * 10.0) / 10.0);
            trend.put("growthRate", Math.round(growthRate * 10.0) / 10.0);

            log.info("Profile views trend fetched for artist: {}", artist.getEmail());
            return trend;

        } catch (Exception e) {
            log.error("Error fetching profile views trend for artist {}: {}", artist.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to fetch profile views trend: " + e.getMessage());
        }
    }

    /**
     * ENDPOINT 5: Get application status breakdown
     * GET /api/artist/dashboard/application-status
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.DASHBOARD_ARTIST, key = "'app-status-' + #artist.id")
    public Map<String, Object> getApplicationStatusBreakdown(User artist) {
        try {
            log.debug("Cache MISS: Fetching application status breakdown for artist: {}", artist.getEmail());

            ArtistProfile artistProfile = artistProfileRepository.findByUserId(artist.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            long pending = jobApplicationRepository.countByArtistIdAndStatus(artistProfile.getId(), JobApplication.ApplicationStatus.APPLIED);
            long underReview = jobApplicationRepository.countByArtistIdAndStatus(artistProfile.getId(), JobApplication.ApplicationStatus.UNDER_REVIEW);
            long shortlisted = jobApplicationRepository.countByArtistIdAndStatus(artistProfile.getId(), JobApplication.ApplicationStatus.SHORTLISTED);
            long interviewing = jobApplicationRepository.countByArtistIdAndStatus(artistProfile.getId(), JobApplication.ApplicationStatus.INTERVIEW_SCHEDULED)
                    + jobApplicationRepository.countByArtistIdAndStatus(artistProfile.getId(), JobApplication.ApplicationStatus.INTERVIEWED);
            long offered = jobApplicationRepository.countByArtistIdAndStatus(artistProfile.getId(), JobApplication.ApplicationStatus.SELECTED);
            long hired = jobApplicationRepository.countByArtistIdAndIsHired(artistProfile.getId());
            long rejected = jobApplicationRepository.countByArtistIdAndStatus(artistProfile.getId(), JobApplication.ApplicationStatus.REJECTED);

            long total = pending + underReview + shortlisted + interviewing + offered + hired + rejected;

            List<String> labels = Arrays.asList("Pending", "Under Review", "Shortlisted", "Interviewing", "Offered", "Hired", "Rejected");
            List<Long> data = Arrays.asList(pending, underReview, shortlisted, interviewing, offered, hired, rejected);

            List<Double> percentages = data.stream()
                    .map(count -> total > 0 ? Math.round((count * 100.0 / total) * 10.0) / 10.0 : 0.0)
                    .collect(Collectors.toList());

            double successRate = total > 0 ? Math.round((hired * 100.0 / total) * 10.0) / 10.0 : 0.0;

            Map<String, Object> statusBreakdown = new HashMap<>();
            statusBreakdown.put("labels", labels);
            statusBreakdown.put("data", data);
            statusBreakdown.put("percentages", percentages);
            statusBreakdown.put("total", total);
            statusBreakdown.put("successRate", successRate);

            log.info("Application status breakdown fetched for artist: {}", artist.getEmail());
            return statusBreakdown;

        } catch (Exception e) {
            log.error("Error fetching application status for artist {}: {}", artist.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to fetch application status: " + e.getMessage());
        }
    }

    /**
     * ENDPOINT 6: Get portfolio items
     * GET /api/artist/dashboard/portfolio?limit=6
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPortfolioItems(User artist, int limit) {
        try {
            log.info("Fetching portfolio items for artist: {} (limit={})", artist.getEmail(), limit);

            ArtistProfile artistProfile = artistProfileRepository.findByUserId(artist.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            List<Map<String, Object>> portfolio = new ArrayList<>();

            // Portfolio Photo
            if (artistProfile.getPhotoUrl() != null && !artistProfile.getPhotoUrl().trim().isEmpty()) {
                Map<String, Object> photoItem = new HashMap<>();
                photoItem.put("id", 1);
                photoItem.put("type", "PHOTO");
                photoItem.put("title", "Portfolio Photo");
                photoItem.put("url", artistProfile.getPhotoUrl());
                photoItem.put("thumbnailUrl", artistProfile.getPhotoUrl());
                photoItem.put("viewCount", 0); // Not tracked yet
                photoItem.put("uploadedAt", artistProfile.getCreatedAt());
                portfolio.add(photoItem);
            }

            // Demo Reel Video
            if (artistProfile.getVideoUrl() != null && !artistProfile.getVideoUrl().trim().isEmpty()) {
                Map<String, Object> videoItem = new HashMap<>();
                videoItem.put("id", 2);
                videoItem.put("type", "VIDEO");
                videoItem.put("title", "Demo Reel");
                videoItem.put("url", artistProfile.getVideoUrl());
                videoItem.put("thumbnailUrl", artistProfile.getVideoUrl());
                videoItem.put("viewCount", 0); // Not tracked yet
                videoItem.put("uploadedAt", artistProfile.getCreatedAt());
                portfolio.add(videoItem);
            }

            // Profile Picture
            if (artistProfile.getProfileUrl() != null && !artistProfile.getProfileUrl().trim().isEmpty()) {
                Map<String, Object> profileItem = new HashMap<>();
                profileItem.put("id", 3);
                profileItem.put("type", "PHOTO");
                profileItem.put("title", "Profile Picture");
                profileItem.put("url", artistProfile.getProfileUrl());
                profileItem.put("thumbnailUrl", artistProfile.getProfileUrl());
                profileItem.put("viewCount", 0); // Not tracked yet
                profileItem.put("uploadedAt", artistProfile.getCreatedAt());
                portfolio.add(profileItem);
            }

            List<Map<String, Object>> limitedPortfolio = portfolio.stream().limit(limit).collect(Collectors.toList());

            log.info("Portfolio items fetched: {} for artist: {}", limitedPortfolio.size(), artist.getEmail());
            return limitedPortfolio;

        } catch (Exception e) {
            log.error("Error fetching portfolio for artist {}: {}", artist.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to fetch portfolio: " + e.getMessage());
        }
    }

    /**
     * ENDPOINT 7: Get earnings trend (7 months)
     * GET /api/artist/dashboard/earnings-trend
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEarningsTrend(User artist) {
        try {
            log.info("Fetching earnings trend for artist: {}", artist.getEmail());

            List<String> months = new ArrayList<>();
            List<Double> earnings = new ArrayList<>();
            BigDecimal totalEarnings = BigDecimal.ZERO;

            LocalDateTime now = LocalDateTime.now();

            // Get 7 months of data
            for (int i = 6; i >= 0; i--) {
                LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime monthEnd = monthStart.plusMonths(1).minusDays(1).withHour(23).withMinute(59).withSecond(59);

                BigDecimal monthEarnings = paymentRepository.sumEarningsByUserIdAndMonth(artist.getId(), monthStart, monthEnd);

                // Safely handle null values
                if (monthEarnings == null) {
                    monthEarnings = BigDecimal.ZERO;
                }

                String monthName = monthStart.getMonth().toString().substring(0, 3);
                months.add(monthName);
                earnings.add(monthEarnings.doubleValue());
                totalEarnings = totalEarnings.add(monthEarnings);
            }

            double averageMonthlyEarnings = totalEarnings.doubleValue() / 7.0;

            // Find highest earning month
            int maxIndex = 0;
            for (int i = 1; i < earnings.size(); i++) {
                if (earnings.get(i) > earnings.get(maxIndex)) {
                    maxIndex = i;
                }
            }
            String highestMonth = earnings.size() > 0 ? months.get(maxIndex) + " (" + earnings.get(maxIndex) + ")" : "N/A";

            double growthRate = earnings.size() >= 2 ? calculateTrendDouble(earnings.get(6), earnings.get(0)) : 0.0;

            Map<String, Object> trend = new HashMap<>();
            trend.put("months", months);
            trend.put("earnings", earnings);
            trend.put("currency", "INR");
            trend.put("totalEarnings", Math.round(totalEarnings.doubleValue() * 100.0) / 100.0);
            trend.put("averageMonthlyEarnings", Math.round(averageMonthlyEarnings * 100.0) / 100.0);
            trend.put("highestMonth", highestMonth);
            trend.put("growthRate", Math.round(growthRate * 10.0) / 10.0);

            log.info("Earnings trend fetched for artist: {}", artist.getEmail());
            return trend;

        } catch (Exception e) {
            log.error("Error fetching earnings trend for artist {}: {}", artist.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to fetch earnings trend: " + e.getMessage());
        }
    }

    /**
     * ENDPOINT 8: Get recent activity timeline
     * GET /api/artist/dashboard/recent-activity?limit=10
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentActivity(User artist, int limit) {
        try {
            log.info("Fetching recent activity for artist: {} (limit={})", artist.getEmail(), limit);

            ArtistProfile artistProfile = artistProfileRepository.findByUserId(artist.getId())
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));

            List<Map<String, Object>> activities = new ArrayList<>();

            // Get recent applications
            Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<JobApplication> recentApplications = jobApplicationRepository.findRecentByArtistId(artistProfile.getId(), pageable);

            for (JobApplication app : recentApplications) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", "app_" + app.getId());
                activity.put("type", "APPLICATION_SENT");
                activity.put("title", "Applied to " + app.getJob().getTitle());
                activity.put("description", "You applied to " + app.getJob().getTitle());
                activity.put("timestamp", app.getCreatedAt());
                activity.put("icon", "application");
                activity.put("actionUrl", "/artist/applications/" + app.getId());
                activities.add(activity);
            }

            // Get recent notifications
            Page<Notification> recentNotifications = notificationRepository.findRecentByUserId(artist.getId(), PageRequest.of(0, 5));

            for (Notification notif : recentNotifications) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", "notif_" + notif.getId());
                activity.put("type", notif.getType().name());
                activity.put("title", notif.getTitle());
                activity.put("description", notif.getMessage());
                activity.put("timestamp", notif.getSentAt());
                activity.put("icon", "notification");
                activities.add(activity);
            }

            // Sort by timestamp descending
            activities.sort((a, b) -> {
                LocalDateTime timeA = (LocalDateTime) a.get("timestamp");
                LocalDateTime timeB = (LocalDateTime) b.get("timestamp");
                return timeB.compareTo(timeA);
            });

            // Limit results
            List<Map<String, Object>> limitedActivities = activities.stream().limit(limit).collect(Collectors.toList());

            log.info("Recent activity fetched: {} items for artist: {}", limitedActivities.size(), artist.getEmail());
            return limitedActivities;

        } catch (Exception e) {
            log.error("Error fetching recent activity for artist {}: {}", artist.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to fetch recent activity: " + e.getMessage());
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Calculate trend percentage
     */
    private double calculateTrend(long current, long last) {
        if (last == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return Math.round(((current - last) / (double) last) * 1000.0) / 10.0;
    }

    /**
     * Calculate trend for BigDecimal values
     */
    private double calculateTrendBigDecimal(BigDecimal current, BigDecimal last) {
        if (last.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        BigDecimal change = current.subtract(last);
        BigDecimal percentage = change.divide(last, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
        return Math.round(percentage.doubleValue() * 10.0) / 10.0;
    }

    /**
     * Calculate trend for Double values
     */
    private double calculateTrendDouble(double current, double last) {
        if (last == 0.0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return Math.round(((current - last) / last) * 1000.0) / 10.0;
    }

    /**
     * Calculate job match score (0-100)
     * Weights: Skills 40%, Experience 30%, Location 15%, Budget 15%
     */
    private double calculateJobMatchScore(ArtistProfile artist, Job job) {
        double skillsScore = calculateSkillsScore(artist, job);
        double experienceScore = calculateExperienceScore(artist, job);
        double locationScore = calculateLocationScore(artist, job);
        double budgetScore = calculateBudgetScore(artist, job);

        return skillsScore + experienceScore + locationScore + budgetScore;
    }

    /**
     * Calculate skills match score (max 40 points)
     */
    private double calculateSkillsScore(ArtistProfile artist, Job job) {
        try {
            if (artist.getSkills() == null || job.getSkillsRequired() == null) {
                return 0.0;
            }

            List<String> artistSkills = parseJsonArray(artist.getSkills());
            List<String> jobSkills = parseJsonArray(job.getSkillsRequired());

            if (jobSkills.isEmpty()) {
                return 40.0; // If no skills required, full score
            }

            long matchingSkills = artistSkills.stream()
                    .filter(skill -> jobSkills.stream().anyMatch(js -> js.equalsIgnoreCase(skill)))
                    .count();

            return (matchingSkills / (double) jobSkills.size()) * 40.0;

        } catch (Exception e) {
            log.warn("Error calculating skills score: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Calculate experience match score (max 30 points)
     */
    private double calculateExperienceScore(ArtistProfile artist, Job job) {
        if (artist.getExperienceYears() == null || job.getExperienceLevel() == null) {
            return 15.0; // Partial score if missing data
        }

        int artistExperience = artist.getExperienceYears();
        Job.ExperienceLevel requiredLevel = job.getExperienceLevel();

        switch (requiredLevel) {
            case ENTRY_LEVEL:
                return (artistExperience >= 0 && artistExperience <= 2) ? 30.0 : 0.0;
            case MID_LEVEL:
                return (artistExperience >= 3 && artistExperience <= 5) ? 30.0 : 0.0;
            case SENIOR_LEVEL:
                return (artistExperience >= 6) ? 30.0 : 0.0;
            default:
                return 15.0;
        }
    }

    /**
     * Calculate location match score (max 15 points)
     */
    private double calculateLocationScore(ArtistProfile artist, Job job) {
        if (artist.getLocation() == null || job.getLocation() == null) {
            return 0.0;
        }

        String artistLocation = artist.getLocation().toLowerCase();
        String jobLocation = job.getLocation().toLowerCase();

        // Exact match
        if (artistLocation.equals(jobLocation)) {
            return 15.0;
        }

        // Partial match (same city or state)
        if (artistLocation.contains(jobLocation) || jobLocation.contains(artistLocation)) {
            return 7.5;
        }

        // Remote jobs get partial score
        if (job.getIsRemote() != null && job.getIsRemote()) {
            return 10.0;
        }

        return 0.0;
    }

    /**
     * Calculate budget alignment score (max 15 points)
     */
    private double calculateBudgetScore(ArtistProfile artist, Job job) {
        if (artist.getHourlyRate() == null || job.getBudgetMin() == null || job.getBudgetMax() == null) {
            return 7.5; // Partial score if missing data
        }

        // Convert hourly rate to monthly (assuming 160 hours/month)
        BigDecimal monthlyRate = BigDecimal.valueOf(artist.getHourlyRate()).multiply(BigDecimal.valueOf(160));
        BigDecimal jobMin = job.getBudgetMin();
        BigDecimal jobMax = job.getBudgetMax();

        // Within budget range
        if (monthlyRate.compareTo(jobMin) >= 0 && monthlyRate.compareTo(jobMax) <= 0) {
            return 15.0;
        }

        // Within 20% of range
        BigDecimal rangeMargin = jobMax.subtract(jobMin).multiply(BigDecimal.valueOf(0.2));
        BigDecimal lowerBound = jobMin.subtract(rangeMargin);
        BigDecimal upperBound = jobMax.add(rangeMargin);

        if (monthlyRate.compareTo(lowerBound) >= 0 && monthlyRate.compareTo(upperBound) <= 0) {
            return 7.5;
        }

        return 0.0;
    }

    /**
     * Build match reasons for job recommendation
     */
    private List<String> buildMatchReasons(ArtistProfile artist, Job job, double matchScore) {
        List<String> reasons = new ArrayList<>();

        try {
            // Skills match
            if (artist.getSkills() != null && job.getSkillsRequired() != null) {
                List<String> artistSkills = parseJsonArray(artist.getSkills());
                List<String> jobSkills = parseJsonArray(job.getSkillsRequired());
                long matchingSkills = artistSkills.stream()
                        .filter(skill -> jobSkills.stream().anyMatch(js -> js.equalsIgnoreCase(skill)))
                        .count();
                if (matchingSkills > 0) {
                    int percentage = (int) ((matchingSkills / (double) jobSkills.size()) * 100);
                    reasons.add("Skills match: " + matchingSkills + "/" + jobSkills.size() + " (" + percentage + "%)");
                }
            }

            // Experience match
            if (artist.getExperienceYears() != null && job.getExperienceLevel() != null) {
                reasons.add("Experience: " + artist.getExperienceYears() + " years matches " + job.getExperienceLevel());
            }

            // Location match
            if (artist.getLocation() != null && job.getLocation() != null) {
                if (artist.getLocation().equalsIgnoreCase(job.getLocation())) {
                    reasons.add("Location: Exact match");
                } else if (job.getIsRemote() != null && job.getIsRemote()) {
                    reasons.add("Location: Remote-friendly");
                }
            }

            // Budget alignment
            if (artist.getHourlyRate() != null && job.getBudgetMin() != null) {
                reasons.add("Budget aligns with your rate");
            }

        } catch (Exception e) {
            log.warn("Error building match reasons: {}", e.getMessage());
        }

        if (reasons.isEmpty()) {
            reasons.add("Good potential match");
        }

        return reasons;
    }

    /**
     * Parse JSON array string to List<String>
     */
    private List<String> parseJsonArray(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Error parsing JSON array: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}

package com.icastar.platform.service;

import com.icastar.platform.dto.job.CreateJobApplicationDto;
import com.icastar.platform.dto.job.JobApplicationDto;
import com.icastar.platform.dto.job.PublicApplyRequestDto;
import com.icastar.platform.dto.job.UpdateJobApplicationDto;
import com.icastar.platform.entity.Job;
import com.icastar.platform.entity.JobApplication;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.repository.JobApplicationRepository;
import com.icastar.platform.repository.JobRepository;
import com.icastar.platform.repository.ArtistProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final JobService jobService;

    @Transactional(readOnly = true)
    public Optional<JobApplication> findById(Long id) {
        return jobApplicationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findByArtist(ArtistProfile artist) {
        return jobApplicationRepository.findByArtist(artist);
    }

    @Transactional(readOnly = true)
    public Page<JobApplication> findByArtist(ArtistProfile artist, Pageable pageable) {
        return jobApplicationRepository.findByArtist(artist, pageable);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findByJob(Job job) {
        return jobApplicationRepository.findByJob(job);
    }

    @Transactional(readOnly = true)
    public Page<JobApplication> findByJob(Job job, Pageable pageable) {
        return jobApplicationRepository.findByJob(job, pageable);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findByJobId(Long jobId) {
        return jobApplicationRepository.findByJobId(jobId);
    }

    @Transactional(readOnly = true)
    public Page<JobApplication> findByJobId(Long jobId, Pageable pageable) {
        return jobApplicationRepository.findByJobId(jobId, pageable);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findByRecruiterId(Long recruiterId) {
        return jobApplicationRepository.findByRecruiterId(recruiterId);
    }

    @Transactional(readOnly = true)
    public Page<JobApplication> findByRecruiterId(Long recruiterId, Pageable pageable) {
        return jobApplicationRepository.findByRecruiterId(recruiterId, pageable);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findByStatus(JobApplication.ApplicationStatus status) {
        return jobApplicationRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findByArtistAndStatus(ArtistProfile artist, JobApplication.ApplicationStatus status) {
        return jobApplicationRepository.findByArtistAndStatus(artist, status);
    }

    public Page<JobApplication> findByArtistAndStatus(ArtistProfile artist, JobApplication.ApplicationStatus status, Pageable pageable) {
        return jobApplicationRepository.findByArtistAndStatus(artist, status, pageable);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findApplicationsNeedingReview(Long recruiterId) {
        return jobApplicationRepository.findApplicationsNeedingReview(recruiterId);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findUpcomingInterviews() {
        return jobApplicationRepository.findUpcomingInterviews(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findShortlistedApplications() {
        return jobApplicationRepository.findByIsShortlistedTrue();
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findHiredApplications() {
        return jobApplicationRepository.findByIsHiredTrue();
    }

    public JobApplication createApplication(Long artistId, CreateJobApplicationDto createDto) throws BadRequestException {
        if (createDto.getJobId() == null) {
            throw new BadRequestException("Job ID is required");
        }
        ArtistProfile artist = artistProfileRepository.findById(artistId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        Job job = jobRepository.findById(createDto.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Check if already applied to this specific job (artists can apply to multiple different jobs)
        Optional<JobApplication> existingApplication = jobApplicationRepository.findByArtistAndJob(artist, job);
        if (existingApplication.isPresent()) {
            throw new BadRequestException("You have already applied for this job. You can apply to other jobs though.");
        }

        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setArtist(artist);
        application.setCoverLetter(createDto.getCoverLetter());
        application.setExpectedSalary(createDto.getExpectedSalary());
        application.setAvailabilityDate(createDto.getAvailabilityDate());
        application.setPortfolioUrl(createDto.getPortfolioUrl());
        application.setResumeUrl(createDto.getResumeUrl());
        application.setDemoReelUrl(createDto.getDemoReelUrl());
        application.setAppliedAt(LocalDateTime.now());
        application.setStatus(JobApplication.ApplicationStatus.APPLIED);

        JobApplication savedApplication = jobApplicationRepository.save(application);

        // Increment job applications count
        jobService.incrementApplications(job.getId());

        log.info("New job application created: {} applied for job {}", artist.getUser().getEmail(), job.getTitle());
        return savedApplication;
    }

    /**
     * Create a public application from a guest (no login required)
     */
    public JobApplication createPublicApplication(Long jobId, PublicApplyRequestDto applyDto) throws BadRequestException {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Check if job is active and accepting applications
        if (job.getStatus() != Job.JobStatus.ACTIVE) {
            throw new BadRequestException("This job is not currently accepting applications");
        }

        // Check if application deadline has passed
        if (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(java.time.LocalDate.now())) {
            throw new BadRequestException("Application deadline has passed for this job");
        }

        // Check for duplicate application by email or phone
        Optional<JobApplication> existingApplication = jobApplicationRepository
                .findByJobIdAndGuestEmailOrPhone(jobId, applyDto.getEmail(), applyDto.getPhone());
        if (existingApplication.isPresent()) {
            throw new BadRequestException("You have already applied for this job with this email or phone number");
        }

        // Create the application
        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setArtist(null); // Guest application, no artist profile
        application.setGuestFullName(applyDto.getFullName());
        application.setGuestEmail(applyDto.getEmail());
        application.setGuestPhone(applyDto.getPhone());
        application.setGuestAddress(applyDto.getAddress());
        application.setGuestExperienceYears(applyDto.getExperienceYears());
        application.setCoverLetter(applyDto.getCoverLetter());
        application.setExpectedSalary(applyDto.getExpectedSalary());
        application.setSource(JobApplication.ApplicationSource.PUBLIC_LINK);
        application.setAppliedAt(LocalDateTime.now());
        application.setStatus(JobApplication.ApplicationStatus.APPLIED);

        JobApplication savedApplication = jobApplicationRepository.save(application);

        // Increment job applications count
        jobService.incrementApplications(job.getId());

        log.info("New public job application created: {} applied for job {} via public link",
                applyDto.getEmail(), job.getTitle());
        return savedApplication;
    }

    public JobApplication updateApplication(Long applicationId, UpdateJobApplicationDto updateDto) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Job application not found"));

        if (updateDto.getStatus() != null) {
            application.setStatus(updateDto.getStatus());
            application.setReviewedAt(LocalDateTime.now());
        }

        if (updateDto.getInterviewNotes() != null) {
            application.setInterviewNotes(updateDto.getInterviewNotes());
        }

        if (updateDto.getRejectionReason() != null) {
            application.setRejectionReason(updateDto.getRejectionReason());
        }

        if (updateDto.getFeedback() != null) {
            application.setFeedback(updateDto.getFeedback());
        }

        if (updateDto.getRating() != null) {
            application.setRating(updateDto.getRating());
        }

        if (updateDto.getIsShortlisted() != null) {
            application.setIsShortlisted(updateDto.getIsShortlisted());
        }

        if (updateDto.getIsHired() != null) {
            application.setIsHired(updateDto.getIsHired());
            if (updateDto.getIsHired()) {
                application.setHiredAt(LocalDateTime.now());
            }
        }

        if (updateDto.getContractUrl() != null) {
            application.setContractUrl(updateDto.getContractUrl());
        }

        if (updateDto.getNotes() != null) {
            application.setNotes(updateDto.getNotes());
        }

        return jobApplicationRepository.save(application);
    }

    public JobApplication scheduleInterview(Long applicationId, LocalDateTime interviewTime, String notes) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Job application not found"));

        application.setStatus(JobApplication.ApplicationStatus.INTERVIEW_SCHEDULED);
        application.setInterviewScheduledAt(interviewTime);
        application.setInterviewNotes(notes);
        application.setReviewedAt(LocalDateTime.now());

        return jobApplicationRepository.save(application);
    }

    public JobApplication shortlistApplication(Long applicationId, String notes) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Job application not found"));

        application.setStatus(JobApplication.ApplicationStatus.SHORTLISTED);
        application.setIsShortlisted(true);
        application.setReviewedAt(LocalDateTime.now());
        if (notes != null) {
            application.setNotes(notes);
        }

        return jobApplicationRepository.save(application);
    }

    public JobApplication rejectApplication(Long applicationId, String rejectionReason) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Job application not found"));

        application.setStatus(JobApplication.ApplicationStatus.REJECTED);
        application.setRejectionReason(rejectionReason);
        application.setReviewedAt(LocalDateTime.now());

        return jobApplicationRepository.save(application);
    }

    public JobApplication hireApplication(Long applicationId, String contractUrl, String notes) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Job application not found"));

        application.setStatus(JobApplication.ApplicationStatus.HIRED);
        application.setIsHired(true);
        application.setHiredAt(LocalDateTime.now());
        application.setContractUrl(contractUrl);
        if (notes != null) {
            application.setNotes(notes);
        }

        return jobApplicationRepository.save(application);
    }

    public void withdrawApplication(Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Job application not found"));

        application.setStatus(JobApplication.ApplicationStatus.WITHDRAWN);
        jobApplicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public boolean hasAppliedForJob(Long artistId, Long jobId) {
        ArtistProfile artist = artistProfileRepository.findById(artistId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        return jobApplicationRepository.findByArtistAndJob(artist, job).isPresent();
    }

    @Transactional(readOnly = true)
    public Long countApplicationsByArtist(ArtistProfile artist) {
        return jobApplicationRepository.countByArtist(artist);
    }

    @Transactional(readOnly = true)
    public Long countApplicationsByJob(Job job) {
        return jobApplicationRepository.countByJob(job);
    }

    @Transactional(readOnly = true)
    public Long countApplicationsByRecruiter(Long recruiterId) {
        return jobApplicationRepository.countByRecruiterId(recruiterId);
    }

    @Transactional(readOnly = true)
    public Long countApplicationsByRecruiterAndStatus(Long recruiterId, JobApplication.ApplicationStatus status) {
        return jobApplicationRepository.countByRecruiterIdAndStatus(recruiterId, status);
    }

    public Page<JobApplication> findByJob(Long jobId, Pageable pageable) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        return jobApplicationRepository.findByJob(job, pageable);
    }

    public Page<JobApplication> findByArtist(Long artistId, Pageable pageable) {
        ArtistProfile artist = artistProfileRepository.findById(artistId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));
        return jobApplicationRepository.findByArtist(artist, pageable);
    }

    public Page<JobApplication> findByRecruiter(Long recruiterId, Pageable pageable) {
        return jobApplicationRepository.findByRecruiterId(recruiterId, pageable);
    }

    public Page<JobApplication> findAll(Pageable pageable) {
        return jobApplicationRepository.findAll(pageable);
    }

    public JobApplication updateApplicationStatus(Long applicationId, Long recruiterId, com.icastar.platform.dto.application.UpdateApplicationStatusDto updateDto) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Job application not found"));

        // If recruiterId is provided, verify the recruiter owns the job
        if (recruiterId != null && !application.getJob().getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("You can only update applications for your own jobs");
        }

        // Update status
        application.setStatus(updateDto.getStatus());
        
        // Update feedback if provided
        if (updateDto.getFeedback() != null) {
            application.setFeedback(updateDto.getFeedback());
        }
        
        // Update rejection reason if provided
        if (updateDto.getRejectionReason() != null) {
            application.setRejectionReason(updateDto.getRejectionReason());
        }
        
        // Set reviewed timestamp
        application.setReviewedAt(LocalDateTime.now());

        return jobApplicationRepository.save(application);
    }

    public void deleteApplication(Long applicationId, Long recruiterId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Job application not found"));

        // If recruiterId is provided, verify the recruiter owns the job
        if (recruiterId != null && !application.getJob().getRecruiter().getId().equals(recruiterId)) {
            throw new RuntimeException("You can only delete applications for your own jobs");
        }

        // Decrement total applications count for the job
        Job job = application.getJob();
        job.setApplicationsCount(job.getApplicationsCount() - 1);
        jobRepository.save(job);

        // Delete the application
        jobApplicationRepository.delete(application);
    }

    public Long getTotalApplicationsCount() {
        return jobApplicationRepository.count();
    }

    public Long getApplicationsCountByStatus(JobApplication.ApplicationStatus status) {
        return jobApplicationRepository.countByStatus(status);
    }

    public List<JobApplication> getRecentApplications(int limit) {
        return jobApplicationRepository.findRecentApplications(LocalDateTime.now().minusDays(30))
                .stream()
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<JobApplication> findByJobAndStatus(Long jobId, JobApplication.ApplicationStatus status) {
        if (jobId != null) {
            // Find by specific job and status
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found"));
            return jobApplicationRepository.findByJobAndStatus(job, status);
        } else {
            // Find by status only
            return jobApplicationRepository.findByStatus(status);
        }
    }

    public Long getApplicationsCountByRecruiter(Long recruiterId) {
        return jobApplicationRepository.countByRecruiterId(recruiterId);
    }

    public List<JobApplication> findByRecruiterAndStatus(Long recruiterId, JobApplication.ApplicationStatus status) {
        return jobApplicationRepository.findByRecruiterIdAndStatus(recruiterId, status);
    }
}
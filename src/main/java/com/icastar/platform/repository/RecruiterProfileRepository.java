package com.icastar.platform.repository;

import com.icastar.platform.entity.RecruiterProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {

    Optional<RecruiterProfile> findByUserId(Long userId);

    List<RecruiterProfile> findByIsVerifiedCompany(Boolean isVerifiedCompany);

    List<RecruiterProfile> findByCompanyNameContainingIgnoreCase(String companyName);

    List<RecruiterProfile> findByIndustryContainingIgnoreCase(String industry);

    List<RecruiterProfile> findByLocationContainingIgnoreCase(String location);

    @Query("SELECT rp FROM RecruiterProfile rp WHERE rp.companySize = :companySize")
    List<RecruiterProfile> findByCompanySize(@Param("companySize") String companySize);

    @Query("SELECT rp FROM RecruiterProfile rp ORDER BY rp.successfulHires DESC")
    Page<RecruiterProfile> findTopRecruitersByHires(Pageable pageable);

    @Query("SELECT rp FROM RecruiterProfile rp ORDER BY rp.totalJobsPosted DESC")
    Page<RecruiterProfile> findTopRecruitersByJobsPosted(Pageable pageable);

    @Query("SELECT rp FROM RecruiterProfile rp WHERE rp.user.status = 'ACTIVE' AND rp.isActive = true")
    List<RecruiterProfile> findActiveRecruiters();

    @Query("SELECT rp FROM RecruiterProfile rp WHERE rp.user.status = 'ACTIVE' AND rp.isActive = true AND rp.isVerifiedCompany = true")
    List<RecruiterProfile> findActiveVerifiedRecruiters();

    @Query("SELECT rp FROM RecruiterProfile rp WHERE rp.chatCredits > 0")
    List<RecruiterProfile> findRecruitersWithChatCredits();

    @Query("SELECT rp FROM RecruiterProfile rp WHERE rp.companyName LIKE %:searchTerm% OR rp.contactPersonName LIKE %:searchTerm%")
    List<RecruiterProfile> findByCompanyNameOrContactPersonContaining(@Param("searchTerm") String searchTerm);

    // Find all recruiters for admin (excludes soft-deleted users)
    @Query("SELECT rp FROM RecruiterProfile rp WHERE rp.user.deactivationReason IS NULL OR rp.user.deactivationReason NOT LIKE 'DELETED%'")
    Page<RecruiterProfile> findAllExcludingDeleted(Pageable pageable);
}

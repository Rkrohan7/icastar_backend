package com.icastar.platform.repository;

import com.icastar.platform.entity.ArtistProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {

    Optional<ArtistProfile> findByUserId(Long userId);

    List<ArtistProfile> findByIsVerifiedBadge(Boolean isVerifiedBadge);

    List<ArtistProfile> findByLocationContainingIgnoreCase(String location);

    @Query("SELECT ap FROM ArtistProfile ap WHERE ap.skills LIKE %:skill%")
    List<ArtistProfile> findBySkillsContaining(@Param("skill") String skill);

    @Query("SELECT ap FROM ArtistProfile ap WHERE ap.experienceYears >= :minExperience")
    List<ArtistProfile> findByExperienceYearsGreaterThanEqual(@Param("minExperience") Integer minExperience);

    @Query("SELECT ap FROM ArtistProfile ap WHERE ap.hourlyRate BETWEEN :minRate AND :maxRate")
    List<ArtistProfile> findByHourlyRateBetween(@Param("minRate") Double minRate, 
                                               @Param("maxRate") Double maxRate);

    @Query("SELECT ap FROM ArtistProfile ap WHERE ap.verificationRequestedAt IS NOT NULL AND ap.isVerifiedBadge = false")
    List<ArtistProfile> findPendingVerificationRequests();

    @Query("SELECT ap FROM ArtistProfile ap WHERE ap.stageName LIKE %:stageName% OR ap.user.firstName LIKE %:firstName% OR ap.user.lastName LIKE %:lastName%")
    List<ArtistProfile> findByStageNameOrNameContaining(@Param("stageName") String stageName, 
                                                       @Param("firstName") String firstName, 
                                                       @Param("lastName") String lastName);

    @Query("SELECT ap FROM ArtistProfile ap ORDER BY ap.successfulHires DESC")
    Page<ArtistProfile> findTopArtistsByHires(Pageable pageable);

    @Query("SELECT ap FROM ArtistProfile ap WHERE ap.user.status = 'ACTIVE' AND ap.isActive = true")
    List<ArtistProfile> findActiveArtists();

    @Query("SELECT ap FROM ArtistProfile ap WHERE ap.user.status = 'ACTIVE' AND ap.isActive = true")
    Page<ArtistProfile> findActiveArtistsPageable(Pageable pageable);

    @Query("SELECT ap FROM ArtistProfile ap WHERE ap.user.status = 'ACTIVE' AND ap.isActive = true AND ap.isVerifiedBadge = true")
    List<ArtistProfile> findActiveVerifiedArtists();

    @Query("SELECT ap FROM ArtistProfile ap WHERE ap.user.status = 'ACTIVE' AND ap.isActive = true ORDER BY ap.totalApplications DESC")
    Page<ArtistProfile> findMostActiveArtists(Pageable pageable);

    List<ArtistProfile> findByArtistTypeId(Long artistTypeId);

    List<ArtistProfile> findByArtistTypeName(String artistTypeName);

    @Query("SELECT ap FROM ArtistProfile ap WHERE ap.artistType.id = :artistTypeId AND ap.user.status = 'ACTIVE' AND ap.isActive = true")
    List<ArtistProfile> findActiveArtistsByType(@Param("artistTypeId") Long artistTypeId);

    @Query("SELECT ap FROM ArtistProfile ap WHERE ap.artistType.name = :artistTypeName AND ap.user.status = 'ACTIVE' AND ap.isActive = true")
    List<ArtistProfile> findActiveArtistsByTypeName(@Param("artistTypeName") String artistTypeName);

    @Query("SELECT ap FROM ArtistProfile ap WHERE " +
           "LOWER(ap.stageName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ap.user.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ap.user.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ap.skills) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ap.bio) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ArtistProfile> findBySearchTerm(@Param("searchTerm") String searchTerm);

    // Count methods for Super Admin
    @Query("SELECT COUNT(ap) FROM ArtistProfile ap WHERE ap.artistType.id = :artistTypeId")
    Long countByArtistTypeId(@Param("artistTypeId") Long artistTypeId);
}

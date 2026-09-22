package com.icastar.platform.repository;

import com.icastar.platform.entity.CastingProject;
import com.icastar.platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CastingProjectRepository extends JpaRepository<CastingProject, Long> {

    // Find projects by recruiter
    List<CastingProject> findByRecruiter(User recruiter);
    Page<CastingProject> findByRecruiter(User recruiter, Pageable pageable);

    // Find projects by recruiter ID
    @Query("SELECT p FROM CastingProject p WHERE p.recruiter.id = :recruiterId")
    List<CastingProject> findByRecruiterId(@Param("recruiterId") Long recruiterId);

    @Query("SELECT p FROM CastingProject p WHERE p.recruiter.id = :recruiterId")
    Page<CastingProject> findByRecruiterId(@Param("recruiterId") Long recruiterId, Pageable pageable);

    // Find project by ID with characters loaded
    @Query("SELECT p FROM CastingProject p LEFT JOIN FETCH p.characters WHERE p.id = :projectId")
    Optional<CastingProject> findByIdWithCharacters(@Param("projectId") Long projectId);

    // Find project by ID with recruiter loaded
    @Query("SELECT p FROM CastingProject p LEFT JOIN FETCH p.recruiter WHERE p.id = :projectId")
    Optional<CastingProject> findByIdWithRecruiter(@Param("projectId") Long projectId);

    // Find projects by status
    List<CastingProject> findByStatus(CastingProject.ProjectStatus status);
    Page<CastingProject> findByStatus(CastingProject.ProjectStatus status, Pageable pageable);

    // Find projects by recruiter and status
    @Query("SELECT p FROM CastingProject p WHERE p.recruiter.id = :recruiterId AND p.status = :status")
    List<CastingProject> findByRecruiterIdAndStatus(
            @Param("recruiterId") Long recruiterId,
            @Param("status") CastingProject.ProjectStatus status);

    // Count projects by recruiter
    Long countByRecruiter(User recruiter);

    @Query("SELECT COUNT(p) FROM CastingProject p WHERE p.recruiter.id = :recruiterId")
    Long countByRecruiterId(@Param("recruiterId") Long recruiterId);

    // Check if project belongs to recruiter
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM CastingProject p WHERE p.id = :projectId AND p.recruiter.id = :recruiterId")
    boolean existsByIdAndRecruiterId(@Param("projectId") Long projectId, @Param("recruiterId") Long recruiterId);
}
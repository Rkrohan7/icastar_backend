package com.icastar.platform.repository;

import com.icastar.platform.entity.CastingCharacter;
import com.icastar.platform.entity.CastingProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CastingCharacterRepository extends JpaRepository<CastingCharacter, Long> {

    // Find characters by project
    List<CastingCharacter> findByProject(CastingProject project);
    Page<CastingCharacter> findByProject(CastingProject project, Pageable pageable);

    // Find characters by project ID
    @Query("SELECT c FROM CastingCharacter c WHERE c.project.id = :projectId")
    List<CastingCharacter> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT c FROM CastingCharacter c WHERE c.project.id = :projectId")
    Page<CastingCharacter> findByProjectId(@Param("projectId") Long projectId, Pageable pageable);

    // Find character by ID with project loaded
    @Query("SELECT c FROM CastingCharacter c LEFT JOIN FETCH c.project WHERE c.id = :characterId")
    Optional<CastingCharacter> findByIdWithProject(@Param("characterId") Long characterId);

    // Find characters by role type
    List<CastingCharacter> findByRoleType(CastingCharacter.RoleType roleType);

    // Find characters by project and role type
    @Query("SELECT c FROM CastingCharacter c WHERE c.project.id = :projectId AND c.roleType = :roleType")
    List<CastingCharacter> findByProjectIdAndRoleType(
            @Param("projectId") Long projectId,
            @Param("roleType") CastingCharacter.RoleType roleType);

    // Count characters by project
    Long countByProject(CastingProject project);

    @Query("SELECT COUNT(c) FROM CastingCharacter c WHERE c.project.id = :projectId")
    Long countByProjectId(@Param("projectId") Long projectId);

    // Check if character belongs to project
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CastingCharacter c WHERE c.id = :characterId AND c.project.id = :projectId")
    boolean existsByIdAndProjectId(@Param("characterId") Long characterId, @Param("projectId") Long projectId);

    // Check if character belongs to a recruiter's project
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CastingCharacter c WHERE c.id = :characterId AND c.project.recruiter.id = :recruiterId")
    boolean existsByIdAndRecruiterId(@Param("characterId") Long characterId, @Param("recruiterId") Long recruiterId);
}
package com.icastar.platform.repository;

import com.icastar.platform.entity.ArtistEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistEducationRepository extends JpaRepository<ArtistEducation, Long> {

    List<ArtistEducation> findByArtistProfileIdOrderByEndYearDesc(Long artistProfileId);

    Optional<ArtistEducation> findByIdAndArtistProfileId(Long id, Long artistProfileId);

    @Modifying
    @Query("DELETE FROM ArtistEducation ae WHERE ae.artistProfile.id = :artistProfileId")
    void deleteAllByArtistProfileId(@Param("artistProfileId") Long artistProfileId);

    @Query("SELECT ae FROM ArtistEducation ae WHERE ae.artistProfile.id = :artistProfileId ORDER BY ae.endYear DESC NULLS FIRST")
    List<ArtistEducation> findByArtistProfileIdOrderByEndYearDescNullsFirst(@Param("artistProfileId") Long artistProfileId);
}
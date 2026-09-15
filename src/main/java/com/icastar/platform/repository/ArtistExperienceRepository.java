package com.icastar.platform.repository;

import com.icastar.platform.entity.ArtistExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistExperienceRepository extends JpaRepository<ArtistExperience, Long> {

    List<ArtistExperience> findByArtistProfileIdOrderByStartDateDesc(Long artistProfileId);

    List<ArtistExperience> findByArtistProfileIdAndArtistTypeIdOrderByStartDateDesc(Long artistProfileId, Long artistTypeId);

    Optional<ArtistExperience> findByIdAndArtistProfileId(Long id, Long artistProfileId);

    @Modifying
    @Query("DELETE FROM ArtistExperience ae WHERE ae.artistProfile.id = :artistProfileId")
    void deleteAllByArtistProfileId(@Param("artistProfileId") Long artistProfileId);

    @Query("SELECT ae FROM ArtistExperience ae WHERE ae.artistProfile.id = :artistProfileId AND ae.artistType.id = :artistTypeId")
    List<ArtistExperience> findByArtistProfileIdAndArtistTypeId(@Param("artistProfileId") Long artistProfileId,
                                                                  @Param("artistTypeId") Long artistTypeId);
}

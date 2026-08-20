package com.icastar.platform.repository;

import com.icastar.platform.entity.ArtistProfileArtistType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistProfileArtistTypeRepository extends JpaRepository<ArtistProfileArtistType, Long> {

    List<ArtistProfileArtistType> findByArtistProfileIdOrderBySortOrder(Long artistProfileId);

    Optional<ArtistProfileArtistType> findByArtistProfileIdAndIsPrimaryTrue(Long artistProfileId);

    List<ArtistProfileArtistType> findByArtistTypeId(Long artistTypeId);

    @Query("SELECT apat FROM ArtistProfileArtistType apat WHERE apat.artistProfile.id = :artistProfileId ORDER BY apat.sortOrder")
    List<ArtistProfileArtistType> findAllByArtistProfileId(@Param("artistProfileId") Long artistProfileId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ArtistProfileArtistType apat WHERE apat.artistProfile.id = :artistProfileId")
    void deleteAllByArtistProfileId(@Param("artistProfileId") Long artistProfileId);

    boolean existsByArtistProfileIdAndArtistTypeId(Long artistProfileId, Long artistTypeId);

    @Query("SELECT COUNT(apat) FROM ArtistProfileArtistType apat WHERE apat.artistType.id = :artistTypeId")
    Long countByArtistTypeId(@Param("artistTypeId") Long artistTypeId);
}
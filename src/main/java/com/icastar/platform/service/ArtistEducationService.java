package com.icastar.platform.service;

import com.icastar.platform.config.CacheNames;
import com.icastar.platform.dto.artist.EducationDto;
import com.icastar.platform.entity.ArtistEducation;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.repository.ArtistEducationRepository;
import com.icastar.platform.repository.ArtistProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtistEducationService {

    private final ArtistEducationRepository educationRepository;
    private final ArtistProfileRepository artistProfileRepository;

    /**
     * Get all educations for an artist profile (sorted by endYear DESC)
     */
    @Transactional(readOnly = true)
    public List<EducationDto> getEducationsByArtistProfileId(Long artistProfileId) {
        List<ArtistEducation> educations = educationRepository.findByArtistProfileIdOrderByEndYearDescNullsFirst(artistProfileId);
        return educations.stream()
                .map(EducationDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Create a new education entry
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_USER, allEntries = true),
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_ID, allEntries = true)
    })
    public EducationDto createEducation(Long artistProfileId, EducationDto dto) {
        ArtistProfile artistProfile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        // Validate
        validateEducationDto(dto);

        ArtistEducation education = new ArtistEducation();
        mapDtoToEntity(dto, education, artistProfile);

        education = educationRepository.save(education);
        log.info("Created education ID: {} for artist profile ID: {}", education.getId(), artistProfileId);

        return EducationDto.fromEntity(education);
    }

    /**
     * Update an existing education entry
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_USER, allEntries = true),
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_ID, allEntries = true)
    })
    public EducationDto updateEducation(Long artistProfileId, Long educationId, EducationDto dto) {
        ArtistEducation education = educationRepository.findByIdAndArtistProfileId(educationId, artistProfileId)
                .orElseThrow(() -> new RuntimeException("Education not found or does not belong to this artist"));

        ArtistProfile artistProfile = education.getArtistProfile();

        // Validate
        validateEducationDto(dto);

        mapDtoToEntity(dto, education, artistProfile);

        education = educationRepository.save(education);
        log.info("Updated education ID: {} for artist profile ID: {}", educationId, artistProfileId);

        return EducationDto.fromEntity(education);
    }

    /**
     * Delete an education entry
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_USER, allEntries = true),
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_ID, allEntries = true)
    })
    public void deleteEducation(Long artistProfileId, Long educationId) {
        ArtistEducation education = educationRepository.findByIdAndArtistProfileId(educationId, artistProfileId)
                .orElseThrow(() -> new RuntimeException("Education not found or does not belong to this artist"));

        educationRepository.delete(education);
        log.info("Deleted education ID: {} for artist profile ID: {}", educationId, artistProfileId);
    }

    /**
     * Save multiple educations during onboarding
     * Deletes existing educations first to avoid duplicates on re-submit
     */
    @Transactional
    public List<EducationDto> saveEducationsForOnboarding(ArtistProfile artistProfile, List<EducationDto> educationDtos) {
        // Delete existing educations first (handles re-submit case)
        educationRepository.deleteAllByArtistProfileId(artistProfile.getId());

        // If empty list, just return
        if (educationDtos == null || educationDtos.isEmpty()) {
            return new ArrayList<>();
        }

        List<ArtistEducation> savedEducations = new ArrayList<>();

        for (EducationDto dto : educationDtos) {
            // Validate
            validateEducationDto(dto);

            ArtistEducation education = new ArtistEducation();
            mapDtoToEntity(dto, education, artistProfile);

            savedEducations.add(educationRepository.save(education));
        }

        log.info("Saved {} educations for artist profile ID: {} during onboarding", savedEducations.size(), artistProfile.getId());

        return savedEducations.stream()
                .map(EducationDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Validate education DTO
     */
    private void validateEducationDto(EducationDto dto) {
        if (dto.getEducationLevel() == null || dto.getEducationLevel().trim().isEmpty()) {
            throw new RuntimeException("Education level is required");
        }

        // Validate education level is valid enum value
        try {
            ArtistEducation.EducationLevel.valueOf(dto.getEducationLevel());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid education level: " + dto.getEducationLevel());
        }

        if (dto.getCourseName() == null || dto.getCourseName().trim().isEmpty()) {
            throw new RuntimeException("Course name is required");
        }
        if (dto.getInstitution() == null || dto.getInstitution().trim().isEmpty()) {
            throw new RuntimeException("Institution is required");
        }

        // Treat null isPursuing as false
        boolean isPursuing = Boolean.TRUE.equals(dto.getIsPursuing());

        if (!isPursuing) {
            if (dto.getEndYear() == null) {
                throw new RuntimeException("End year is required when not currently pursuing");
            }
        }

        // Validate year range
        if (dto.getStartYear() != null && dto.getEndYear() != null) {
            if (dto.getEndYear() < dto.getStartYear()) {
                throw new RuntimeException("End year must be greater than or equal to start year");
            }
        }

        // Validate years are reasonable (not in distant future)
        int currentYear = LocalDate.now().getYear();
        if (dto.getStartYear() != null && dto.getStartYear() > currentYear + 1) {
            throw new RuntimeException("Start year cannot be more than 1 year in the future");
        }
        if (dto.getEndYear() != null && dto.getEndYear() > currentYear + 10) {
            throw new RuntimeException("End year cannot be more than 10 years in the future");
        }
    }

    /**
     * Map DTO to Entity
     */
    private void mapDtoToEntity(EducationDto dto, ArtistEducation entity, ArtistProfile artistProfile) {
        entity.setArtistProfile(artistProfile);

        if (dto.getEducationLevel() != null && !dto.getEducationLevel().isEmpty()) {
            try {
                entity.setEducationLevel(ArtistEducation.EducationLevel.valueOf(dto.getEducationLevel()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid education level: {}, setting to OTHER", dto.getEducationLevel());
                entity.setEducationLevel(ArtistEducation.EducationLevel.OTHER);
            }
        }

        entity.setCourseName(dto.getCourseName().trim());
        entity.setSpecialization(dto.getSpecialization() != null ? dto.getSpecialization().trim() : null);
        entity.setInstitution(dto.getInstitution().trim());

        if (dto.getCourseType() != null && !dto.getCourseType().isEmpty()) {
            try {
                entity.setCourseType(ArtistEducation.CourseType.valueOf(dto.getCourseType()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid course type: {}, setting to null", dto.getCourseType());
                entity.setCourseType(null);
            }
        } else {
            entity.setCourseType(null);
        }

        entity.setIsPursuing(dto.getIsPursuing() != null ? dto.getIsPursuing() : false);
        entity.setStartYear(dto.getStartYear());
        entity.setEndYear(Boolean.TRUE.equals(dto.getIsPursuing()) ? null : dto.getEndYear());
        entity.setGrade(dto.getGrade() != null ? dto.getGrade().trim() : null);
        entity.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
    }

    /**
     * Check if education belongs to the given artist profile
     */
    @Transactional(readOnly = true)
    public boolean isEducationOwnedByArtist(Long educationId, Long artistProfileId) {
        return educationRepository.findByIdAndArtistProfileId(educationId, artistProfileId).isPresent();
    }
}

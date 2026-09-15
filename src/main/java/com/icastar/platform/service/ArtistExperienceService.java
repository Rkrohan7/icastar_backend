package com.icastar.platform.service;

import com.icastar.platform.config.CacheNames;
import com.icastar.platform.dto.artist.ExperienceDto;
import com.icastar.platform.entity.ArtistExperience;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.ArtistProfileArtistType;
import com.icastar.platform.entity.ArtistType;
import com.icastar.platform.repository.ArtistExperienceRepository;
import com.icastar.platform.repository.ArtistProfileArtistTypeRepository;
import com.icastar.platform.repository.ArtistProfileRepository;
import com.icastar.platform.repository.ArtistTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtistExperienceService {

    private final ArtistExperienceRepository experienceRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistTypeRepository artistTypeRepository;
    private final ArtistProfileArtistTypeRepository artistProfileArtistTypeRepository;

    /**
     * Get all experiences for an artist profile
     */
    @Transactional(readOnly = true)
    public List<ExperienceDto> getExperiencesByArtistProfileId(Long artistProfileId) {
        List<ArtistExperience> experiences = experienceRepository.findByArtistProfileIdOrderByStartDateDesc(artistProfileId);
        return experiences.stream()
                .map(ExperienceDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Create a new experience entry
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_USER, allEntries = true),
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_ID, allEntries = true)
    })
    public ExperienceDto createExperience(Long artistProfileId, ExperienceDto dto) {
        ArtistProfile artistProfile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        // Validate
        validateExperienceDto(dto, artistProfile);

        ArtistExperience experience = new ArtistExperience();
        mapDtoToEntity(dto, experience, artistProfile);

        experience = experienceRepository.save(experience);
        log.info("Created experience ID: {} for artist profile ID: {}", experience.getId(), artistProfileId);

        // Recalculate experience years
        recalculateExperienceYears(artistProfile);

        return ExperienceDto.fromEntity(experience);
    }

    /**
     * Update an existing experience entry
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_USER, allEntries = true),
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_ID, allEntries = true)
    })
    public ExperienceDto updateExperience(Long artistProfileId, Long experienceId, ExperienceDto dto) {
        ArtistExperience experience = experienceRepository.findByIdAndArtistProfileId(experienceId, artistProfileId)
                .orElseThrow(() -> new RuntimeException("Experience not found or does not belong to this artist"));

        ArtistProfile artistProfile = experience.getArtistProfile();

        // Validate
        validateExperienceDto(dto, artistProfile);

        mapDtoToEntity(dto, experience, artistProfile);

        experience = experienceRepository.save(experience);
        log.info("Updated experience ID: {} for artist profile ID: {}", experienceId, artistProfileId);

        // Recalculate experience years
        recalculateExperienceYears(artistProfile);

        return ExperienceDto.fromEntity(experience);
    }

    /**
     * Delete an experience entry
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_USER, allEntries = true),
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_ID, allEntries = true)
    })
    public void deleteExperience(Long artistProfileId, Long experienceId) {
        ArtistExperience experience = experienceRepository.findByIdAndArtistProfileId(experienceId, artistProfileId)
                .orElseThrow(() -> new RuntimeException("Experience not found or does not belong to this artist"));

        ArtistProfile artistProfile = experience.getArtistProfile();

        experienceRepository.delete(experience);
        log.info("Deleted experience ID: {} for artist profile ID: {}", experienceId, artistProfileId);

        // Recalculate experience years
        recalculateExperienceYears(artistProfile);
    }

    /**
     * Save multiple experiences during onboarding
     * Deletes existing experiences first to avoid duplicates on re-submit
     */
    @Transactional
    public List<ExperienceDto> saveExperiencesForOnboarding(ArtistProfile artistProfile, List<ExperienceDto> experienceDtos) {
        // Delete existing experiences first (handles re-submit case)
        experienceRepository.deleteAllByArtistProfileId(artistProfile.getId());

        // If empty list, just recalculate and return
        if (experienceDtos == null || experienceDtos.isEmpty()) {
            recalculateExperienceYears(artistProfile);
            return new ArrayList<>();
        }

        List<ArtistExperience> savedExperiences = new ArrayList<>();

        for (ExperienceDto dto : experienceDtos) {
            // Validate but skip artistTypeId validation during onboarding since professions might not be set yet
            validateExperienceDtoBasic(dto);

            ArtistExperience experience = new ArtistExperience();
            mapDtoToEntity(dto, experience, artistProfile);

            savedExperiences.add(experienceRepository.save(experience));
        }

        log.info("Saved {} experiences for artist profile ID: {} during onboarding", savedExperiences.size(), artistProfile.getId());

        // Recalculate experience years
        recalculateExperienceYears(artistProfile);

        return savedExperiences.stream()
                .map(ExperienceDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Validate experience DTO
     */
    private void validateExperienceDto(ExperienceDto dto, ArtistProfile artistProfile) {
        validateExperienceDtoBasic(dto);

        // Validate artistTypeId belongs to artist's professions
        // Also accept primary artistType for old single-profession artists without join table entries
        if (dto.getArtistTypeId() != null) {
            boolean isValidProfession = artistProfileArtistTypeRepository
                    .existsByArtistProfileIdAndArtistTypeId(artistProfile.getId(), dto.getArtistTypeId())
                    || (artistProfile.getArtistType() != null
                        && artistProfile.getArtistType().getId().equals(dto.getArtistTypeId()));
            if (!isValidProfession) {
                throw new RuntimeException("Artist type ID " + dto.getArtistTypeId() + " is not one of the artist's selected professions");
            }
        }
    }

    /**
     * Basic validation without artistTypeId check
     */
    private void validateExperienceDtoBasic(ExperienceDto dto) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Title is required");
        }
        if (dto.getCompanyName() == null || dto.getCompanyName().trim().isEmpty()) {
            throw new RuntimeException("Company name is required");
        }
        if (dto.getStartDate() == null) {
            throw new RuntimeException("Start date is required");
        }
        if (dto.getStartDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("Start date cannot be in the future");
        }

        // Treat null isCurrent as false
        boolean isCurrent = Boolean.TRUE.equals(dto.getIsCurrent());

        if (!isCurrent) {
            if (dto.getEndDate() == null) {
                throw new RuntimeException("End date is required when not currently working");
            }
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new RuntimeException("End date must be after or equal to start date");
            }
            // End date should not be in the future
            if (dto.getEndDate().isAfter(LocalDate.now())) {
                throw new RuntimeException("End date cannot be in the future");
            }
        }
        if (isCurrent && dto.getEndDate() != null) {
            throw new RuntimeException("End date must be null when currently working");
        }
    }

    /**
     * Map DTO to Entity
     */
    private void mapDtoToEntity(ExperienceDto dto, ArtistExperience entity, ArtistProfile artistProfile) {
        entity.setArtistProfile(artistProfile);

        if (dto.getArtistTypeId() != null) {
            ArtistType artistType = artistTypeRepository.findById(dto.getArtistTypeId())
                    .orElseThrow(() -> new RuntimeException("Artist type not found: " + dto.getArtistTypeId()));
            entity.setArtistType(artistType);
        } else {
            entity.setArtistType(null);
        }

        entity.setTitle(dto.getTitle().trim());
        entity.setCompanyName(dto.getCompanyName().trim());
        entity.setProjectType(dto.getProjectType() != null ? dto.getProjectType().trim() : null);

        if (dto.getEmploymentType() != null && !dto.getEmploymentType().isEmpty()) {
            try {
                entity.setEmploymentType(ArtistExperience.EmploymentType.valueOf(dto.getEmploymentType()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid employment type: {}, setting to null", dto.getEmploymentType());
                entity.setEmploymentType(null);
            }
        } else {
            entity.setEmploymentType(null);
        }

        entity.setLocation(dto.getLocation() != null ? dto.getLocation().trim() : null);
        entity.setIsCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(Boolean.TRUE.equals(dto.getIsCurrent()) ? null : dto.getEndDate());
        entity.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
    }

    /**
     * Recalculate experience years after any experience change
     * - Total experience years on artist_profiles.experience_years
     * - Per profession experience years on artist_profile_artist_types.experience_years
     */
    @Transactional
    public void recalculateExperienceYears(ArtistProfile artistProfile) {
        List<ArtistExperience> allExperiences = experienceRepository
                .findByArtistProfileIdOrderByStartDateDesc(artistProfile.getId());

        // Calculate total experience years (all experiences merged)
        int totalMonths = calculateTotalMonthsFromExperiences(allExperiences);
        int totalYears = totalMonths / 12;
        artistProfile.setExperienceYears(totalYears);
        artistProfileRepository.save(artistProfile);
        log.info("Updated total experience years to {} for artist profile ID: {}", totalYears, artistProfile.getId());

        // Calculate per-profession experience years
        List<ArtistProfileArtistType> professions = artistProfileArtistTypeRepository
                .findByArtistProfileIdOrderBySortOrder(artistProfile.getId());

        for (ArtistProfileArtistType profession : professions) {
            List<ArtistExperience> professionExperiences = allExperiences.stream()
                    .filter(exp -> exp.getArtistType() != null &&
                            exp.getArtistType().getId().equals(profession.getArtistType().getId()))
                    .collect(Collectors.toList());

            int professionMonths = calculateTotalMonthsFromExperiences(professionExperiences);
            int professionYears = professionMonths / 12;
            profession.setExperienceYears(professionYears);
            artistProfileArtistTypeRepository.save(profession);
            log.debug("Updated experience years to {} for profession {} of artist profile ID: {}",
                    professionYears, profession.getArtistType().getName(), artistProfile.getId());
        }
    }

    /**
     * Calculate total months from a list of experiences by merging overlapping ranges
     * Uses month-level precision (date normalized to 1st of month)
     * For current experiences, end = current month
     */
    private int calculateTotalMonthsFromExperiences(List<ArtistExperience> experiences) {
        if (experiences == null || experiences.isEmpty()) {
            return 0;
        }

        // Convert experiences to YearMonth intervals
        List<int[]> intervals = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (ArtistExperience exp : experiences) {
            YearMonth start = YearMonth.from(exp.getStartDate());
            YearMonth end;

            if (Boolean.TRUE.equals(exp.getIsCurrent()) || exp.getEndDate() == null) {
                end = currentMonth;
            } else {
                end = YearMonth.from(exp.getEndDate());
            }

            // Convert to months since epoch for easier calculation
            int startMonths = start.getYear() * 12 + start.getMonthValue();
            int endMonths = end.getYear() * 12 + end.getMonthValue();

            // End should be at least equal to start
            if (endMonths >= startMonths) {
                intervals.add(new int[]{startMonths, endMonths});
            }
        }

        if (intervals.isEmpty()) {
            return 0;
        }

        // Sort by start time
        intervals.sort(Comparator.comparingInt(a -> a[0]));

        // Merge overlapping intervals
        List<int[]> merged = new ArrayList<>();
        int[] current = intervals.get(0);

        for (int i = 1; i < intervals.size(); i++) {
            int[] next = intervals.get(i);
            if (next[0] <= current[1] + 1) {
                // Overlapping or adjacent, merge
                current[1] = Math.max(current[1], next[1]);
            } else {
                // Non-overlapping, add current to result and start new
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        // Calculate total months (inclusive)
        int totalMonths = 0;
        for (int[] interval : merged) {
            // +1 because both start and end months are inclusive
            totalMonths += (interval[1] - interval[0] + 1);
        }

        return totalMonths;
    }

    /**
     * Check if experience belongs to the given artist profile
     */
    @Transactional(readOnly = true)
    public boolean isExperienceOwnedByArtist(Long experienceId, Long artistProfileId) {
        return experienceRepository.findByIdAndArtistProfileId(experienceId, artistProfileId).isPresent();
    }
}
package com.icastar.platform.service;

import com.icastar.platform.dto.ArtistProfileFieldDto;
import com.icastar.platform.dto.user.UpdateUserProfileDto;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.ArtistProfileField;
import com.icastar.platform.entity.ArtistType;
import com.icastar.platform.entity.ArtistTypeField;
import com.icastar.platform.entity.User;
import com.icastar.platform.repository.ArtistProfileFieldRepository;
import com.icastar.platform.repository.ArtistProfileRepository;
import com.icastar.platform.repository.ArtistTypeFieldRepository;
import com.icastar.platform.repository.ArtistTypeRepository;
import com.icastar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArtistService {

    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistTypeRepository artistTypeRepository;
    private final UserRepository userRepository;
    private final ArtistProfileFieldRepository artistProfileFieldRepository;
    private final ArtistTypeFieldRepository artistTypeFieldRepository;

    @Transactional(readOnly = true)
    public Optional<ArtistProfile> findById(Long id) {
        return artistProfileRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ArtistProfile> findByUserId(Long userId) {
        return artistProfileRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ArtistProfile> findActiveArtists() {
        return artistProfileRepository.findActiveArtists();
    }

    @Transactional(readOnly = true)
    public List<ArtistProfile> findActiveVerifiedArtists() {
        return artistProfileRepository.findActiveVerifiedArtists();
    }

    @Transactional(readOnly = true)
    public Page<ArtistProfile> findMostActiveArtists(Pageable pageable) {
        return artistProfileRepository.findMostActiveArtists(pageable);
    }

    @Transactional(readOnly = true)
    public List<ArtistProfile> findByArtistTypeId(Long artistTypeId) {
        return artistProfileRepository.findByArtistTypeId(artistTypeId);
    }

    @Transactional(readOnly = true)
    public List<ArtistProfile> findByArtistTypeName(String artistTypeName) {
        return artistProfileRepository.findByArtistTypeName(artistTypeName);
    }

    @Transactional(readOnly = true)
    public List<ArtistProfile> findActiveArtistsByType(Long artistTypeId) {
        return artistProfileRepository.findActiveArtistsByType(artistTypeId);
    }

    @Transactional(readOnly = true)
    public List<ArtistProfile> findActiveArtistsByTypeName(String artistTypeName) {
        return artistProfileRepository.findActiveArtistsByTypeName(artistTypeName);
    }

    public ArtistProfile createArtistProfile(Long userId, Long artistTypeId, String firstName, String lastName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ArtistType artistType = artistTypeRepository.findById(artistTypeId)
                .orElseThrow(() -> new RuntimeException("Artist type not found"));

        ArtistProfile artistProfile = new ArtistProfile();
        artistProfile.setUser(user);
        artistProfile.setArtistType(artistType);
        artistProfile.setFirstName(firstName);
        artistProfile.setLastName(lastName);

        return artistProfileRepository.save(artistProfile);
    }

    public ArtistProfile updateArtistProfile(Long artistProfileId, ArtistProfile updatedProfile) {
        ArtistProfile existingProfile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        // Note: firstName and lastName are in User entity, not ArtistProfile
        // They should be updated through UserService or a separate method
        if (updatedProfile.getStageName() != null) {
            existingProfile.setStageName(updatedProfile.getStageName());
        }
        if (updatedProfile.getBio() != null) {
            existingProfile.setBio(updatedProfile.getBio());
        }
        if (updatedProfile.getDateOfBirth() != null) {
            existingProfile.setDateOfBirth(updatedProfile.getDateOfBirth());
        }
        if (updatedProfile.getGender() != null) {
            existingProfile.setGender(updatedProfile.getGender());
        }
        if (updatedProfile.getLocation() != null) {
            existingProfile.setLocation(updatedProfile.getLocation());
        }
        if (updatedProfile.getArtistType() != null) {
            existingProfile.setArtistType(updatedProfile.getArtistType());
        }
        // Note: Profile images and portfolio URLs are now handled by Document entity
        // They should be uploaded separately using the document upload API
        if (updatedProfile.getSkills() != null) {
            existingProfile.setSkills(updatedProfile.getSkills());
        }
        if (updatedProfile.getExperienceYears() != null) {
            existingProfile.setExperienceYears(updatedProfile.getExperienceYears());
        }
        if (updatedProfile.getHourlyRate() != null) {
            existingProfile.setHourlyRate(updatedProfile.getHourlyRate());
        }

        // Physical / personal attribute fields
        if (updatedProfile.getMaritalStatus() != null) {
            existingProfile.setMaritalStatus(updatedProfile.getMaritalStatus());
        }
        if (updatedProfile.getWeight() != null) {
            existingProfile.setWeight(updatedProfile.getWeight());
        }
        if (updatedProfile.getHeight() != null) {
            existingProfile.setHeight(updatedProfile.getHeight());
        }
        if (updatedProfile.getHairColor() != null) {
            existingProfile.setHairColor(updatedProfile.getHairColor());
        }
        if (updatedProfile.getHairLength() != null) {
            existingProfile.setHairLength(updatedProfile.getHairLength());
        }
        if (updatedProfile.getHasTattoo() != null) {
            existingProfile.setHasTattoo(updatedProfile.getHasTattoo());
        }
        if (updatedProfile.getHasMole() != null) {
            existingProfile.setHasMole(updatedProfile.getHasMole());
        }
        if (updatedProfile.getShoeSize() != null) {
            existingProfile.setShoeSize(updatedProfile.getShoeSize());
        }
        if (updatedProfile.getEyeColor() != null) {
            existingProfile.setEyeColor(updatedProfile.getEyeColor());
        }
        if (updatedProfile.getComplexion() != null) {
            existingProfile.setComplexion(updatedProfile.getComplexion());
        }
        if (updatedProfile.getHasPassport() != null) {
            existingProfile.setHasPassport(updatedProfile.getHasPassport());
        }

        // JSON fields
        if (updatedProfile.getLanguagesSpoken() != null) {
            existingProfile.setLanguagesSpoken(updatedProfile.getLanguagesSpoken());
        }
        if (updatedProfile.getComfortableAreas() != null) {
            existingProfile.setComfortableAreas(updatedProfile.getComfortableAreas());
        }
        if (updatedProfile.getProjectsWorked() != null) {
            existingProfile.setProjectsWorked(updatedProfile.getProjectsWorked());
        }
        if (updatedProfile.getTravelCities() != null) {
            existingProfile.setTravelCities(updatedProfile.getTravelCities());
        }

        // Portfolio URLs
        if (updatedProfile.getPhotoUrl() != null) {
            existingProfile.setPhotoUrl(updatedProfile.getPhotoUrl());
        }
        if (updatedProfile.getVideoUrl() != null) {
            existingProfile.setVideoUrl(updatedProfile.getVideoUrl());
        }
        if (updatedProfile.getPortfolioUrls() != null) {
            existingProfile.setPortfolioUrls(updatedProfile.getPortfolioUrls());
        }
        if (updatedProfile.getProfileUrl() != null) {
            existingProfile.setProfileUrl(updatedProfile.getProfileUrl());
        }
        if (updatedProfile.getFrontProfilePhoto() != null) {
            existingProfile.setFrontProfilePhoto(updatedProfile.getFrontProfilePhoto());
        }
        if (updatedProfile.getLeftProfilePhoto() != null) {
            existingProfile.setLeftProfilePhoto(updatedProfile.getLeftProfilePhoto());
        }
        if (updatedProfile.getRightProfilePhoto() != null) {
            existingProfile.setRightProfilePhoto(updatedProfile.getRightProfilePhoto());
        }
        if (updatedProfile.getCoverPhotoUrl() != null) {
            existingProfile.setCoverPhotoUrl(updatedProfile.getCoverPhotoUrl());
        }

        ArtistProfile saved = artistProfileRepository.save(existingProfile);
        // Update profile completeness after saving
        updateProfileCompleteness(saved.getId());
        return saved;
    }

    /**
     * Calculate and update profile completeness for an artist
     */
    @Transactional
    public void updateProfileCompleteness(Long artistProfileId) {
        ArtistProfile profile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        int completionPercentage = calculateProfileCompletionPercentage(profile);
        boolean isComplete = completionPercentage >= 80;

        profile.setIsProfileComplete(isComplete);
        artistProfileRepository.save(profile);
        log.info("Profile completeness updated for artist {}: {}% (complete: {})", artistProfileId, completionPercentage, isComplete);
    }

    /**
     * Calculate profile completion percentage for an artist profile
     * @param profile Artist profile to calculate completion for
     * @return Completion percentage (0-100)
     */
    public int calculateProfileCompletionPercentage(ArtistProfile profile) {
        if (profile == null) {
            return 0;
        }

        int completedFields = 0;
        int totalFields = 12; // Total number of fields to check

        // Check required fields
        if (profile.getFirstName() != null && !profile.getFirstName().trim().isEmpty()) completedFields++;
        if (profile.getLastName() != null && !profile.getLastName().trim().isEmpty()) completedFields++;
        if (profile.getBio() != null && !profile.getBio().trim().isEmpty()) completedFields++;
        if (profile.getLocation() != null && !profile.getLocation().trim().isEmpty()) completedFields++;
        if (profile.getSkills() != null && !profile.getSkills().trim().isEmpty()) completedFields++;
        if (profile.getExperienceYears() != null) completedFields++;
        if (profile.getHourlyRate() != null) completedFields++;
        if (profile.getWeight() != null) completedFields++;
        if (profile.getHeight() != null) completedFields++;
        if (profile.getLanguagesSpoken() != null && !profile.getLanguagesSpoken().trim().isEmpty()) completedFields++;
        if (profile.getPortfolioUrls() != null && !profile.getPortfolioUrls().trim().isEmpty()) completedFields++;
        if (profile.getComfortableAreas() != null && !profile.getComfortableAreas().trim().isEmpty()) completedFields++;

        return totalFields > 0 ? (completedFields * 100) / totalFields : 0;
    }

    /**
     * Calculate profile completion percentage by artist profile ID
     * @param artistProfileId Artist profile ID
     * @return Completion percentage (0-100)
     */
    public int calculateProfileCompletionPercentage(Long artistProfileId) {
        ArtistProfile profile = artistProfileRepository.findById(artistProfileId)
                .orElse(null);
        return calculateProfileCompletionPercentage(profile);
    }

    public void updateBasicProfile(Long userId, UpdateUserProfileDto updateDto) {
        ArtistProfile artistProfile = artistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        User user = artistProfile.getUser();
        if (updateDto.getFirstName() != null) {
            user.setFirstName(updateDto.getFirstName());
        }
        if (updateDto.getLastName() != null) {
            user.setLastName(updateDto.getLastName());
        }
        userRepository.save(user);
        if (updateDto.getBio() != null) {
            artistProfile.setBio(updateDto.getBio());
        }
        if (updateDto.getLocation() != null) {
            artistProfile.setLocation(updateDto.getLocation());
        }
        // Note: Profile images are now handled by Document entity
        // They should be uploaded separately using the document upload API

        artistProfileRepository.save(artistProfile);
    }

    public ArtistProfile requestVerificationBadge(Long artistProfileId) {
        ArtistProfile artistProfile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        artistProfile.setVerificationRequestedAt(java.time.LocalDate.now());
        return artistProfileRepository.save(artistProfile);
    }

    public ArtistProfile approveVerificationBadge(Long artistProfileId) {
        ArtistProfile artistProfile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        artistProfile.setIsVerifiedBadge(true);
        artistProfile.setVerificationApprovedAt(java.time.LocalDate.now());
        return artistProfileRepository.save(artistProfile);
    }

    public ArtistProfile rejectVerificationBadge(Long artistProfileId) {
        ArtistProfile artistProfile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        artistProfile.setVerificationRequestedAt(null);
        return artistProfileRepository.save(artistProfile);
    }

    public void incrementApplications(Long artistProfileId) {
        ArtistProfile artistProfile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        artistProfile.setTotalApplications(artistProfile.getTotalApplications() + 1);
        artistProfileRepository.save(artistProfile);
    }

    public void incrementSuccessfulHires(Long artistProfileId) {
        ArtistProfile artistProfile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        artistProfile.setSuccessfulHires(artistProfile.getSuccessfulHires() + 1);
        artistProfileRepository.save(artistProfile);
    }

    public void deleteArtistProfile(Long artistProfileId) {
        ArtistProfile artistProfile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        artistProfile.setIsActive(false);
        artistProfileRepository.save(artistProfile);
    }

    @Transactional(readOnly = true)
    public List<ArtistProfile> findPendingVerificationRequests() {
        return artistProfileRepository.findPendingVerificationRequests();
    }

    public ArtistProfile save(ArtistProfile artistProfile) {
        return artistProfileRepository.save(artistProfile);
    }

    @Transactional(readOnly = true)
    public List<ArtistProfile> searchArtists(String searchTerm, String location, String skills, 
                                           Long artistTypeId, Integer minExperience, Double maxRate) {
        // This would be implemented with more complex search logic
        // For now, return basic search results
        if (artistTypeId != null) {
            return findActiveArtistsByType(artistTypeId);
        }
        return findActiveArtists();
    }

    /**
     * Save dynamic fields for an artist profile
     * @param artistProfileId The ID of the artist profile
     * @param dynamicFields List of dynamic field data
     */
    @Transactional
    public void saveDynamicFields(Long artistProfileId, List<ArtistProfileFieldDto> dynamicFields) {
        ArtistProfile artistProfile = artistProfileRepository.findById(artistProfileId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        // Get existing fields to check for duplicates
        List<ArtistProfileField> existingFields = artistProfileFieldRepository.findByArtistProfileId(artistProfileId);
        
        // Process each dynamic field
        for (ArtistProfileFieldDto fieldDto : dynamicFields) {
            if (fieldDto.getFieldValue() != null && !fieldDto.getFieldValue().trim().isEmpty()) {
                ArtistTypeField artistTypeField = artistTypeFieldRepository.findById(fieldDto.getArtistTypeFieldId())
                        .orElseThrow(() -> new RuntimeException("Artist type field not found with id: " + fieldDto.getArtistTypeFieldId()));

                // Check if field already exists
                Optional<ArtistProfileField> existingField = existingFields.stream()
                        .filter(field -> field.getArtistTypeField().getId().equals(fieldDto.getArtistTypeFieldId()))
                        .findFirst();

                if (existingField.isPresent()) {
                    // Update existing field
                    ArtistProfileField profileField = existingField.get();
                    profileField.setFieldValue(fieldDto.getFieldValue());
                    artistProfileFieldRepository.save(profileField);
                    log.info("Updated dynamic field '{}' for artist profile {}", artistTypeField.getFieldName(), artistProfileId);
                } else {
                    // Create new field
                    ArtistProfileField profileField = new ArtistProfileField();
                    profileField.setArtistProfile(artistProfile);
                    profileField.setArtistTypeField(artistTypeField);
                    profileField.setFieldValue(fieldDto.getFieldValue());
                    
                    // File handling is now managed by Document entity

                    artistProfileFieldRepository.save(profileField);
                    log.info("Saved dynamic field '{}' for artist profile {}", artistTypeField.getFieldName(), artistProfileId);
                }
            }
        }

        // Remove fields that are no longer in the request
        List<Long> requestedFieldIds = dynamicFields.stream()
                .map(ArtistProfileFieldDto::getArtistTypeFieldId)
                .collect(java.util.stream.Collectors.toList());
        
        List<ArtistProfileField> fieldsToDelete = existingFields.stream()
                .filter(field -> !requestedFieldIds.contains(field.getArtistTypeField().getId()))
                .collect(java.util.stream.Collectors.toList());
        
        if (!fieldsToDelete.isEmpty()) {
            artistProfileFieldRepository.deleteAll(fieldsToDelete);
            log.info("Deleted {} unused dynamic fields for artist profile {}", fieldsToDelete.size(), artistProfileId);
        }
        
        // Update profile completeness after saving dynamic fields
        updateProfileCompleteness(artistProfileId);
    }

    /**
     * Get dynamic fields for an artist profile
     * @param artistProfileId The ID of the artist profile
     * @return List of dynamic field data
     */
    @Transactional(readOnly = true)
    public List<ArtistProfileFieldDto> getDynamicFields(Long artistProfileId) {
        List<ArtistProfileField> fields = artistProfileFieldRepository.findByArtistProfileId(artistProfileId);
        return fields.stream()
                .map(this::convertToFieldDto)
                .collect(java.util.stream.Collectors.toList());
    }

    private ArtistProfileFieldDto convertToFieldDto(ArtistProfileField field) {
        ArtistProfileFieldDto dto = new ArtistProfileFieldDto();
        dto.setId(field.getId());
        dto.setArtistTypeFieldId(field.getArtistTypeField().getId());
        dto.setFieldName(field.getArtistTypeField().getFieldName());
        dto.setDisplayName(field.getArtistTypeField().getDisplayName());
        dto.setFieldValue(field.getFieldValue());
        // File handling is now managed by Document entity
        return dto;
    }
}

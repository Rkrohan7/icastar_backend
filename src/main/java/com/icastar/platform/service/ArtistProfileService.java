package com.icastar.platform.service;

import com.icastar.platform.dto.artist.ArtistProfileCompleteDto;
import com.icastar.platform.dto.artist.ArtistProfileCompleteDto.DocumentDto;
import com.icastar.platform.dto.artist.ArtistTypeDto;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.ArtistProfileArtistType;
import com.icastar.platform.entity.ArtistType;
import com.icastar.platform.entity.Document;
import com.icastar.platform.entity.User;
import com.icastar.platform.repository.ArtistProfileRepository;
import com.icastar.platform.repository.ArtistProfileArtistTypeRepository;
import com.icastar.platform.repository.ArtistTypeRepository;
import com.icastar.platform.repository.DocumentRepository;
import com.icastar.platform.repository.UserRepository;
import com.icastar.platform.config.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtistProfileService {

    private final ArtistProfileRepository artistProfileRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ArtistProfileArtistTypeRepository artistProfileArtistTypeRepository;
    private final ArtistTypeRepository artistTypeRepository;

    /**
     * Get complete artist profile by user ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.ARTIST_PROFILE_BY_USER, key = "#userId", unless = "#result == null || !#result.isPresent()")
    public Optional<ArtistProfileCompleteDto> getCompleteProfileByUserId(Long userId) {
        log.debug("Cache MISS: Loading artist profile for user ID: {}", userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            ArtistProfile artistProfile = artistProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));
            
            List<Document> documents = documentRepository.findByUserId(userId);
            
            return Optional.of(mapToCompleteDto(user, artistProfile, documents));
        } catch (Exception e) {
            log.error("Error getting complete profile for user ID: {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Get complete artist profile by artist profile ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.ARTIST_PROFILE_BY_ID, key = "#artistProfileId", unless = "#result == null || !#result.isPresent()")
    public Optional<ArtistProfileCompleteDto> getCompleteProfileById(Long artistProfileId) {
        log.debug("Cache MISS: Loading artist profile for profile ID: {}", artistProfileId);
        try {
            ArtistProfile artistProfile = artistProfileRepository.findById(artistProfileId)
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));
            
            User user = artistProfile.getUser();
            List<Document> documents = documentRepository.findByUserId(user.getId());
            
            return Optional.of(mapToCompleteDto(user, artistProfile, documents));
        } catch (Exception e) {
            log.error("Error getting complete profile for artist profile ID: {}", artistProfileId, e);
            return Optional.empty();
        }
    }

    /**
     * Get all artist profiles with pagination
     */
    @Transactional(readOnly = true)
    public Page<ArtistProfileCompleteDto> getAllCompleteProfiles(Pageable pageable) {
        try {
            Page<ArtistProfile> artistProfiles = artistProfileRepository.findAll(pageable);
            
            return artistProfiles.map(artistProfile -> {
                User user = artistProfile.getUser();
                List<Document> documents = documentRepository.findByUserId(user.getId());
                return mapToCompleteDto(user, artistProfile, documents);
            });
        } catch (Exception e) {
            log.error("Error getting all complete profiles", e);
            throw new RuntimeException("Failed to get artist profiles: " + e.getMessage());
        }
    }

    /**
     * Get artist profiles by artist type
     */
    @Transactional(readOnly = true)
    public List<ArtistProfileCompleteDto> getProfilesByArtistType(String artistTypeName) {
        try {
            List<ArtistProfile> artistProfiles = artistProfileRepository.findByArtistTypeName(artistTypeName);
            
            return artistProfiles.stream().map(artistProfile -> {
                User user = artistProfile.getUser();
                List<Document> documents = documentRepository.findByUserId(user.getId());
                return mapToCompleteDto(user, artistProfile, documents);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting profiles by artist type: {}", artistTypeName, e);
            throw new RuntimeException("Failed to get artist profiles: " + e.getMessage());
        }
    }

    /**
     * Search artist profiles
     */
    @Transactional(readOnly = true)
    public List<ArtistProfileCompleteDto> searchProfiles(String searchTerm) {
        try {
            List<ArtistProfile> artistProfiles = artistProfileRepository.findBySearchTerm(searchTerm);
            
            return artistProfiles.stream().map(artistProfile -> {
                User user = artistProfile.getUser();
                List<Document> documents = documentRepository.findByUserId(user.getId());
                return mapToCompleteDto(user, artistProfile, documents);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching profiles with term: {}", searchTerm, e);
            throw new RuntimeException("Failed to search artist profiles: " + e.getMessage());
        }
    }

    /**
     * Update artist profile basic information
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_USER, key = "#userId"),
        @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_ID, allEntries = true),
        @CacheEvict(value = CacheNames.DASHBOARD_ARTIST, key = "#userId")
    })
    public ArtistProfileCompleteDto updateProfile(Long userId, ArtistProfileCompleteDto updateDto) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            ArtistProfile artistProfile = artistProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));
            
            // Update user fields
            if (updateDto.getFirstName() != null) {
                user.setFirstName(updateDto.getFirstName());
            }
            if (updateDto.getLastName() != null) {
                user.setLastName(updateDto.getLastName());
            }
            if (updateDto.getPhone() != null) {
                user.setMobile(updateDto.getPhone()); // User entity has 'mobile' field, not 'phone'
            }
            // Note: User entity doesn't have 'city' field, it's in ArtistProfile
            
            // Update artist profile fields
            if (updateDto.getStageName() != null) {
                artistProfile.setStageName(updateDto.getStageName());
            }
            if (updateDto.getBio() != null) {
                artistProfile.setBio(updateDto.getBio());
            }
            if (updateDto.getDateOfBirth() != null) {
                artistProfile.setDateOfBirth(updateDto.getDateOfBirth());
            }
            if (updateDto.getGender() != null) {
                artistProfile.setGender(updateDto.getGender());
            }
            if (updateDto.getLocation() != null) {
                artistProfile.setLocation(updateDto.getLocation());
            }
            if (updateDto.getMaritalStatus() != null) {
                artistProfile.setMaritalStatus(updateDto.getMaritalStatus());
            }
            if (updateDto.getLanguagesSpoken() != null) {
                artistProfile.setLanguagesSpoken(updateDto.getLanguagesSpoken());
            }
            if (updateDto.getComfortableAreas() != null) {
                artistProfile.setComfortableAreas(updateDto.getComfortableAreas());
            }
            if (updateDto.getProjectsWorked() != null) {
                artistProfile.setProjectsWorked(updateDto.getProjectsWorked());
            }
            if (updateDto.getSkills() != null) {
                artistProfile.setSkills(updateDto.getSkills());
            }
            if (updateDto.getExperienceYears() != null) {
                artistProfile.setExperienceYears(updateDto.getExperienceYears());
            }
            if (updateDto.getWeight() != null) {
                artistProfile.setWeight(updateDto.getWeight());
            }
            if (updateDto.getHeight() != null) {
                artistProfile.setHeight(updateDto.getHeight());
            }
            if (updateDto.getHairColor() != null) {
                artistProfile.setHairColor(updateDto.getHairColor());
            }
            if (updateDto.getHairLength() != null) {
                artistProfile.setHairLength(updateDto.getHairLength());
            }
            if (updateDto.getHasTattoo() != null) {
                artistProfile.setHasTattoo(updateDto.getHasTattoo());
            }
            if (updateDto.getHasMole() != null) {
                artistProfile.setHasMole(updateDto.getHasMole());
            }
            if (updateDto.getShoeSize() != null) {
                artistProfile.setShoeSize(updateDto.getShoeSize());
            }
            if (updateDto.getEyeColor() != null) {
                artistProfile.setEyeColor(updateDto.getEyeColor());
            }
            if (updateDto.getComplexion() != null) {
                artistProfile.setComplexion(updateDto.getComplexion());
            }
            if (updateDto.getHasPassport() != null) {
                artistProfile.setHasPassport(updateDto.getHasPassport());
            }
            if (updateDto.getTravelCities() != null) {
                artistProfile.setTravelCities(updateDto.getTravelCities());
            }
            if (updateDto.getHourlyRate() != null) {
                artistProfile.setHourlyRate(updateDto.getHourlyRate());
            }
            if (updateDto.getPhotoUrl() != null) {
                artistProfile.setPhotoUrl(updateDto.getPhotoUrl());
            }
            if (updateDto.getVideoUrl() != null) {
                artistProfile.setVideoUrl(updateDto.getVideoUrl());
            }
            if (updateDto.getPortfolioUrls() != null) {
                artistProfile.setPortfolioUrls(updateDto.getPortfolioUrls());
            }
            if (updateDto.getProfileUrl() != null) {
                artistProfile.setProfileUrl(updateDto.getProfileUrl());
            }
            if (updateDto.getCoverPhotoUrl() != null) {
                artistProfile.setCoverPhotoUrl(updateDto.getCoverPhotoUrl());
            }
            if (updateDto.getIdProofUrl() != null) {
                artistProfile.setIdProofUrl(updateDto.getIdProofUrl());
                // Set upload timestamp when ID proof is uploaded
                if (artistProfile.getIdProofUploadedAt() == null) {
                    artistProfile.setIdProofUploadedAt(java.time.LocalDate.now());
                }
            }

            // Handle onboarding completion
            if (updateDto.getIsOnboardingComplete() != null) {
                // If frontend explicitly sends isOnboardingComplete value, use it
                user.setIsOnboardingComplete(updateDto.getIsOnboardingComplete());
                log.info("Onboarding status set to {} for user ID: {}", updateDto.getIsOnboardingComplete(), userId);
            } else if (!user.getIsOnboardingComplete()) {
                // Otherwise, automatically mark as complete on first profile update
                user.setIsOnboardingComplete(true);
                log.info("Onboarding automatically completed for user ID: {}", userId);
            }

            // Handle multiple artist types update
            if (updateDto.getArtistTypeIds() != null && !updateDto.getArtistTypeIds().isEmpty()) {
                updateArtistTypes(artistProfile, updateDto.getArtistTypeIds());
            } else if (updateDto.getArtistTypeId() != null) {
                // Backward compatibility: if only single artistTypeId is provided
                List<Long> singleTypeList = new ArrayList<>();
                singleTypeList.add(updateDto.getArtistTypeId());
                updateArtistTypes(artistProfile, singleTypeList);
            }

            // Save updates
            userRepository.save(user);
            artistProfileRepository.save(artistProfile);
            
            // Get updated documents
            List<Document> documents = documentRepository.findByUserId(userId);
            
            return mapToCompleteDto(user, artistProfile, documents);
        } catch (Exception e) {
            log.error("Error updating profile for user ID: {}", userId, e);
            throw new RuntimeException("Failed to update profile: " + e.getMessage());
        }
    }

    /**
     * Update artist profile photo URL
     */
    @Transactional
    @CacheEvict(value = CacheNames.ARTIST_PROFILE_BY_USER, key = "#userId")
    public String updateProfilePhotoUrl(Long userId, String profileUrl) {
        try {
            ArtistProfile artistProfile = artistProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Artist profile not found for user ID: " + userId));

            artistProfile.setProfileUrl(profileUrl);
            artistProfileRepository.save(artistProfile);

            log.info("Successfully updated profile photo URL for user ID: {}", userId);
            return profileUrl;
        } catch (Exception e) {
            log.error("Error updating profile photo URL for user ID: {}", userId, e);
            throw new RuntimeException("Failed to update profile photo URL: " + e.getMessage());
        }
    }

    /**
     * Update artist cover photo URL
     */
    @Transactional
    public String updateCoverPhotoUrl(Long userId, String coverPhotoUrl) {
        try {
            ArtistProfile artistProfile = artistProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Artist profile not found for user ID: " + userId));

            artistProfile.setCoverPhotoUrl(coverPhotoUrl);
            artistProfileRepository.save(artistProfile);

            log.info("Successfully updated cover photo URL for user ID: {}", userId);
            return coverPhotoUrl;
        } catch (Exception e) {
            log.error("Error updating cover photo URL for user ID: {}", userId, e);
            throw new RuntimeException("Failed to update cover photo URL: " + e.getMessage());
        }
    }

    /**
     * Update artist ID proof URL
     */
    @Transactional
    public String updateIdProofUrl(Long userId, String idProofUrl) {
        try {
            ArtistProfile artistProfile = artistProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Artist profile not found for user ID: " + userId));

            artistProfile.setIdProofUrl(idProofUrl);
            artistProfile.setIdProofUploadedAt(java.time.LocalDate.now());
            artistProfileRepository.save(artistProfile);

            log.info("Successfully updated ID proof URL for user ID: {}", userId);
            return idProofUrl;
        } catch (Exception e) {
            log.error("Error updating ID proof URL for user ID: {}", userId, e);
            throw new RuntimeException("Failed to update ID proof URL: " + e.getMessage());
        }
    }

    /**
     * Update artist dance showreel URL
     */
    @Transactional
    public String updateDanceShowreelUrl(Long userId, String danceShowreelUrl) {
        try {
            ArtistProfile artistProfile = artistProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Artist profile not found for user ID: " + userId));

            artistProfile.setDanceShowreelUrl(danceShowreelUrl);
            artistProfileRepository.save(artistProfile);

            log.info("Successfully updated dance showreel URL for user ID: {}", userId);
            return danceShowreelUrl;
        } catch (Exception e) {
            log.error("Error updating dance showreel URL for user ID: {}", userId, e);
            throw new RuntimeException("Failed to update dance showreel URL: " + e.getMessage());
        }
    }

    /**
     * Update artist face verification URL
     */
    @Transactional
    public String updateFaceVerificationUrl(Long userId, String faceVerificationUrl) {
        try {
            ArtistProfile artistProfile = artistProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Artist profile not found for user ID: " + userId));

            artistProfile.setFaceVerificationUrl(faceVerificationUrl);
            artistProfileRepository.save(artistProfile);

            log.info("Successfully updated face verification URL for user ID: {}", userId);
            return faceVerificationUrl;
        } catch (Exception e) {
            log.error("Error updating face verification URL for user ID: {}", userId, e);
            throw new RuntimeException("Failed to update face verification URL: " + e.getMessage());
        }
    }

    /**
     * Delete artist profile
     */
    @Transactional
    public void deleteProfile(Long userId) {
        try {
            ArtistProfile artistProfile = artistProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Artist profile not found"));
            
            // Delete documents first
            List<Document> documents = documentRepository.findByUserId(userId);
            documentRepository.deleteAll(documents);
            
            // Delete artist profile
            artistProfileRepository.delete(artistProfile);
            
            log.info("Successfully deleted artist profile for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Error deleting profile for user ID: {}", userId, e);
            throw new RuntimeException("Failed to delete profile: " + e.getMessage());
        }
    }

    /**
     * Map entities to complete DTO
     */
    private ArtistProfileCompleteDto mapToCompleteDto(User user, ArtistProfile artistProfile, List<Document> documents) {
        ArtistProfileCompleteDto dto = new ArtistProfileCompleteDto();
        
        // User fields
        dto.setUserId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getMobile()); // User entity has 'mobile' field, not 'phone'
        dto.setCity(artistProfile.getLocation()); // City is stored in ArtistProfile.location
        dto.setIsActive(user.getStatus() == com.icastar.platform.entity.User.UserStatus.ACTIVE);
        dto.setIsOnboardingComplete(user.getIsOnboardingComplete());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        
        // Artist Profile fields
        dto.setArtistProfileId(artistProfile.getId());

        // Set primary artist type (backward compatibility)
        dto.setArtistTypeId(artistProfile.getArtistType().getId());
        dto.setArtistTypeName(artistProfile.getArtistType().getName());
        dto.setArtistTypeDisplayName(artistProfile.getArtistType().getDisplayName());

        // Get all artist types from the join table
        List<ArtistProfileArtistType> allArtistTypes = artistProfileArtistTypeRepository
                .findByArtistProfileIdOrderBySortOrder(artistProfile.getId());

        if (allArtistTypes != null && !allArtistTypes.isEmpty()) {
            // Map to ArtistTypeDto list
            List<ArtistTypeDto> artistTypeDtos = allArtistTypes.stream()
                    .map(apat -> new ArtistTypeDto(
                            apat.getArtistType().getId(),
                            apat.getArtistType().getName(),
                            apat.getArtistType().getDisplayName()))
                    .collect(Collectors.toList());
            dto.setArtistTypes(artistTypeDtos);

            // Also set the list of IDs for convenience
            List<Long> artistTypeIds = allArtistTypes.stream()
                    .map(apat -> apat.getArtistType().getId())
                    .collect(Collectors.toList());
            dto.setArtistTypeIds(artistTypeIds);
        } else {
            // Fallback: if no entries in join table, use the primary artist type
            List<ArtistTypeDto> artistTypeDtos = new ArrayList<>();
            artistTypeDtos.add(new ArtistTypeDto(
                    artistProfile.getArtistType().getId(),
                    artistProfile.getArtistType().getName(),
                    artistProfile.getArtistType().getDisplayName()));
            dto.setArtistTypes(artistTypeDtos);

            List<Long> artistTypeIds = new ArrayList<>();
            artistTypeIds.add(artistProfile.getArtistType().getId());
            dto.setArtistTypeIds(artistTypeIds);
        }

        dto.setStageName(artistProfile.getStageName());
        dto.setBio(artistProfile.getBio());
        dto.setDateOfBirth(artistProfile.getDateOfBirth());
        dto.setGender(artistProfile.getGender());
        dto.setLocation(artistProfile.getLocation());
        dto.setMaritalStatus(artistProfile.getMaritalStatus());
        dto.setLanguagesSpoken(artistProfile.getLanguagesSpoken());
        dto.setComfortableAreas(artistProfile.getComfortableAreas());
        dto.setProjectsWorked(artistProfile.getProjectsWorked());
        dto.setSkills(artistProfile.getSkills());
        dto.setExperienceYears(artistProfile.getExperienceYears());
        dto.setWeight(artistProfile.getWeight());
        dto.setHeight(artistProfile.getHeight());
        dto.setHairColor(artistProfile.getHairColor());
        dto.setHairLength(artistProfile.getHairLength());
        dto.setHasTattoo(artistProfile.getHasTattoo());
        dto.setHasMole(artistProfile.getHasMole());
        dto.setShoeSize(artistProfile.getShoeSize());
        dto.setEyeColor(artistProfile.getEyeColor());
        dto.setComplexion(artistProfile.getComplexion());
        dto.setHasPassport(artistProfile.getHasPassport());
        dto.setTravelCities(artistProfile.getTravelCities());
        dto.setHourlyRate(artistProfile.getHourlyRate());
        dto.setPhotoUrl(artistProfile.getPhotoUrl());
        dto.setVideoUrl(artistProfile.getVideoUrl());
        dto.setPortfolioUrls(artistProfile.getPortfolioUrls());
        dto.setProfileUrl(artistProfile.getProfileUrl());
        dto.setCoverPhotoUrl(artistProfile.getCoverPhotoUrl());
        dto.setIdProofUrl(artistProfile.getIdProofUrl());
        dto.setFaceVerificationUrl(artistProfile.getFaceVerificationUrl());
        dto.setIdProofVerified(artistProfile.getIdProofVerified());
        dto.setIdProofUploadedAt(artistProfile.getIdProofUploadedAt());
        dto.setIsVerifiedBadge(artistProfile.getIsVerifiedBadge());
        dto.setVerificationRequestedAt(artistProfile.getVerificationRequestedAt());
        dto.setVerificationApprovedAt(artistProfile.getVerificationApprovedAt());
        dto.setTotalApplications(artistProfile.getTotalApplications());
        dto.setSuccessfulHires(artistProfile.getSuccessfulHires());
        dto.setIsProfileComplete(artistProfile.getIsProfileComplete());
        
        // Documents
        dto.setDocuments(documents.stream().map(this::mapToDocumentDto).collect(Collectors.toList()));
        
        return dto;
    }

    /**
     * Map Document entity to DocumentDto
     */
    private DocumentDto mapToDocumentDto(Document document) {
        DocumentDto dto = new DocumentDto();
        dto.setId(document.getId());
        dto.setDocumentType(document.getDocumentType());
        dto.setFileName(document.getFileName());
        dto.setFileUrl(document.getFileUrl());
        dto.setFileSize(document.getFileSize());
        dto.setMimeType(document.getMimeType());
        dto.setUploadedAt(document.getUploadedAt());
        dto.setIsVerified(document.getIsVerified());
        dto.setVerifiedAt(document.getVerifiedAt());
        dto.setVerifiedBy(document.getVerifiedBy());
        dto.setVerificationNotes(document.getVerificationNotes());
        return dto;
    }

    /**
     * Update artist types for a profile
     * First ID in the list is the primary type
     * Removes duplicates and validates all IDs
     * Maximum 5 artist types allowed
     */
    private void updateArtistTypes(ArtistProfile artistProfile, List<Long> artistTypeIds) {
        // Remove duplicates while preserving order
        Set<Long> uniqueIds = new LinkedHashSet<>(artistTypeIds);
        List<Long> uniqueIdList = new ArrayList<>(uniqueIds);

        // Limit to max 5 artist types
        if (uniqueIdList.size() > 5) {
            uniqueIdList = uniqueIdList.subList(0, 5);
            log.warn("Artist types list truncated to 5 for profile ID: {}", artistProfile.getId());
        }

        // Validate at least one artist type
        if (uniqueIdList.isEmpty()) {
            throw new RuntimeException("At least one artist type is required");
        }

        // Fetch and validate all artist types
        List<ArtistType> validArtistTypes = new ArrayList<>();
        for (Long typeId : uniqueIdList) {
            ArtistType artistType = artistTypeRepository.findById(typeId)
                    .orElseThrow(() -> new RuntimeException("Invalid artist type ID: " + typeId));
            if (!artistType.getIsActive()) {
                throw new RuntimeException("Artist type is not active: " + typeId);
            }
            validArtistTypes.add(artistType);
        }

        // First type is primary - update the main artist_type_id field
        ArtistType primaryType = validArtistTypes.get(0);
        artistProfile.setArtistType(primaryType);
        artistProfileRepository.save(artistProfile);

        // Delete existing entries in join table
        artistProfileArtistTypeRepository.deleteAllByArtistProfileId(artistProfile.getId());

        // Flush to ensure delete is committed before insert
        artistProfileArtistTypeRepository.flush();

        // Create new entries in join table for ALL artist types
        List<ArtistProfileArtistType> newEntries = new ArrayList<>();
        for (int i = 0; i < validArtistTypes.size(); i++) {
            ArtistProfileArtistType entry = new ArtistProfileArtistType(
                    artistProfile,
                    validArtistTypes.get(i),
                    i == 0, // first one is primary
                    i       // sort order
            );
            newEntries.add(entry);
            log.debug("Adding artist type {} with sortOrder {} for profile ID: {}",
                    validArtistTypes.get(i).getName(), i, artistProfile.getId());
        }
        artistProfileArtistTypeRepository.saveAll(newEntries);
        artistProfileArtistTypeRepository.flush();

        log.info("Saved {} artist types to join table for profile ID: {}", newEntries.size(), artistProfile.getId());
    }
}

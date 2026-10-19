package com.icastar.platform.service;

import com.icastar.platform.dto.artist.CreateArtistProfileDto;
import com.icastar.platform.dto.ArtistProfileFieldDto;
import com.icastar.platform.entity.ArtistProfile;
import com.icastar.platform.entity.ArtistProfileField;
import com.icastar.platform.entity.ArtistType;
import com.icastar.platform.entity.ArtistTypeField;
import com.icastar.platform.entity.User;
import com.icastar.platform.repository.ArtistProfileRepository;
import com.icastar.platform.repository.ArtistTypeRepository;
import com.icastar.platform.repository.ArtistTypeFieldRepository;
import com.icastar.platform.repository.ArtistProfileFieldRepository;
import com.icastar.platform.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActorService {

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistTypeRepository artistTypeRepository;
    private final ArtistTypeFieldRepository artistTypeFieldRepository;
    private final ArtistProfileFieldRepository artistProfileFieldRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Transactional
    public ArtistProfile createActorProfile(User user, CreateArtistProfileDto request) {
        try {
            // Encode password
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            
            // Save user
            User savedUser = userRepository.save(user);

            // Find actor type
            ArtistType actorType = artistTypeRepository.findByName("ACTOR")
                    .orElseThrow(() -> new RuntimeException("Actor type not found"));

            // Create basic artist profile (only core fields)
            ArtistProfile artistProfile = new ArtistProfile();
            artistProfile.setUser(savedUser);
            artistProfile.setArtistType(actorType);
            artistProfile.setFirstName(request.getFirstName());
            artistProfile.setLastName(request.getLastName());
            artistProfile.setStageName(request.getStageName());
            artistProfile.setBio(request.getBio());
            artistProfile.setDateOfBirth(request.getDateOfBirth());
            artistProfile.setGender(request.getGender());
            artistProfile.setLocation(request.getLocation());
            artistProfile.setSkills(convertToJson(request.getSkills()));
            artistProfile.setExperienceYears(request.getExperienceYears());
            artistProfile.setHourlyRate(request.getHourlyRate());
            artistProfile.setFrontProfilePhoto(request.getFrontProfilePhoto());
            artistProfile.setLeftProfilePhoto(request.getLeftProfilePhoto());
            artistProfile.setRightProfilePhoto(request.getRightProfilePhoto());

            // Save the basic profile first
            ArtistProfile savedProfile = artistProfileRepository.save(artistProfile);

            // Now save actor-specific fields using the dynamic field system
            List<ArtistProfileFieldDto> dynamicFields = createActorDynamicFields(request);
            saveDynamicFields(savedProfile.getId(), dynamicFields);

            return savedProfile;

        } catch (Exception e) {
            log.error("Error creating actor profile", e);
            throw new RuntimeException("Failed to create actor profile: " + e.getMessage());
        }
    }

    @Transactional
    public ArtistProfile updateActorProfile(Long profileId, CreateArtistProfileDto request) {
        try {
            ArtistProfile existingProfile = artistProfileRepository.findById(profileId)
                    .orElseThrow(() -> new RuntimeException("Actor profile not found"));

            // Update basic fields
            if (request.getFirstName() != null) {
                existingProfile.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                existingProfile.setLastName(request.getLastName());
            }
            if (request.getStageName() != null) {
                existingProfile.setStageName(request.getStageName());
            }
            if (request.getBio() != null) {
                existingProfile.setBio(request.getBio());
            }
            if (request.getDateOfBirth() != null) {
                existingProfile.setDateOfBirth(request.getDateOfBirth());
            }
            if (request.getGender() != null) {
                existingProfile.setGender(request.getGender());
            }
            if (request.getLocation() != null) {
                existingProfile.setLocation(request.getLocation());
            }
            if (request.getSkills() != null) {
                existingProfile.setSkills(convertToJson(request.getSkills()));
            }
            if (request.getExperienceYears() != null) {
                existingProfile.setExperienceYears(request.getExperienceYears());
            }
            if (request.getHourlyRate() != null) {
                existingProfile.setHourlyRate(request.getHourlyRate());
            }
            if (request.getFrontProfilePhoto() != null) {
                existingProfile.setFrontProfilePhoto(request.getFrontProfilePhoto());
            }
            if (request.getLeftProfilePhoto() != null) {
                existingProfile.setLeftProfilePhoto(request.getLeftProfilePhoto());
            }
            if (request.getRightProfilePhoto() != null) {
                existingProfile.setRightProfilePhoto(request.getRightProfilePhoto());
            }

            // Save basic profile updates
            ArtistProfile savedProfile = artistProfileRepository.save(existingProfile);

            // Update actor-specific fields using dynamic field system
            List<ArtistProfileFieldDto> dynamicFields = createActorDynamicFields(request);
            saveDynamicFields(savedProfile.getId(), dynamicFields);

            return savedProfile;

        } catch (Exception e) {
            log.error("Error updating actor profile", e);
            throw new RuntimeException("Failed to update actor profile: " + e.getMessage());
        }
    }

    /**
     * Create dynamic fields for actor profile from the request
     */
    private List<ArtistProfileFieldDto> createActorDynamicFields(CreateArtistProfileDto request) {
        List<ArtistProfileFieldDto> dynamicFields = new ArrayList<>();
        
        try {
            // Get actor type fields
            ArtistType actorType = artistTypeRepository.findByName("ACTOR")
                    .orElseThrow(() -> new RuntimeException("Actor type not found"));
            List<ArtistTypeField> actorFields = artistTypeFieldRepository.findActiveFieldsByArtistTypeName("ACTOR");
            
            // Map request data to dynamic fields
            for (ArtistTypeField field : actorFields) {
                ArtistProfileFieldDto fieldDto = new ArtistProfileFieldDto();
                fieldDto.setArtistTypeFieldId(field.getId());
                fieldDto.setFieldName(field.getFieldName());
                fieldDto.setDisplayName(field.getDisplayName());
                
                // Map values based on field name
                String fieldValue = getFieldValue(request, field.getFieldName());
                if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                    fieldDto.setFieldValue(fieldValue);
                    dynamicFields.add(fieldDto);
                }
            }
            
        } catch (Exception e) {
            log.error("Error creating dynamic fields", e);
        }
        
        return dynamicFields;
    }
    
    /**
     * Get field value from request based on field name
     */
    private String getFieldValue(CreateArtistProfileDto request, String fieldName) {
        try {
            switch (fieldName) {
                case "height":
                    return request.getHeight() != null ? request.getHeight().toString() : null;
                case "weight":
                    return request.getWeight() != null ? request.getWeight().toString() : null;
                case "hair_color":
                    return request.getHairColor();
                case "hair_length":
                    return request.getHairLength();
                case "has_tattoo":
                    return request.getHasTattoo() != null ? request.getHasTattoo().toString() : null;
                case "has_mole":
                    return request.getHasMole() != null ? request.getHasMole().toString() : null;
                case "shoe_size":
                    return request.getShoeSize();
                case "comfortable_areas":
                    return request.getComfortableAreas() != null ? 
                        objectMapper.writeValueAsString(request.getComfortableAreas()) : null;
                case "travel_cities":
                    return request.getTravelCities() != null ? 
                        objectMapper.writeValueAsString(request.getTravelCities()) : null;
                case "languages_spoken":
                    return request.getLanguagesSpoken() != null ? 
                        objectMapper.writeValueAsString(request.getLanguagesSpoken()) : null;
                case "years_experience":
                    return request.getExperienceYears() != null ? 
                        request.getExperienceYears().toString() : null;
                // Profile pictures are now handled by Document entity
                case "projects_worked":
                    return request.getProjectsWorked() != null ? 
                        objectMapper.writeValueAsString(request.getProjectsWorked()) : null;
                // Document-related fields are now handled by Document entity
                default:
                    return null;
            }
        } catch (JsonProcessingException e) {
            log.error("Error converting field value to JSON for field: " + fieldName, e);
            return null;
        }
    }

    /**
     * Save dynamic fields for an artist profile
     */
    @Transactional
    private void saveDynamicFields(Long artistProfileId, List<ArtistProfileFieldDto> dynamicFields) {
        try {
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
        } catch (Exception e) {
            log.error("Error saving dynamic fields", e);
            throw new RuntimeException("Failed to save dynamic fields: " + e.getMessage());
        }
    }

    private String convertToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Error converting list to JSON", e);
            return null;
        }
    }
}
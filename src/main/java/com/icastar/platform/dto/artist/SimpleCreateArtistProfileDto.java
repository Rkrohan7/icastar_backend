package com.icastar.platform.dto.artist;

import com.icastar.platform.entity.ArtistProfile;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SimpleCreateArtistProfileDto {

    // Single artistTypeId for backward compatibility
    private Long artistTypeId;

    // Multiple artist types - first one is primary
    private List<Long> artistTypeIds;

    private LocalDate dateOfBirth;

    private ArtistProfile.Gender gender;

    private String location;

    private Integer experienceYears;

    private Boolean isOnboardingComplete;
}


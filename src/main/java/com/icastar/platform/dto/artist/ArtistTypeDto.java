package com.icastar.platform.dto.artist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtistTypeDto {
    private Long id;
    private String name;
    private String displayName;
    private Integer experienceYears;

    // Constructor without experienceYears for backward compatibility
    public ArtistTypeDto(Long id, String name, String displayName) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.experienceYears = 0;
    }
}
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
}
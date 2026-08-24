package com.icastar.platform.dto.artist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionDto {
    private Long artistTypeId;
    private Integer experienceYears;
}

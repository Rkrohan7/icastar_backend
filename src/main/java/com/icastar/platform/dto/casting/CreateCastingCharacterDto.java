package com.icastar.platform.dto.casting;

import com.icastar.platform.entity.CastingCharacter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCastingCharacterDto {

    @NotBlank(message = "Character name is required")
    private String name;

    @NotNull(message = "Role type is required")
    private CastingCharacter.RoleType roleType;

    private CastingCharacter.Gender gender = CastingCharacter.Gender.ANY;
    private Integer ageMin;
    private Integer ageMax;
    private String description;
    private Integer requiredCount = 1;
}
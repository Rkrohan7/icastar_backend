package com.icastar.platform.dto.casting;

import com.icastar.platform.entity.CastingCharacter;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CastingCharacterDto {
    private Long id;
    private Long projectId;
    private String projectName;
    private String name;
    private CastingCharacter.RoleType roleType;
    private CastingCharacter.Gender gender;
    private Integer ageMin;
    private Integer ageMax;
    private String description;
    private Integer requiredCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CastingCharacterDto() {}

    public CastingCharacterDto(CastingCharacter character) {
        this.id = character.getId();
        this.projectId = character.getProject().getId();
        this.projectName = character.getProject().getName();
        this.name = character.getName();
        this.roleType = character.getRoleType();
        this.gender = character.getGender();
        this.ageMin = character.getAgeMin();
        this.ageMax = character.getAgeMax();
        this.description = character.getDescription();
        this.requiredCount = character.getRequiredCount();
        this.createdAt = character.getCreatedAt();
        this.updatedAt = character.getUpdatedAt();
    }
}
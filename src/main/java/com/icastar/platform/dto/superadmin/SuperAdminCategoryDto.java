package com.icastar.platform.dto.superadmin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminCategoryDto {

    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String iconUrl;
    private Boolean isActive;
    private Integer sortOrder;
    private Long artistCount;
    private List<FieldDto> fields;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldDto {
        private Long id;
        private String fieldName;
        private String fieldLabel;
        private String fieldType;
        private Boolean isRequired;
        private String options;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCategoryRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String displayName;
        private String description;
        private String iconUrl;
        private Boolean isActive;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCategoryRequest {
        private String name;
        private String displayName;
        private String description;
        private String iconUrl;
        private Boolean isActive;
        private Integer sortOrder;
    }
}

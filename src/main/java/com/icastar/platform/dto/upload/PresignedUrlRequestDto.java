package com.icastar.platform.dto.upload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PresignedUrlRequestDto {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File type is required")
    private String fileType;

    @NotBlank(message = "Upload type is required")
    private String uploadType;

    private String photoType;

    @Positive(message = "File size must be greater than zero")
    private Long fileSize;
}

package com.icastar.platform.dto.artist;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FaceVerificationSubmissionDto {

    @NotBlank(message = "Face verification image URL is required")
    @Size(max = 2048, message = "Face verification image URL must not exceed 2048 characters")
    @Pattern(regexp = "^https?://\\S+$", message = "Face verification image URL must be a valid HTTP or HTTPS URL")
    @JsonAlias("imageUrl")
    private String faceImageUrl;
}

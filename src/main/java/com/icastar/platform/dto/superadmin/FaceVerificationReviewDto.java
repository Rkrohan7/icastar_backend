package com.icastar.platform.dto.superadmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FaceVerificationReviewDto {

    @NotNull(message = "Review status is required")
    private Status status;

    @NotBlank(message = "Review reason is required")
    @Size(max = 2000, message = "Review reason must not exceed 2000 characters")
    private String reason;

    public enum Status {
        APPROVED, REJECTED
    }
}

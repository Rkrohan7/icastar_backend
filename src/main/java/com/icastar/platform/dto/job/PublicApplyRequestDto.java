package com.icastar.platform.dto.job;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PublicApplyRequestDto {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;

    private String address;

    @Min(value = 0, message = "Experience years must be 0 or more")
    private Integer experienceYears;

    private String coverLetter;

    private Double expectedSalary;
}
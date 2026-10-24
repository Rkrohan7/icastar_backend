package com.icastar.platform.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ApplicationEligibilityException extends RuntimeException {

    private final List<String> missingRequirements;
    private final int completionPercentage;

    public ApplicationEligibilityException(List<String> missingRequirements, int completionPercentage) {
        super("Complete the required verifications and profile requirements before applying.");
        this.missingRequirements = List.copyOf(missingRequirements);
        this.completionPercentage = completionPercentage;
    }
}

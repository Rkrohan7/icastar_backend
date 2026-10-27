package com.icastar.platform.util;

import com.icastar.platform.dto.auth.EmailRegisterRequestDto;
import com.icastar.platform.dto.auth.RegisterRequestDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsPasswordMeetingAllSignupRequirements() {
        assertTrue(PasswordPolicy.isValid("Aa1!xxxx"));
        assertTrue(PasswordPolicy.isValid("StrongPass1!"));
        assertTrue(validator.validateProperty(registerRequest("StrongPass1!"), "password").isEmpty());
        RegisterRequestDto legacyRequest = new RegisterRequestDto();
        legacyRequest.setPassword("StrongPass1!");
        assertTrue(validator.validateProperty(legacyRequest, "password").isEmpty());
    }

    @Test
    void rejectsPasswordsMissingSignupRequirements() {
        assertFalse(PasswordPolicy.isValid("weakpass1!"));
        assertFalse(PasswordPolicy.isValid("WEAKPASS1!"));
        assertFalse(PasswordPolicy.isValid("Weakpass!"));
        assertFalse(PasswordPolicy.isValid("Weakpass1"));
        assertFalse(PasswordPolicy.isValid("Aa1!xxx"));
        assertFalse(PasswordPolicy.isValid("Weak1!"));
        assertEquals(PasswordPolicy.MESSAGE,
                validator.validateProperty(registerRequest("weakpass1!"), "password")
                        .iterator().next().getMessage());
        RegisterRequestDto legacyRequest = new RegisterRequestDto();
        legacyRequest.setPassword("weakpass1!");
        assertEquals(PasswordPolicy.MESSAGE,
                validator.validateProperty(legacyRequest, "password").iterator().next().getMessage());
    }

    private static EmailRegisterRequestDto registerRequest(String password) {
        EmailRegisterRequestDto request = new EmailRegisterRequestDto();
        request.setPassword(password);
        return request;
    }
}

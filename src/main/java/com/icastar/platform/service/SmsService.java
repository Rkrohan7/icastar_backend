package com.icastar.platform.service;

import com.icastar.platform.entity.CommunicationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class SmsService {

    private final CommunicationLogService communicationLogService;
    private final RestTemplate restTemplate;

    @Value("${msg91.auth-key:}")
    private String msg91AuthKey;

    @Value("${msg91.template-id:}")
    private String msg91TemplateId;

    @Value("${msg91.otp-url:https://control.msg91.com/api/v5/otp}")
    private String msg91OtpUrl;

    public SmsService(CommunicationLogService communicationLogService, RestTemplateBuilder restTemplateBuilder) {
        this.communicationLogService = communicationLogService;
        this.restTemplate = restTemplateBuilder.build();
    }

    @Transactional
    public void sendOtpSms(String mobile, String otp) {
        sendAuthenticationOtpSms(mobile, otp);
    }

    @Transactional
    public void sendAuthenticationOtpSms(String mobile, String otp) {
        if (msg91AuthKey == null || msg91AuthKey.isBlank()
                || msg91TemplateId == null || msg91TemplateId.isBlank()) {
            throw new IllegalStateException("MSG91 OTP credentials and template ID are not configured.");
        }

        CommunicationLog logEntry = null;
        try {
            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.SMS,
                null,
                mobile,
                "iCastar OTP Verification",
                "Authentication OTP sent.",
                "OTP_VERIFICATION_SMS",
                null,
                "{\"type\":\"verification\"}"
            );

            String normalizedMobile = normalizeIndianMobile(mobile);
            var uri = UriComponentsBuilder.fromUriString(msg91OtpUrl)
                    .queryParam("template_id", msg91TemplateId)
                    .queryParam("mobile", "91" + normalizedMobile)
                    .queryParam("authkey", msg91AuthKey)
                    .queryParam("otp", otp)
                    .build()
                    .encode()
                    .toUri();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    uri, new HttpEntity<>("{}", headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful()
                    || (response.getBody() != null
                    && response.getBody().toLowerCase().contains("\"type\":\"error\""))) {
                throw new IllegalStateException("MSG91 rejected the OTP delivery request.");
            }
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Authentication SMS OTP sent to mobile ending in {}", normalizedMobile.substring(6));
        } catch (Exception e) {
            log.error("Failed to send authentication SMS OTP to mobile ending in {}",
                    mobile != null && mobile.length() >= 4 ? mobile.substring(mobile.length() - 4) : "unknown");
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), "Authentication OTP delivery failed.");
            }
            throw new IllegalStateException("Unable to send SMS verification code.");
        }
    }

    private String normalizeIndianMobile(String mobile) {
        String normalized = mobile == null ? "" : mobile.replaceAll("\\D", "");
        if (normalized.startsWith("91") && normalized.length() == 12) {
            normalized = normalized.substring(2);
        }
        if (!normalized.matches("^[6-9]\\d{9}$")) {
            throw new IllegalArgumentException("Mobile number must be a valid 10-digit Indian number.");
        }
        return normalized;
    }

    @Transactional
    public void sendWelcomeSms(String mobile, String firstName) {
        CommunicationLog logEntry = null;
        try {
            // Create communication log
            String messageText = "Welcome to iCastar, " + firstName + "! Your account is ready.";
            
            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.SMS,
                null,
                mobile,
                "Welcome to iCastar",
                messageText,
                "WELCOME_SMS",
                null,
                "{\"firstName\":\"" + firstName + "\",\"type\":\"welcome\"}"
            );

            // Placeholder for SMS integration
            // Mark as sent (placeholder - in production, use actual SMS provider response)
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Welcome SMS sent to {}: Welcome to iCastar, {}!", mobile, firstName);
            
        } catch (Exception e) {
            log.error("Failed to send welcome SMS to: {}", mobile, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void sendJobAlertSms(String mobile, String artistName, String jobTitle) {
        CommunicationLog logEntry = null;
        try {
            // Create communication log
            String messageText = "New job '" + jobTitle + "' matches your profile. Check your iCastar account!";
            
            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.SMS,
                null,
                mobile,
                "Job Alert",
                messageText,
                "JOB_ALERT_SMS",
                null,
                "{\"artistName\":\"" + artistName + "\",\"jobTitle\":\"" + jobTitle + "\",\"type\":\"job_alert\"}"
            );

            // Placeholder for SMS integration
            // Mark as sent (placeholder - in production, use actual SMS provider response)
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Job alert SMS sent to {}: New job '{}' matches your profile", mobile, jobTitle);
            
        } catch (Exception e) {
            log.error("Failed to send job alert SMS to: {}", mobile, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
        }
    }
}

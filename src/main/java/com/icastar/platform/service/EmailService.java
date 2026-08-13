package com.icastar.platform.service;

import com.icastar.platform.entity.CommunicationLog;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final CommunicationLogService communicationLogService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send HTML email
     */
    @Async
    @Transactional
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        CommunicationLog logEntry = null;
        try {
            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.EMAIL,
                toEmail,
                null,
                subject,
                htmlBody,
                "GENERAL_HTML_EMAIL",
                null,
                "{\"type\":\"html_email\"}"
            );

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);

            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("HTML email sent successfully to: {} with subject: {}", toEmail, subject);

        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}", toEmail, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
        }
    }

    @Async
    @Transactional
    public void sendOtpEmail(String toEmail, String otp) {
        CommunicationLog logEntry = null;
        try {
            // Create communication log
            String messageText = "Your OTP for iCastar verification is: " + otp + 
                               "\n\nThis OTP is valid for 5 minutes." +
                               "\n\nIf you didn't request this OTP, please ignore this email." +
                               "\n\nBest regards,\nThe iCastar Team";
            
            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.EMAIL,
                toEmail,
                null,
                "iCastar - OTP Verification",
                messageText,
                "OTP_VERIFICATION",
                null,
                "{\"otp\":\"" + otp + "\",\"type\":\"verification\"}"
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("iCastar - OTP Verification");
            message.setText(messageText);

            mailSender.send(message);
            
            // Mark as sent
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("OTP email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
            // In production, you might want to throw an exception or handle this differently
        }
    }

    @Async
    @Transactional
    public void sendWelcomeEmail(String toEmail, String firstName) {
        CommunicationLog logEntry = null;
        try {
            // Create communication log
            String messageText = "Dear " + firstName + ",\n\n" +
                               "Welcome to iCastar - your gateway to the entertainment industry!\n\n" +
                               "Your account has been successfully created. You can now:\n" +
                               "- Create your professional profile\n" +
                               "- Browse and apply for jobs\n" +
                               "- Connect with recruiters\n" +
                               "- Showcase your talent\n\n" +
                               "If you have any questions, feel free to contact our support team.\n\n" +
                               "Best regards,\nThe iCastar Team";
            
            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.EMAIL,
                toEmail,
                null,
                "Welcome to iCastar!",
                messageText,
                "WELCOME_EMAIL",
                null,
                "{\"firstName\":\"" + firstName + "\",\"type\":\"welcome\"}"
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to iCastar!");
            message.setText(messageText);

            mailSender.send(message);
            
            // Mark as sent
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Welcome email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
        }
    }

    @Async
    @Transactional
    public void sendJobAlertEmail(String toEmail, String artistName, String jobTitle, String companyName) {
        CommunicationLog logEntry = null;
        try {
            // Create communication log
            String messageText = "Dear " + artistName + ",\n\n" +
                               "A new job opportunity that matches your profile has been posted:\n\n" +
                               "Job Title: " + jobTitle + "\n" +
                               "Company: " + companyName + "\n\n" +
                               "Log in to your iCastar account to view details and apply.\n\n" +
                               "Best regards,\nThe iCastar Team";
            
            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.EMAIL,
                toEmail,
                null,
                "New Job Alert - " + jobTitle,
                messageText,
                "JOB_ALERT",
                null,
                "{\"artistName\":\"" + artistName + "\",\"jobTitle\":\"" + jobTitle + "\",\"companyName\":\"" + companyName + "\",\"type\":\"job_alert\"}"
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("New Job Alert - " + jobTitle);
            message.setText(messageText);

            mailSender.send(message);
            
            // Mark as sent
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Job alert email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send job alert email to: {}", toEmail, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
        }
    }

    @Async
    public void sendInterviewScheduleEmail(String toEmail, String applicantName, String jobTitle,
                                           String companyName, String recruiterName,
                                           LocalDateTime interviewDateTime, String interviewType,
                                           String interviewLocation, String meetingLink, String notes) {
        CommunicationLog logEntry = null;
        try {
            // Format interview date and time
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
            String formattedDate = interviewDateTime.format(dateFormatter);
            String formattedTime = interviewDateTime.format(timeFormatter);

            // Build HTML email
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("<!DOCTYPE html>");
            emailBody.append("<html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
            emailBody.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

            // Header
            emailBody.append("<div style='background-color: #4CAF50; color: white; padding: 30px; text-align: center; border-radius: 5px 5px 0 0;'>");
            emailBody.append("<h1 style='margin: 0; font-size: 28px;'>Interview Scheduled</h1>");
            emailBody.append("</div>");

            // Content
            emailBody.append("<div style='background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;'>");
            emailBody.append("<p style='font-size: 16px;'>Dear ").append(applicantName).append(",</p>");
            emailBody.append("<p style='font-size: 16px;'>Congratulations! Your application for the position of <strong>").append(jobTitle).append("</strong> at <strong>").append(companyName).append("</strong> has been shortlisted.</p>");
            emailBody.append("<p style='font-size: 16px;'>We would like to invite you for an interview with the following details:</p>");

            // Interview Details Box
            emailBody.append("<div style='background-color: white; padding: 20px; border-radius: 5px; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");
            emailBody.append("<div style='margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #e0e0e0;'>");
            emailBody.append("<div style='color: #666; font-size: 14px;'>Date</div>");
            emailBody.append("<div style='font-size: 16px; font-weight: bold; color: #333;'>").append(formattedDate).append("</div>");
            emailBody.append("</div>");

            emailBody.append("<div style='margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #e0e0e0;'>");
            emailBody.append("<div style='color: #666; font-size: 14px;'>Time</div>");
            emailBody.append("<div style='font-size: 16px; font-weight: bold; color: #333;'>").append(formattedTime).append("</div>");
            emailBody.append("</div>");

            emailBody.append("<div style='margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #e0e0e0;'>");
            emailBody.append("<div style='color: #666; font-size: 14px;'>Interview Type</div>");
            emailBody.append("<div style='font-size: 16px; font-weight: bold; color: #333;'>").append(interviewType).append("</div>");
            emailBody.append("</div>");

            if (interviewLocation != null && !interviewLocation.isEmpty()) {
                emailBody.append("<div style='margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #e0e0e0;'>");
                emailBody.append("<div style='color: #666; font-size: 14px;'>Location</div>");
                emailBody.append("<div style='font-size: 16px; font-weight: bold; color: #333;'>").append(interviewLocation).append("</div>");
                emailBody.append("</div>");
            }

            emailBody.append("<div style='margin-bottom: 15px;'>");
            emailBody.append("<div style='color: #666; font-size: 14px;'>Interviewer</div>");
            emailBody.append("<div style='font-size: 16px; font-weight: bold; color: #333;'>").append(recruiterName).append("</div>");
            emailBody.append("</div>");
            emailBody.append("</div>");

            // Meeting Link Button
            if (meetingLink != null && !meetingLink.isEmpty()) {
                emailBody.append("<div style='text-align: center; margin: 30px 0;'>");
                emailBody.append("<a href='").append(meetingLink).append("' style='display: inline-block; background-color: #4CAF50; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; font-size: 16px; font-weight: bold;'>Join Interview</a>");
                emailBody.append("</div>");
            }

            // Additional Notes
            if (notes != null && !notes.isEmpty()) {
                emailBody.append("<div style='background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 3px;'>");
                emailBody.append("<div style='font-weight: bold; margin-bottom: 5px;'>Additional Notes:</div>");
                emailBody.append("<div>").append(notes).append("</div>");
                emailBody.append("</div>");
            }

            // Instructions
            emailBody.append("<div style='background-color: white; padding: 20px; border-radius: 5px; margin: 20px 0;'>");
            emailBody.append("<h3 style='color: #4CAF50; margin-top: 0;'>What to Bring:</h3>");
            emailBody.append("<ul style='padding-left: 20px;'>");
            emailBody.append("<li>Updated resume/portfolio</li>");
            emailBody.append("<li>Valid ID proof</li>");
            emailBody.append("<li>Any relevant certificates or documents</li>");
            emailBody.append("<li>Questions you may have about the role</li>");
            emailBody.append("</ul>");
            emailBody.append("</div>");

            emailBody.append("<p style='font-size: 16px;'>Please confirm your attendance by replying to this email.</p>");
            emailBody.append("<p style='font-size: 16px;'>If you have any questions or need to reschedule, please contact us immediately.</p>");
            emailBody.append("<p style='font-size: 16px;'>We look forward to meeting you!</p>");

            // Footer
            emailBody.append("<div style='margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; text-align: center; color: #666;'>");
            emailBody.append("<p style='margin: 5px 0;'>Best regards,</p>");
            emailBody.append("<p style='margin: 5px 0; font-weight: bold;'>").append(companyName).append("</p>");
            emailBody.append("<p style='margin: 5px 0; font-size: 12px;'>Powered by iCastar</p>");
            emailBody.append("</div>");

            emailBody.append("</div></div></body></html>");

            // Create communication log
            String metadata = String.format("{\"applicantName\":\"%s\",\"jobTitle\":\"%s\",\"companyName\":\"%s\",\"interviewDateTime\":\"%s\",\"interviewType\":\"%s\",\"type\":\"interview_schedule\"}",
                    applicantName, jobTitle, companyName, interviewDateTime.toString(), interviewType);

            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.EMAIL,
                toEmail,
                null,
                "Interview Scheduled - " + jobTitle,
                emailBody.toString(),
                "INTERVIEW_SCHEDULE",
                null,
                metadata
            );

            // Send HTML email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Interview Scheduled - " + jobTitle);
            helper.setText(emailBody.toString(), true); // true = HTML

            mailSender.send(mimeMessage);

            // Mark as sent
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Interview schedule email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send interview schedule email to: {}", toEmail, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
            // Don't throw exception - we don't want to prevent interview scheduling if email fails
            log.warn("Interview will be scheduled despite email failure for: {}", toEmail);
        }
    }

    /**
     * Send email notification when applicant is hired
     */
    @Async
    @Transactional
    public void sendHiredEmail(String toEmail, String applicantName, String jobTitle,
                                String companyName, String notes) {
        CommunicationLog logEntry = null;
        try {
            // Build HTML email
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("<!DOCTYPE html>");
            emailBody.append("<html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
            emailBody.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

            // Header
            emailBody.append("<div style='background-color: #4CAF50; color: white; padding: 30px; text-align: center; border-radius: 5px 5px 0 0;'>");
            emailBody.append("<h1 style='margin: 0; font-size: 28px;'>Congratulations!</h1>");
            emailBody.append("<p style='margin: 10px 0 0 0; font-size: 18px;'>You've Been Selected!</p>");
            emailBody.append("</div>");

            // Content
            emailBody.append("<div style='background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;'>");
            emailBody.append("<p style='font-size: 16px;'>Dear ").append(applicantName).append(",</p>");
            emailBody.append("<p style='font-size: 16px;'>We are thrilled to inform you that you have been <strong style='color: #4CAF50;'>selected</strong> for the position of <strong>").append(jobTitle).append("</strong> at <strong>").append(companyName).append("</strong>!</p>");

            emailBody.append("<div style='background-color: white; padding: 20px; border-radius: 5px; margin: 20px 0; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");
            emailBody.append("<div style='font-size: 48px; margin-bottom: 10px;'>🎉</div>");
            emailBody.append("<div style='font-size: 20px; font-weight: bold; color: #4CAF50;'>Welcome to the Team!</div>");
            emailBody.append("</div>");

            // Notes section
            if (notes != null && !notes.isEmpty()) {
                emailBody.append("<div style='background-color: #e8f5e9; border-left: 4px solid #4CAF50; padding: 15px; margin: 20px 0; border-radius: 3px;'>");
                emailBody.append("<div style='font-weight: bold; margin-bottom: 5px;'>Message from the Recruiter:</div>");
                emailBody.append("<div>").append(notes).append("</div>");
                emailBody.append("</div>");
            }

            emailBody.append("<p style='font-size: 16px;'>Our team will reach out to you shortly with further details about the next steps, including onboarding information and documentation requirements.</p>");
            emailBody.append("<p style='font-size: 16px;'>If you have any questions, please don't hesitate to reach out.</p>");

            // Footer
            emailBody.append("<div style='margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; text-align: center; color: #666;'>");
            emailBody.append("<p style='margin: 5px 0;'>Best regards,</p>");
            emailBody.append("<p style='margin: 5px 0; font-weight: bold;'>").append(companyName).append("</p>");
            emailBody.append("<p style='margin: 5px 0; font-size: 12px;'>Powered by iCastar</p>");
            emailBody.append("</div>");

            emailBody.append("</div></div></body></html>");

            // Create communication log
            String metadata = String.format("{\"applicantName\":\"%s\",\"jobTitle\":\"%s\",\"companyName\":\"%s\",\"result\":\"HIRED\",\"type\":\"interview_result\"}",
                    applicantName, jobTitle, companyName);

            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.EMAIL,
                toEmail,
                null,
                "Congratulations! You've Been Selected - " + jobTitle,
                emailBody.toString(),
                "HIRED_NOTIFICATION",
                null,
                metadata
            );

            // Send HTML email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Congratulations! You've Been Selected - " + jobTitle);
            helper.setText(emailBody.toString(), true);

            mailSender.send(mimeMessage);

            // Mark as sent
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Hired notification email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send hired notification email to: {}", toEmail, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
        }
    }

    /**
     * Send email notification when applicant is rejected
     */
    @Async
    @Transactional
    public void sendRejectionEmail(String toEmail, String applicantName, String jobTitle,
                                    String companyName, String notes) {
        CommunicationLog logEntry = null;
        try {
            // Build HTML email
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("<!DOCTYPE html>");
            emailBody.append("<html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
            emailBody.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

            // Header
            emailBody.append("<div style='background-color: #607D8B; color: white; padding: 30px; text-align: center; border-radius: 5px 5px 0 0;'>");
            emailBody.append("<h1 style='margin: 0; font-size: 28px;'>Application Update</h1>");
            emailBody.append("</div>");

            // Content
            emailBody.append("<div style='background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;'>");
            emailBody.append("<p style='font-size: 16px;'>Dear ").append(applicantName).append(",</p>");
            emailBody.append("<p style='font-size: 16px;'>Thank you for taking the time to interview for the position of <strong>").append(jobTitle).append("</strong> at <strong>").append(companyName).append("</strong>.</p>");
            emailBody.append("<p style='font-size: 16px;'>After careful consideration, we regret to inform you that we have decided to move forward with other candidates whose qualifications more closely match our current requirements.</p>");

            // Notes/Feedback section
            if (notes != null && !notes.isEmpty()) {
                emailBody.append("<div style='background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0; border-radius: 3px;'>");
                emailBody.append("<div style='font-weight: bold; margin-bottom: 5px;'>Feedback:</div>");
                emailBody.append("<div>").append(notes).append("</div>");
                emailBody.append("</div>");
            }

            emailBody.append("<p style='font-size: 16px;'>We encourage you to continue exploring opportunities on iCastar and apply for other positions that match your skills and experience.</p>");
            emailBody.append("<p style='font-size: 16px;'>We appreciate your interest in ").append(companyName).append(" and wish you the best in your career endeavors.</p>");

            // Footer
            emailBody.append("<div style='margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; text-align: center; color: #666;'>");
            emailBody.append("<p style='margin: 5px 0;'>Best regards,</p>");
            emailBody.append("<p style='margin: 5px 0; font-weight: bold;'>").append(companyName).append("</p>");
            emailBody.append("<p style='margin: 5px 0; font-size: 12px;'>Powered by iCastar</p>");
            emailBody.append("</div>");

            emailBody.append("</div></div></body></html>");

            // Create communication log
            String metadata = String.format("{\"applicantName\":\"%s\",\"jobTitle\":\"%s\",\"companyName\":\"%s\",\"result\":\"REJECTED\",\"type\":\"interview_result\"}",
                    applicantName, jobTitle, companyName);

            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.EMAIL,
                toEmail,
                null,
                "Application Update - " + jobTitle,
                emailBody.toString(),
                "REJECTION_NOTIFICATION",
                null,
                metadata
            );

            // Send HTML email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Application Update - " + jobTitle);
            helper.setText(emailBody.toString(), true);

            mailSender.send(mimeMessage);

            // Mark as sent
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Rejection notification email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send rejection notification email to: {}", toEmail, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
        }
    }

    /**
     * Send hire request email to artist
     */
    @Async
    @Transactional
    public void sendHireRequestEmail(String toEmail, String artistName, String recruiterName,
                                     String companyName, String jobTitle, String message, Double offeredSalary) {
        CommunicationLog logEntry = null;
        try {
            // Build HTML email
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("<!DOCTYPE html>");
            emailBody.append("<html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
            emailBody.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

            // Header
            emailBody.append("<div style='background-color: #6366F1; color: white; padding: 30px; text-align: center; border-radius: 5px 5px 0 0;'>");
            emailBody.append("<h1 style='margin: 0; font-size: 28px;'>New Hire Request!</h1>");
            emailBody.append("<p style='margin: 10px 0 0 0; font-size: 18px;'>You've caught someone's attention</p>");
            emailBody.append("</div>");

            // Content
            emailBody.append("<div style='background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;'>");
            emailBody.append("<p style='font-size: 16px;'>Dear ").append(artistName).append(",</p>");
            emailBody.append("<p style='font-size: 16px;'>Great news! <strong>").append(recruiterName).append("</strong> from <strong>").append(companyName).append("</strong> is interested in hiring you for:</p>");

            // Job Details Box
            emailBody.append("<div style='background-color: white; padding: 20px; border-radius: 5px; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");
            emailBody.append("<div style='margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #e0e0e0;'>");
            emailBody.append("<div style='color: #666; font-size: 14px;'>Position</div>");
            emailBody.append("<div style='font-size: 18px; font-weight: bold; color: #6366F1;'>").append(jobTitle).append("</div>");
            emailBody.append("</div>");

            emailBody.append("<div style='margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #e0e0e0;'>");
            emailBody.append("<div style='color: #666; font-size: 14px;'>Company</div>");
            emailBody.append("<div style='font-size: 16px; font-weight: bold; color: #333;'>").append(companyName).append("</div>");
            emailBody.append("</div>");

            if (offeredSalary != null) {
                emailBody.append("<div style='margin-bottom: 15px;'>");
                emailBody.append("<div style='color: #666; font-size: 14px;'>Offered Salary</div>");
                emailBody.append("<div style='font-size: 16px; font-weight: bold; color: #10B981;'>₹").append(String.format("%,.2f", offeredSalary)).append("</div>");
                emailBody.append("</div>");
            }
            emailBody.append("</div>");

            // Message from recruiter
            if (message != null && !message.isEmpty()) {
                emailBody.append("<div style='background-color: #EEF2FF; border-left: 4px solid #6366F1; padding: 15px; margin: 20px 0; border-radius: 3px;'>");
                emailBody.append("<div style='font-weight: bold; margin-bottom: 5px;'>Message from ").append(recruiterName).append(":</div>");
                emailBody.append("<div style='font-style: italic;'>\"").append(message).append("\"</div>");
                emailBody.append("</div>");
            }

            // Call to action
            emailBody.append("<div style='text-align: center; margin: 30px 0;'>");
            emailBody.append("<p style='font-size: 16px;'>Log in to your iCastar account to review this opportunity and respond.</p>");
            emailBody.append("</div>");

            emailBody.append("<p style='font-size: 16px;'>Don't keep them waiting - respond soon to show your interest!</p>");

            // Footer
            emailBody.append("<div style='margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; text-align: center; color: #666;'>");
            emailBody.append("<p style='margin: 5px 0;'>Best regards,</p>");
            emailBody.append("<p style='margin: 5px 0; font-weight: bold;'>The iCastar Team</p>");
            emailBody.append("</div>");

            emailBody.append("</div></div></body></html>");

            // Create communication log
            String metadata = String.format("{\"artistName\":\"%s\",\"recruiterName\":\"%s\",\"companyName\":\"%s\",\"jobTitle\":\"%s\",\"type\":\"hire_request\"}",
                    artistName, recruiterName, companyName, jobTitle);

            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.EMAIL,
                toEmail,
                null,
                "New Hire Request - " + jobTitle,
                emailBody.toString(),
                "HIRE_REQUEST",
                null,
                metadata
            );

            // Send HTML email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("New Hire Request - " + jobTitle + " at " + companyName);
            helper.setText(emailBody.toString(), true);

            mailSender.send(mimeMessage);

            // Mark as sent
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Hire request email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send hire request email to: {}", toEmail, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
        }
    }

    /**
     * Send hire request reminder email to artist
     */
    @Async
    @Transactional
    public void sendHireRequestReminderEmail(String toEmail, String artistName, String recruiterName,
                                              String companyName, String jobTitle) {
        CommunicationLog logEntry = null;
        try {
            // Build HTML email
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("<!DOCTYPE html>");
            emailBody.append("<html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
            emailBody.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

            // Header
            emailBody.append("<div style='background-color: #F59E0B; color: white; padding: 30px; text-align: center; border-radius: 5px 5px 0 0;'>");
            emailBody.append("<h1 style='margin: 0; font-size: 28px;'>Reminder: Pending Hire Request</h1>");
            emailBody.append("</div>");

            // Content
            emailBody.append("<div style='background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;'>");
            emailBody.append("<p style='font-size: 16px;'>Dear ").append(artistName).append(",</p>");
            emailBody.append("<p style='font-size: 16px;'>This is a friendly reminder that you have a pending hire request from <strong>").append(recruiterName).append("</strong> at <strong>").append(companyName).append("</strong> for the position of <strong>").append(jobTitle).append("</strong>.</p>");

            emailBody.append("<div style='background-color: #FEF3C7; border-left: 4px solid #F59E0B; padding: 15px; margin: 20px 0; border-radius: 3px;'>");
            emailBody.append("<div style='font-weight: bold;'>Don't miss this opportunity!</div>");
            emailBody.append("<div>The recruiter is eagerly waiting for your response.</div>");
            emailBody.append("</div>");

            emailBody.append("<p style='font-size: 16px;'>Please log in to your iCastar account to review and respond to this opportunity.</p>");

            // Footer
            emailBody.append("<div style='margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; text-align: center; color: #666;'>");
            emailBody.append("<p style='margin: 5px 0;'>Best regards,</p>");
            emailBody.append("<p style='margin: 5px 0; font-weight: bold;'>The iCastar Team</p>");
            emailBody.append("</div>");

            emailBody.append("</div></div></body></html>");

            // Create communication log
            String metadata = String.format("{\"artistName\":\"%s\",\"recruiterName\":\"%s\",\"companyName\":\"%s\",\"jobTitle\":\"%s\",\"type\":\"hire_request_reminder\"}",
                    artistName, recruiterName, companyName, jobTitle);

            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.EMAIL,
                toEmail,
                null,
                "Reminder: Pending Hire Request - " + jobTitle,
                emailBody.toString(),
                "HIRE_REQUEST_REMINDER",
                null,
                metadata
            );

            // Send HTML email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reminder: Pending Hire Request - " + jobTitle);
            helper.setText(emailBody.toString(), true);

            mailSender.send(mimeMessage);

            // Mark as sent
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Hire request reminder email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send hire request reminder email to: {}", toEmail, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
        }
    }

    /**
     * Send hire request response notification to recruiter
     */
    @Async
    @Transactional
    public void sendHireRequestResponseEmail(String toEmail, String recruiterName, String artistName,
                                              String jobTitle, com.icastar.platform.entity.HireRequest.HireRequestStatus status,
                                              String artistResponse) {
        CommunicationLog logEntry = null;
        try {
            boolean isAccepted = status == com.icastar.platform.entity.HireRequest.HireRequestStatus.ACCEPTED;
            String statusText = isAccepted ? "Accepted" : "Declined";
            String headerColor = isAccepted ? "#10B981" : "#EF4444";

            // Build HTML email
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("<!DOCTYPE html>");
            emailBody.append("<html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
            emailBody.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

            // Header
            emailBody.append("<div style='background-color: ").append(headerColor).append("; color: white; padding: 30px; text-align: center; border-radius: 5px 5px 0 0;'>");
            emailBody.append("<h1 style='margin: 0; font-size: 28px;'>Hire Request ").append(statusText).append("</h1>");
            emailBody.append("</div>");

            // Content
            emailBody.append("<div style='background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;'>");
            emailBody.append("<p style='font-size: 16px;'>Dear ").append(recruiterName).append(",</p>");

            if (isAccepted) {
                emailBody.append("<p style='font-size: 16px;'>Great news! <strong>").append(artistName).append("</strong> has <strong style='color: #10B981;'>accepted</strong> your hire request for <strong>").append(jobTitle).append("</strong>!</p>");
                emailBody.append("<div style='background-color: #D1FAE5; border-left: 4px solid #10B981; padding: 15px; margin: 20px 0; border-radius: 3px;'>");
                emailBody.append("<div style='font-weight: bold;'>Next Steps:</div>");
                emailBody.append("<div>Log in to your iCastar account to proceed with the hiring process.</div>");
                emailBody.append("</div>");
            } else {
                emailBody.append("<p style='font-size: 16px;'><strong>").append(artistName).append("</strong> has <strong style='color: #EF4444;'>declined</strong> your hire request for <strong>").append(jobTitle).append("</strong>.</p>");
            }

            // Artist response if provided
            if (artistResponse != null && !artistResponse.isEmpty()) {
                String bgColor = isAccepted ? "#D1FAE5" : "#FEE2E2";
                String borderColor = isAccepted ? "#10B981" : "#EF4444";
                emailBody.append("<div style='background-color: ").append(bgColor).append("; border-left: 4px solid ").append(borderColor).append("; padding: 15px; margin: 20px 0; border-radius: 3px;'>");
                emailBody.append("<div style='font-weight: bold; margin-bottom: 5px;'>Response from ").append(artistName).append(":</div>");
                emailBody.append("<div style='font-style: italic;'>\"").append(artistResponse).append("\"</div>");
                emailBody.append("</div>");
            }

            if (!isAccepted) {
                emailBody.append("<p style='font-size: 16px;'>Don't be discouraged! There are many talented artists on iCastar. Keep searching and you'll find the perfect fit for your project.</p>");
            }

            // Footer
            emailBody.append("<div style='margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; text-align: center; color: #666;'>");
            emailBody.append("<p style='margin: 5px 0;'>Best regards,</p>");
            emailBody.append("<p style='margin: 5px 0; font-weight: bold;'>The iCastar Team</p>");
            emailBody.append("</div>");

            emailBody.append("</div></div></body></html>");

            // Create communication log
            String metadata = String.format("{\"recruiterName\":\"%s\",\"artistName\":\"%s\",\"jobTitle\":\"%s\",\"status\":\"%s\",\"type\":\"hire_request_response\"}",
                    recruiterName, artistName, jobTitle, statusText);

            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.EMAIL,
                toEmail,
                null,
                "Hire Request " + statusText + " - " + artistName,
                emailBody.toString(),
                "HIRE_REQUEST_RESPONSE",
                null,
                metadata
            );

            // Send HTML email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Hire Request " + statusText + " - " + artistName);
            helper.setText(emailBody.toString(), true);

            mailSender.send(mimeMessage);

            // Mark as sent
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Hire request response email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send hire request response email to: {}", toEmail, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
        }
    }

    /**
     * Send profile completion reminder email
     */
    @Async
    @Transactional
    public void sendProfileCompletionReminderEmail(String toEmail, String userName, String userRole, String profileUrl) {
        CommunicationLog logEntry = null;
        try {
            // Build HTML email
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("<!DOCTYPE html>");
            emailBody.append("<html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");
            emailBody.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

            // Header
            emailBody.append("<div style='background-color: #6366F1; color: white; padding: 30px; text-align: center; border-radius: 5px 5px 0 0;'>");
            emailBody.append("<h1 style='margin: 0; font-size: 28px;'>Complete Your Profile</h1>");
            emailBody.append("<p style='margin: 10px 0 0 0; font-size: 18px;'>Unlock more opportunities on iCastar</p>");
            emailBody.append("</div>");

            // Content
            emailBody.append("<div style='background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px;'>");
            emailBody.append("<p style='font-size: 16px;'>Dear ").append(userName != null ? userName : "User").append(",</p>");
            emailBody.append("<p style='font-size: 16px;'>We noticed that your iCastar profile is not yet complete. A complete profile helps you:</p>");

            // Benefits
            emailBody.append("<div style='background-color: white; padding: 20px; border-radius: 5px; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");
            if ("ARTIST".equalsIgnoreCase(userRole)) {
                emailBody.append("<ul style='padding-left: 20px; margin: 0;'>");
                emailBody.append("<li style='margin-bottom: 10px;'>Get discovered by top recruiters and casting directors</li>");
                emailBody.append("<li style='margin-bottom: 10px;'>Increase your chances of getting selected for auditions</li>");
                emailBody.append("<li style='margin-bottom: 10px;'>Showcase your talent with a professional portfolio</li>");
                emailBody.append("<li style='margin-bottom: 10px;'>Receive relevant job alerts matching your skills</li>");
                emailBody.append("</ul>");
            } else {
                emailBody.append("<ul style='padding-left: 20px; margin: 0;'>");
                emailBody.append("<li style='margin-bottom: 10px;'>Build trust with artists by showcasing your company</li>");
                emailBody.append("<li style='margin-bottom: 10px;'>Get better responses to your job postings</li>");
                emailBody.append("<li style='margin-bottom: 10px;'>Access verified and talented artists</li>");
                emailBody.append("<li style='margin-bottom: 10px;'>Streamline your hiring process</li>");
                emailBody.append("</ul>");
            }
            emailBody.append("</div>");

            // Call to action
            emailBody.append("<div style='text-align: center; margin: 30px 0;'>");
            emailBody.append("<a href='").append(profileUrl != null ? profileUrl : "https://icastar.com/profile").append("' style='display: inline-block; background-color: #6366F1; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; font-size: 16px; font-weight: bold;'>Complete Your Profile Now</a>");
            emailBody.append("</div>");

            emailBody.append("<p style='font-size: 16px;'>Completing your profile only takes a few minutes and can make a big difference in your iCastar experience!</p>");

            // Footer
            emailBody.append("<div style='margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; text-align: center; color: #666;'>");
            emailBody.append("<p style='margin: 5px 0;'>Best regards,</p>");
            emailBody.append("<p style='margin: 5px 0; font-weight: bold;'>The iCastar Team</p>");
            emailBody.append("<p style='margin: 15px 0 5px 0; font-size: 12px; color: #999;'>If you have any questions, contact us at support@icastar.com</p>");
            emailBody.append("</div>");

            emailBody.append("</div></div></body></html>");

            // Create communication log
            String metadata = String.format("{\"userName\":\"%s\",\"userRole\":\"%s\",\"type\":\"profile_completion_reminder\"}",
                    userName != null ? userName : "", userRole != null ? userRole : "");

            logEntry = communicationLogService.createLog(
                CommunicationLog.CommunicationType.EMAIL,
                toEmail,
                null,
                "Complete Your iCastar Profile",
                emailBody.toString(),
                "PROFILE_COMPLETION_REMINDER",
                null,
                metadata
            );

            // Send HTML email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Complete Your iCastar Profile");
            helper.setText(emailBody.toString(), true);

            mailSender.send(mimeMessage);

            // Mark as sent
            communicationLogService.markAsSent(logEntry.getId(), null);
            log.info("Profile completion reminder email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send profile completion reminder email to: {}", toEmail, e);
            if (logEntry != null) {
                communicationLogService.markAsFailed(logEntry.getId(), e.getMessage());
            }
        }
    }
}

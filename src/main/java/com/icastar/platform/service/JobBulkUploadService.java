package com.icastar.platform.service;

import com.icastar.platform.dto.superadmin.BulkUploadResponseDto;
import com.icastar.platform.entity.Job;
import com.icastar.platform.entity.User;
import com.icastar.platform.repository.JobRepository;
import com.icastar.platform.repository.UserRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobBulkUploadService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    private static final int BATCH_SIZE = 500;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // Column indices (0-based)
    private static final int COL_TITLE = 0;
    private static final int COL_DESCRIPTION = 1;
    private static final int COL_REQUIREMENTS = 2;
    private static final int COL_JOB_TYPE = 3;
    private static final int COL_EXPERIENCE_LEVEL = 4;
    private static final int COL_LOCATION = 5;
    private static final int COL_IS_REMOTE = 6;
    private static final int COL_BUDGET_MIN = 7;
    private static final int COL_BUDGET_MAX = 8;
    private static final int COL_CURRENCY = 9;
    private static final int COL_SKILLS_REQUIRED = 10;
    private static final int COL_APPLICATION_DEADLINE = 11;
    private static final int COL_START_DATE = 12;
    private static final int COL_RECRUITER_ID = 13;

    public BulkUploadResponseDto process(MultipartFile file) {
        BulkUploadResponseDto response = BulkUploadResponseDto.builder()
                .totalRows(0)
                .successCount(0)
                .failureCount(0)
                .errors(new ArrayList<>())
                .build();

        // Validate file
        if (file.isEmpty()) {
            response.addError(0, "file", "File is empty");
            return response;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            response.addError(0, "file", "File size exceeds 10MB limit");
            return response;
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            response.addError(0, "file", "Invalid filename");
            return response;
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!List.of("xlsx", "xls", "csv").contains(extension)) {
            response.addError(0, "file", "Invalid file type. Allowed: xlsx, xls, csv");
            return response;
        }

        try {
            List<String[]> rows;
            if ("csv".equals(extension)) {
                rows = parseCsv(file.getInputStream());
            } else {
                rows = parseExcel(file.getInputStream(), extension);
            }

            if (rows.isEmpty()) {
                response.addError(0, "file", "File contains no data");
                return response;
            }

            // Skip header row
            int totalDataRows = rows.size() - 1;
            response.setTotalRows(totalDataRows);

            if (totalDataRows == 0) {
                response.addError(0, "file", "File contains only header row");
                return response;
            }

            // Process rows in batches
            List<Job> jobsToSave = new ArrayList<>();

            for (int i = 1; i < rows.size(); i++) {
                int rowNumber = i; // 1-based row number (excluding header)
                String[] row = rows.get(i);

                try {
                    Job job = parseRow(row, rowNumber, response);
                    if (job != null) {
                        jobsToSave.add(job);
                    }
                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", rowNumber, e.getMessage());
                    response.addError(rowNumber, "row", "Unexpected error: " + e.getMessage());
                    response.setFailureCount(response.getFailureCount() + 1);
                }

                // Save in batches
                if (jobsToSave.size() >= BATCH_SIZE) {
                    saveBatch(jobsToSave, response);
                    jobsToSave.clear();
                }
            }

            // Save remaining jobs
            if (!jobsToSave.isEmpty()) {
                saveBatch(jobsToSave, response);
            }

            log.info("Bulk upload completed. Total: {}, Success: {}, Failed: {}",
                    response.getTotalRows(), response.getSuccessCount(), response.getFailureCount());

        } catch (IOException | CsvException e) {
            log.error("Error reading file: {}", e.getMessage());
            response.addError(0, "file", "Error reading file: " + e.getMessage());
        }

        return response;
    }

    private List<String[]> parseCsv(InputStream inputStream) throws IOException, CsvException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVReader csvReader = new CSVReader(reader)) {
            return csvReader.readAll();
        }
    }

    private List<String[]> parseExcel(InputStream inputStream, String extension) throws IOException {
        List<String[]> rows = new ArrayList<>();

        Workbook workbook = "xlsx".equals(extension)
                ? new XSSFWorkbook(inputStream)
                : new HSSFWorkbook(inputStream);

        Sheet sheet = workbook.getSheetAt(0);
        DataFormatter formatter = new DataFormatter();

        for (Row row : sheet) {
            int lastColumn = Math.max(row.getLastCellNum(), COL_RECRUITER_ID + 1);
            String[] rowData = new String[lastColumn];

            for (int cn = 0; cn < lastColumn; cn++) {
                Cell cell = row.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                rowData[cn] = cell != null ? formatter.formatCellValue(cell).trim() : "";
            }
            rows.add(rowData);
        }

        workbook.close();
        return rows;
    }

    private Job parseRow(String[] row, int rowNumber, BulkUploadResponseDto response) {
        boolean hasError = false;

        // Required: title
        String title = getCell(row, COL_TITLE);
        if (title.isEmpty()) {
            response.addError(rowNumber, "title", "Title is required");
            hasError = true;
        }

        // Required: description
        String description = getCell(row, COL_DESCRIPTION);
        if (description.isEmpty()) {
            response.addError(rowNumber, "description", "Description is required");
            hasError = true;
        }

        // Required: recruiterId
        String recruiterIdStr = getCell(row, COL_RECRUITER_ID);
        Long recruiterId = null;
        User recruiter = null;

        if (recruiterIdStr.isEmpty()) {
            response.addError(rowNumber, "recruiterId", "Recruiter ID is required");
            hasError = true;
        } else {
            try {
                recruiterId = Long.parseLong(recruiterIdStr);
                recruiter = userRepository.findById(recruiterId).orElse(null);
                if (recruiter == null) {
                    response.addError(rowNumber, "recruiterId", "Recruiter not found with ID: " + recruiterId);
                    hasError = true;
                }
                // Note: No role check - admin bulk upload is trusted operation
            } catch (NumberFormatException e) {
                response.addError(rowNumber, "recruiterId", "Invalid recruiter ID: " + recruiterIdStr);
                hasError = true;
            }
        }

        // Optional: jobType
        Job.JobType jobType = null;
        String jobTypeStr = getCell(row, COL_JOB_TYPE);
        if (!jobTypeStr.isEmpty()) {
            try {
                jobType = Job.JobType.valueOf(jobTypeStr.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                response.addError(rowNumber, "jobType", "Invalid job type: " + jobTypeStr +
                        ". Valid values: FULL_TIME, PART_TIME, CONTRACT, FREELANCE, INTERNSHIP, PROJECT_BASED");
                hasError = true;
            }
        }

        // Optional: experienceLevel
        Job.ExperienceLevel experienceLevel = null;
        String expLevelStr = getCell(row, COL_EXPERIENCE_LEVEL);
        if (!expLevelStr.isEmpty()) {
            try {
                experienceLevel = Job.ExperienceLevel.valueOf(expLevelStr.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                response.addError(rowNumber, "experienceLevel", "Invalid experience level: " + expLevelStr +
                        ". Valid values: ENTRY_LEVEL, MID_LEVEL, SENIOR_LEVEL, EXPERT_LEVEL");
                hasError = true;
            }
        }

        // Optional: budgetMin
        BigDecimal budgetMin = null;
        String budgetMinStr = getCell(row, COL_BUDGET_MIN);
        if (!budgetMinStr.isEmpty()) {
            try {
                budgetMin = new BigDecimal(budgetMinStr.replace(",", ""));
            } catch (NumberFormatException e) {
                response.addError(rowNumber, "budgetMin", "Invalid budget min: " + budgetMinStr);
                hasError = true;
            }
        }

        // Optional: budgetMax
        BigDecimal budgetMax = null;
        String budgetMaxStr = getCell(row, COL_BUDGET_MAX);
        if (!budgetMaxStr.isEmpty()) {
            try {
                budgetMax = new BigDecimal(budgetMaxStr.replace(",", ""));
            } catch (NumberFormatException e) {
                response.addError(rowNumber, "budgetMax", "Invalid budget max: " + budgetMaxStr);
                hasError = true;
            }
        }

        // Optional: isRemote
        Boolean isRemote = false;
        String isRemoteStr = getCell(row, COL_IS_REMOTE);
        if (!isRemoteStr.isEmpty()) {
            isRemote = "true".equalsIgnoreCase(isRemoteStr) || "yes".equalsIgnoreCase(isRemoteStr) || "1".equals(isRemoteStr);
        }

        // Optional: applicationDeadline
        LocalDate applicationDeadline = null;
        String deadlineStr = getCell(row, COL_APPLICATION_DEADLINE);
        if (!deadlineStr.isEmpty()) {
            applicationDeadline = parseDate(deadlineStr);
            if (applicationDeadline == null) {
                response.addError(rowNumber, "applicationDeadline", "Invalid date format: " + deadlineStr + ". Use YYYY-MM-DD");
                hasError = true;
            }
        }

        // Optional: startDate
        LocalDate startDate = null;
        String startDateStr = getCell(row, COL_START_DATE);
        if (!startDateStr.isEmpty()) {
            startDate = parseDate(startDateStr);
            if (startDate == null) {
                response.addError(rowNumber, "startDate", "Invalid date format: " + startDateStr + ". Use YYYY-MM-DD");
                hasError = true;
            }
        }

        if (hasError) {
            response.setFailureCount(response.getFailureCount() + 1);
            return null;
        }

        // Build job entity
        Job job = new Job();
        job.setTitle(title);
        job.setDescription(description);
        job.setRequirements(getCell(row, COL_REQUIREMENTS));
        job.setJobType(jobType != null ? jobType : Job.JobType.FULL_TIME);
        job.setExperienceLevel(experienceLevel);
        job.setLocation(getCell(row, COL_LOCATION));
        job.setIsRemote(isRemote);
        job.setBudgetMin(budgetMin);
        job.setBudgetMax(budgetMax);
        job.setCurrency(getCell(row, COL_CURRENCY).isEmpty() ? "INR" : getCell(row, COL_CURRENCY));
        job.setSkillsRequired(formatAsJsonArray(getCell(row, COL_SKILLS_REQUIRED)));
        job.setApplicationDeadline(applicationDeadline);
        job.setStartDate(startDate);
        job.setRecruiter(recruiter);
        job.setStatus(Job.JobStatus.ACTIVE);
        job.setPublishedAt(LocalDateTime.now());
        job.setIsActive(true);

        // Set audit fields explicitly (in case JPA auditing is not working)
        LocalDateTime now = LocalDateTime.now();
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        // Set default values for other fields
        job.setViewsCount(0);
        job.setApplicationsCount(0);
        job.setIsUrgent(false);
        job.setIsFeatured(false);

        return job;
    }

    private void saveBatch(List<Job> jobs, BulkUploadResponseDto response) {
        try {
            jobRepository.saveAll(jobs);
            response.setSuccessCount(response.getSuccessCount() + jobs.size());
            log.info("Successfully saved batch of {} jobs", jobs.size());
        } catch (Exception e) {
            log.error("Error saving batch: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            // If batch fails, try saving individually
            for (Job job : jobs) {
                try {
                    jobRepository.save(job);
                    response.setSuccessCount(response.getSuccessCount() + 1);
                    log.info("Saved job: {}", job.getTitle());
                } catch (Exception ex) {
                    String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    log.error("Error saving job '{}': {} - {}", job.getTitle(), ex.getClass().getSimpleName(), errorMsg, ex);
                    response.setFailureCount(response.getFailureCount() + 1);
                    response.addError(0, "job", "Failed to save job '" + job.getTitle() + "': " + errorMsg);
                }
            }
        }
    }

    private String getCell(String[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index].trim();
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        // Try multiple date formats
        String[] formats = {"yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "MM/dd/yyyy"};
        for (String format : formats) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String formatAsJsonArray(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isEmpty()) {
            return null;
        }
        String[] items = commaSeparated.split(",");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) {
            sb.append("\"").append(items[i].trim()).append("\"");
            if (i < items.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}

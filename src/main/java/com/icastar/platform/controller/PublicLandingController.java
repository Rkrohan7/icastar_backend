package com.icastar.platform.controller;

import com.icastar.platform.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public Landing", description = "Public landing page endpoints - No authentication required")
public class PublicLandingController {

    private final SystemSettingService systemSettingService;

    /**
     * Get landing page stats (counters)
     * GET /api/public/landing-stats
     * No authentication required
     *
     * Response uses field names without "landing" prefix:
     * - activeArtists
     * - castingDirectors
     * - successfulAuditions
     * - successRate
     *
     * Cached for 5-10 minutes since these numbers rarely change.
     */
    @Operation(summary = "Get landing page stats", description = "Get landing page counters - No authentication required")
    @GetMapping("/landing-stats")
    public ResponseEntity<Map<String, Object>> getLandingStats() {
        try {
            log.info("Fetching landing page stats");

            // Get stats from service (cached)
            Map<String, Object> stats = systemSettingService.getLandingStats();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching landing stats", e);

            // Return default values on error
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("enabled", true);
            defaultStats.put("activeArtists", 10000);
            defaultStats.put("castingDirectors", 250);
            defaultStats.put("successfulAuditions", 10000);
            defaultStats.put("successRate", 95);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", defaultStats);

            return ResponseEntity.ok(response);
        }
    }
}

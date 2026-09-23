package com.icastar.platform.controller;

import com.icastar.platform.dto.BlogDto;
import com.icastar.platform.service.BlogService;
import com.icastar.platform.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public Landing", description = "Public landing page endpoints - No authentication required")
public class PublicLandingController {

    private final SystemSettingService systemSettingService;
    private final BlogService blogService;

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
            defaultStats.put("blogsEnabled", true);
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

    /**
     * Get published blogs for landing page
     * GET /api/public/blogs?size=3
     * No authentication required
     * Returns only PUBLISHED blogs, ordered by publishedAt descending
     */
    @Operation(summary = "Get published blogs", description = "Get published blogs for landing page - No authentication required")
    @GetMapping("/blogs")
    public ResponseEntity<Map<String, Object>> getPublishedBlogs(
            @RequestParam(required = false) Integer size) {
        try {
            log.info("Fetching published blogs, size: {}", size);

            List<BlogDto.PublicBlogDto> blogs = blogService.getPublishedBlogs(size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blogs);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching published blogs", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching blogs");

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get single published blog by slug
     * GET /api/public/blogs/{slug}
     * No authentication required
     * Returns 404 if blog not found or is draft
     */
    @Operation(summary = "Get blog by slug", description = "Get a single published blog by slug - No authentication required")
    @GetMapping("/blogs/{slug}")
    public ResponseEntity<Map<String, Object>> getBlogBySlug(@PathVariable String slug) {
        try {
            log.info("Fetching blog by slug: {}", slug);

            BlogDto.PublicBlogDto blog = blogService.getPublishedBlogBySlug(slug)
                    .orElse(null);

            if (blog == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Blog not found");
                return ResponseEntity.status(404).body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", blog);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching blog by slug: {}", slug, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching blog");

            return ResponseEntity.internalServerError().body(response);
        }
    }
}

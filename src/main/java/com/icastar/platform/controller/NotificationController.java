package com.icastar.platform.controller;

import com.icastar.platform.entity.User;
import com.icastar.platform.service.NotificationService;
import com.icastar.platform.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Slf4j
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    /**
     * Get all notifications for the logged-in user
     * SECURITY: userId extracted from JWT/SecurityContext
     *
     * GET /api/notifications?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        try {
            // SECURITY: Extract user from authentication context (JWT/SecurityContext)
            // Never accept userId from request params to prevent data leakage
            if (authentication == null || authentication.getName() == null) {
                log.warn("Unauthorized access attempt to /notifications - no authentication");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Fetching notifications for user: {}", email);

            Map<String, Object> notifications = notificationService.getUserNotifications(user, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", notifications);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            log.error("Error fetching notifications: {}", errorMessage, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", errorMessage);

            if (errorMessage != null && errorMessage.contains("not found")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Unexpected error fetching notifications: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Mark a specific notification as read
     * SECURITY: Validates notification belongs to logged-in user
     *
     * PATCH /api/notifications/:id/read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            // SECURITY: Extract user from authentication context
            if (authentication == null || authentication.getName() == null) {
                log.warn("Unauthorized access attempt to mark notification as read - no authentication");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Marking notification {} as read for user: {}", id, email);

            // Service layer validates notification ownership
            notificationService.markAsRead(id, user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification marked as read");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            log.error("Error marking notification as read: {}", errorMessage, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", errorMessage);

            if (errorMessage != null && errorMessage.contains("Unauthorized")) {
                return ResponseEntity.status(403).body(response);
            } else if (errorMessage != null && errorMessage.contains("not found")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Unexpected error marking notification as read: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Mark all notifications as read for the logged-in user
     *
     * POST /api/notifications/mark-all-read
     */
    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        try {
            // SECURITY: Extract user from authentication context
            if (authentication == null || authentication.getName() == null) {
                log.warn("Unauthorized access attempt to mark all notifications as read - no authentication");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Marking all notifications as read for user: {}", email);

            notificationService.markAllAsRead(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All notifications marked as read");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error marking all notifications as read: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to mark all notifications as read");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get unread notification count
     *
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        try {
            // SECURITY: Extract user from authentication context
            if (authentication == null || authentication.getName() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Long unreadCount = notificationService.countUnread(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("unreadCount", unreadCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching unread count: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch unread count");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Delete a notification
     *
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                log.warn("Unauthorized access attempt to delete notification - no authentication");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Authentication required");
                return ResponseEntity.status(401).body(response);
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Deleting notification {} for user: {}", id, email);

            notificationService.deleteNotification(id, user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification deleted successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            log.error("Error deleting notification: {}", errorMessage, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", errorMessage);

            if (errorMessage != null && errorMessage.contains("Unauthorized")) {
                return ResponseEntity.status(403).body(response);
            } else if (errorMessage != null && errorMessage.contains("not found")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Unexpected error deleting notification: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An unexpected error occurred");
            return ResponseEntity.status(500).body(response);
        }
    }
}

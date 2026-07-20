package com.icastar.platform.service;

import com.icastar.platform.entity.Notification;
import com.icastar.platform.entity.User;
import com.icastar.platform.repository.NotificationRepository;
import com.icastar.platform.config.CacheNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Create a new notification
     * SECURITY: userId is derived from authenticated user, NOT from request params
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Notification createNotification(User user, Notification.NotificationType type,
                                          String title, String message,
                                          Notification.Priority priority, String actionUrl,
                                          String metadata) {
        try {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setPriority(priority != null ? priority : Notification.Priority.MEDIUM);
            notification.setIsRead(false);
            notification.setSentAt(LocalDateTime.now());
            notification.setActionUrl(actionUrl);
            notification.setMetadata(metadata);
            notification.setEmailSent(false);
            notification.setPushSent(false);

            Notification savedNotification = notificationRepository.save(notification);

            log.info("Notification created - Type: {}, User ID: {}, Title: {}",
                    type, user.getId(), title);

            return savedNotification;
        } catch (Exception e) {
            log.error("Error creating notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create notification: " + e.getMessage(), e);
        }
    }

    /**
     * Get all notifications for a user with pagination
     * SECURITY: Only returns notifications belonging to the authenticated user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserNotifications(User user, Pageable pageable) {
        try {
            Page<Notification> notificationsPage = notificationRepository.findByUserOrderBySentAtDesc(user, pageable);

            List<Map<String, Object>> notifications = notificationsPage.getContent().stream()
                    .map(this::mapNotificationToDto)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("notifications", notifications);
            result.put("totalElements", notificationsPage.getTotalElements());
            result.put("totalPages", notificationsPage.getTotalPages());
            result.put("currentPage", notificationsPage.getNumber());
            result.put("size", notificationsPage.getSize());
            result.put("unreadCount", countUnread(user.getId()));

            log.debug("Fetched {} notifications for user {}", notifications.size(), user.getId());

            return result;
        } catch (Exception e) {
            log.error("Error fetching notifications for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to fetch notifications: " + e.getMessage(), e);
        }
    }

    /**
     * Get unread notifications count for a user
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.NOTIFICATION_UNREAD, key = "#userId")
    public Long countUnread(Long userId) {
        log.debug("Cache MISS: Loading unread count for user: {}", userId);
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * Mark notification as read
     * SECURITY: Validates notification belongs to the user before marking as read
     */
    @Transactional
    @CacheEvict(value = CacheNames.NOTIFICATION_UNREAD, key = "#user.id")
    public Notification markAsRead(Long notificationId, User user) {
        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            // SECURITY CHECK: Verify notification belongs to this user
            if (!notification.getUser().getId().equals(user.getId())) {
                log.warn("Unauthorized attempt to mark notification {} as read by user {}",
                        notificationId, user.getId());
                throw new RuntimeException("Unauthorized: This notification does not belong to you");
            }

            if (!notification.getIsRead()) {
                notification.setIsRead(true);
                notification.setReadAt(LocalDateTime.now());
                notification.setUpdatedAt(LocalDateTime.now());

                Notification savedNotification = notificationRepository.save(notification);

                log.info("Notification {} marked as read by user {}", notificationId, user.getId());

                return savedNotification;
            }

            return notification;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error marking notification {} as read: {}", notificationId, e.getMessage(), e);
            throw new RuntimeException("Failed to mark notification as read: " + e.getMessage(), e);
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    @CacheEvict(value = CacheNames.NOTIFICATION_UNREAD, key = "#user.id")
    public void markAllAsRead(User user) {
        try {
            List<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(user.getId());
            LocalDateTime now = LocalDateTime.now();

            unreadNotifications.forEach(notification -> {
                notification.setIsRead(true);
                notification.setReadAt(now);
                notification.setUpdatedAt(now);
            });

            notificationRepository.saveAll(unreadNotifications);

            log.info("Marked {} notifications as read for user {}", unreadNotifications.size(), user.getId());
        } catch (Exception e) {
            log.error("Error marking all notifications as read for user {}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to mark all notifications as read: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a notification
     * SECURITY: Validates notification belongs to the user before deleting
     */
    @Transactional
    public void deleteNotification(Long notificationId, User user) {
        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            // SECURITY CHECK: Verify notification belongs to this user
            if (!notification.getUser().getId().equals(user.getId())) {
                log.warn("Unauthorized attempt to delete notification {} by user {}",
                        notificationId, user.getId());
                throw new RuntimeException("Unauthorized: This notification does not belong to you");
            }

            notificationRepository.delete(notification);
            log.info("Notification {} deleted by user {}", notificationId, user.getId());

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting notification {}: {}", notificationId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete notification: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to map Notification entity to DTO
     */
    private Map<String, Object> mapNotificationToDto(Notification notification) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", notification.getId());
        dto.put("type", notification.getType().name());
        dto.put("title", notification.getTitle());
        dto.put("message", notification.getMessage());
        dto.put("priority", notification.getPriority().name());
        dto.put("isRead", notification.getIsRead());
        dto.put("sentAt", notification.getSentAt());
        dto.put("readAt", notification.getReadAt());
        dto.put("actionUrl", notification.getActionUrl());
        dto.put("metadata", notification.getMetadata());
        return dto;
    }
}

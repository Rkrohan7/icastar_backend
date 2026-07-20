package com.icastar.platform.config;

/**
 * Constants for cache names used throughout the application.
 * Centralized cache name definitions for consistency and easy maintenance.
 */
public final class CacheNames {

    private CacheNames() {
        // Prevent instantiation
    }

    // ==================== STATIC/MASTER DATA (24 HOUR TTL) ====================

    /** Artist type master data - rarely changes */
    public static final String ARTIST_TYPES = "artist-types";

    /** Artist type field definitions - rarely changes */
    public static final String ARTIST_TYPE_FIELDS = "artist-type-fields";

    /** Subscription plans - admin managed */
    public static final String SUBSCRIPTION_PLANS = "subscription-plans";

    /** Recruiter categories - admin managed */
    public static final String RECRUITER_CATEGORIES = "recruiter-categories";

    // ==================== AGGREGATED DATA (12 HOUR TTL) ====================

    /** Skills list aggregated from artists and jobs */
    public static final String SKILLS_LIST = "skills-list";

    // ==================== PROFILE DATA (4 HOUR TTL) ====================

    /** Artists grouped by type */
    public static final String ARTISTS_BY_TYPE = "artists-by-type";

    /** Featured/top artists */
    public static final String ARTISTS_FEATURED = "artists-featured";

    /** Active artist profiles */
    public static final String ARTISTS_ACTIVE = "artists-active";

    /** Top recruiters by various metrics */
    public static final String RECRUITERS_TOP = "recruiters-top";

    // ==================== LISTINGS DATA (2 HOUR TTL) ====================

    /** Active job listings */
    public static final String JOBS_ACTIVE = "jobs-active";

    /** Featured job listings */
    public static final String JOBS_FEATURED = "jobs-featured";

    /** All casting calls */
    public static final String CASTING_CALLS = "casting-calls";

    /** Open casting calls */
    public static final String CASTING_CALLS_OPEN = "casting-calls-open";

    // ==================== INDIVIDUAL ENTITIES (1 HOUR TTL) ====================

    /** Individual job by ID */
    public static final String JOBS_BY_ID = "jobs-by-id";

    /** Count statistics (users, jobs, applications) */
    public static final String COUNT_STATS = "count-stats";

    // ==================== USER DATA (30 MINUTE TTL) ====================

    /** User lookup by email */
    public static final String USER_BY_EMAIL = "user-by-email";

    /** User lookup by ID */
    public static final String USER_BY_ID = "user-by-id";

    /** Users grouped by role */
    public static final String USERS_BY_ROLE = "users-by-role";

    // ==================== DASHBOARD STATISTICS (15 MINUTE TTL) ====================

    /** Super admin dashboard statistics */
    public static final String DASHBOARD_STATS = "dashboard-stats";

    // ==================== USER-SPECIFIC DASHBOARDS (10 MINUTE TTL) ====================

    /** Recruiter dashboard data */
    public static final String DASHBOARD_RECRUITER = "dashboard-recruiter";

    /** Artist dashboard data */
    public static final String DASHBOARD_ARTIST = "dashboard-artist";

    // ==================== FREQUENTLY CHANGING DATA (5 MINUTE TTL) ====================

    /** Job applications */
    public static final String JOB_APPLICATIONS = "job-applications";

    /** Auditions */
    public static final String AUDITIONS = "auditions";

    // ==================== ARTIST PROFILE DATA (10 MINUTE TTL) ====================

    /** Artist profile by user ID */
    public static final String ARTIST_PROFILE_BY_USER = "artist-profile-by-user";

    /** Artist profile by ID */
    public static final String ARTIST_PROFILE_BY_ID = "artist-profile-by-id";

    // ==================== BOOKMARKS & APPLICATIONS (5 MINUTE TTL) ====================

    /** Bookmarked jobs by user */
    public static final String BOOKMARKS_BY_USER = "bookmarks-by-user";

    /** Bookmark check cache */
    public static final String BOOKMARK_CHECK = "bookmark-check";

    /** Application check cache */
    public static final String APPLICATION_CHECK = "application-check";

    /** Notification unread count */
    public static final String NOTIFICATION_UNREAD = "notification-unread";
}

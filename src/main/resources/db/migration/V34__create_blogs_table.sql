-- V34: Create blogs table

CREATE TABLE blogs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL UNIQUE,
    excerpt VARCHAR(300),
    content TEXT NOT NULL,
    cover_image_url VARCHAR(500),
    author_name VARCHAR(100),
    tags VARCHAR(500),
    status ENUM('DRAFT','PUBLISHED') DEFAULT 'DRAFT',
    published_at TIMESTAMP NULL,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_blog_status (status),
    INDEX idx_blog_slug (slug)
);

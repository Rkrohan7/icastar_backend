package com.icastar.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icastar.platform.dto.BlogDto;
import com.icastar.platform.entity.Blog;
import com.icastar.platform.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlogService {

    private final BlogRepository blogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a new blog
     */
    public Blog createBlog(BlogDto.CreateBlogRequest request, Long createdBy) {
        log.info("Creating new blog with title: {}", request.getTitle());

        Blog blog = new Blog();
        blog.setTitle(request.getTitle());
        blog.setSlug(generateUniqueSlug(request.getSlug()));
        blog.setExcerpt(request.getExcerpt());
        blog.setContent(request.getContent());
        blog.setCoverImageUrl(request.getCoverImageUrl());
        blog.setAuthorName(request.getAuthorName());
        blog.setCreatedBy(createdBy);

        // Convert tags list to JSON string using ObjectMapper
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            try {
                blog.setTags(objectMapper.writeValueAsString(request.getTags()));
            } catch (JsonProcessingException e) {
                log.error("Error converting tags to JSON", e);
                throw new RuntimeException("Error processing blog tags");
            }
        }

        // Handle status and publishedAt
        if (request.getStatus() != null) {
            blog.setStatus(request.getStatus());
            if (request.getStatus() == Blog.BlogStatus.PUBLISHED) {
                blog.setPublishedAt(LocalDateTime.now());
            }
        } else {
            blog.setStatus(Blog.BlogStatus.DRAFT);
        }

        return blogRepository.save(blog);
    }

    /**
     * Update an existing blog
     */
    public Blog updateBlog(Long blogId, BlogDto.UpdateBlogRequest request) {
        log.info("Updating blog id: {}", blogId);

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found"));

        if (request.getTitle() != null) {
            blog.setTitle(request.getTitle());
        }

        // Handle slug update with uniqueness check
        if (request.getSlug() != null && !request.getSlug().equals(blog.getSlug())) {
            blog.setSlug(generateUniqueSlug(request.getSlug(), blogId));
        }

        if (request.getExcerpt() != null) {
            blog.setExcerpt(request.getExcerpt());
        }

        if (request.getContent() != null) {
            blog.setContent(request.getContent());
        }

        if (request.getCoverImageUrl() != null) {
            blog.setCoverImageUrl(request.getCoverImageUrl());
        }

        if (request.getAuthorName() != null) {
            blog.setAuthorName(request.getAuthorName());
        }

        // Convert tags list to JSON string using ObjectMapper
        if (request.getTags() != null) {
            try {
                blog.setTags(objectMapper.writeValueAsString(request.getTags()));
            } catch (JsonProcessingException e) {
                log.error("Error converting tags to JSON", e);
                throw new RuntimeException("Error processing blog tags");
            }
        }

        // Handle status change and publishedAt
        if (request.getStatus() != null && request.getStatus() != blog.getStatus()) {
            Blog.BlogStatus oldStatus = blog.getStatus();
            blog.setStatus(request.getStatus());

            // Set publishedAt only when first published
            if (request.getStatus() == Blog.BlogStatus.PUBLISHED && blog.getPublishedAt() == null) {
                blog.setPublishedAt(LocalDateTime.now());
            }
            // If unpublished and republished, keep old publishedAt date
        }

        return blogRepository.save(blog);
    }

    /**
     * Delete a blog
     */
    public void deleteBlog(Long blogId) {
        log.info("Deleting blog id: {}", blogId);
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found"));
        blogRepository.delete(blog);
    }

    /**
     * Get all blogs for admin (includes drafts)
     */
    @Transactional(readOnly = true)
    public Page<BlogDto> getAllBlogsForAdmin(Blog.BlogStatus status, Pageable pageable) {
        Page<Blog> blogs;

        if (status != null) {
            blogs = blogRepository.findByStatus(status, pageable);
        } else {
            blogs = blogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return blogs.map(this::convertToDto);
    }

    /**
     * Get blog by id for admin
     */
    @Transactional(readOnly = true)
    public Optional<BlogDto> getBlogById(Long blogId) {
        return blogRepository.findById(blogId).map(this::convertToDto);
    }

    /**
     * Get published blogs for public API
     */
    @Transactional(readOnly = true)
    public List<BlogDto.PublicBlogDto> getPublishedBlogs(Integer size) {
        Pageable pageable;
        if (size != null && size > 0) {
            pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        } else {
            pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "publishedAt"));
        }

        List<Blog> blogs = blogRepository.findByStatusOrderByPublishedAtDesc(Blog.BlogStatus.PUBLISHED, pageable);
        return blogs.stream()
                .map(this::convertToPublicDto)
                .collect(Collectors.toList());
    }

    /**
     * Get single published blog by slug for public API
     */
    @Transactional(readOnly = true)
    public Optional<BlogDto.PublicBlogDto> getPublishedBlogBySlug(String slug) {
        return blogRepository.findBySlugAndStatus(slug, Blog.BlogStatus.PUBLISHED)
                .map(this::convertToPublicDto);
    }

    /**
     * Generate unique slug
     * If slug exists, append -2, -3, etc.
     */
    private String generateUniqueSlug(String baseSlug) {
        return generateUniqueSlug(baseSlug, null);
    }

    private String generateUniqueSlug(String baseSlug, Long excludeId) {
        String slug = baseSlug;
        int counter = 2;

        while (true) {
            Optional<Blog> existing = blogRepository.findBySlug(slug);
            if (existing.isEmpty()) {
                return slug;
            }
            // If updating and the existing slug belongs to the same blog, it's ok
            if (excludeId != null && existing.get().getId().equals(excludeId)) {
                return slug;
            }
            slug = baseSlug + "-" + counter;
            counter++;
        }
    }

    /**
     * Convert Blog entity to BlogDto
     */
    private BlogDto convertToDto(Blog blog) {
        return BlogDto.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .excerpt(blog.getExcerpt())
                .content(blog.getContent())
                .coverImageUrl(blog.getCoverImageUrl())
                .authorName(blog.getAuthorName())
                .tags(parseTagsFromJson(blog.getTags()))
                .status(blog.getStatus())
                .publishedAt(blog.getPublishedAt())
                .createdBy(blog.getCreatedBy())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .build();
    }

    /**
     * Convert Blog entity to PublicBlogDto
     */
    private BlogDto.PublicBlogDto convertToPublicDto(Blog blog) {
        return BlogDto.PublicBlogDto.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .excerpt(blog.getExcerpt())
                .content(blog.getContent())
                .coverImageUrl(blog.getCoverImageUrl())
                .authorName(blog.getAuthorName())
                .tags(parseTagsFromJson(blog.getTags()))
                .publishedAt(blog.getPublishedAt())
                .build();
    }

    /**
     * Parse tags JSON string to List<String> using ObjectMapper
     */
    private List<String> parseTagsFromJson(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing tags JSON: {}", tagsJson, e);
            return new ArrayList<>();
        }
    }
}

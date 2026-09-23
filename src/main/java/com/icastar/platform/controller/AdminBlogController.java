package com.icastar.platform.controller;

import com.icastar.platform.dto.BlogDto;
import com.icastar.platform.entity.Blog;
import com.icastar.platform.entity.User;
import com.icastar.platform.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/blogs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Blog Management", description = "Blog management endpoints for admins")
public class AdminBlogController {

    private final BlogService blogService;

    @Operation(summary = "Get all blogs", description = "Get all blogs including drafts with pagination")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) Blog.BlogStatus status) {

        log.info("Fetching all blogs - page: {}, size: {}, status: {}", page, size, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BlogDto> blogs = blogService.getAllBlogsForAdmin(status, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", blogs.getContent());
        response.put("totalPages", blogs.getTotalPages());
        response.put("totalElements", blogs.getTotalElements());
        response.put("currentPage", blogs.getNumber());
        response.put("size", blogs.getSize());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get blog by ID", description = "Get a single blog by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBlogById(@PathVariable Long id) {
        log.info("Fetching blog with id: {}", id);

        BlogDto blog = blogService.getBlogById(id)
                .orElseThrow(() -> new RuntimeException("Blog not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", blog);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a new blog", description = "Create a new blog post")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBlog(
            @RequestBody BlogDto.CreateBlogRequest request,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();
        log.info("Admin {} creating new blog: {}", admin.getEmail(), request.getTitle());

        Blog blog = blogService.createBlog(request, admin.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Blog created successfully");
        response.put("data", blogService.getBlogById(blog.getId()).orElse(null));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update a blog", description = "Update an existing blog post")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateBlog(
            @PathVariable Long id,
            @RequestBody BlogDto.UpdateBlogRequest request,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();
        log.info("Admin {} updating blog id: {}", admin.getEmail(), id);

        Blog blog = blogService.updateBlog(id, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Blog updated successfully");
        response.put("data", blogService.getBlogById(blog.getId()).orElse(null));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a blog", description = "Delete a blog post")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBlog(
            @PathVariable Long id,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();
        log.info("Admin {} deleting blog id: {}", admin.getEmail(), id);

        blogService.deleteBlog(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Blog deleted successfully");

        return ResponseEntity.ok(response);
    }
}

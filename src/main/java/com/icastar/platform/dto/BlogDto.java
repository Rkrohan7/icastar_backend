package com.icastar.platform.dto;

import com.icastar.platform.entity.Blog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogDto {

    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String coverImageUrl;
    private String authorName;
    private List<String> tags;
    private Blog.BlogStatus status;
    private LocalDateTime publishedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBlogRequest {
        private String title;
        private String slug;
        private String excerpt;
        private String content;
        private String coverImageUrl;
        private String authorName;
        private List<String> tags;
        private Blog.BlogStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateBlogRequest {
        private String title;
        private String slug;
        private String excerpt;
        private String content;
        private String coverImageUrl;
        private String authorName;
        private List<String> tags;
        private Blog.BlogStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicBlogDto {
        private Long id;
        private String title;
        private String slug;
        private String excerpt;
        private String content;
        private String coverImageUrl;
        private String authorName;
        private List<String> tags;
        private LocalDateTime publishedAt;
    }
}
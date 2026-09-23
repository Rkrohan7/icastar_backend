package com.icastar.platform.repository;

import com.icastar.platform.entity.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    Optional<Blog> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Blog> findByStatus(Blog.BlogStatus status, Pageable pageable);

    Page<Blog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Blog> findByStatusOrderByPublishedAtDesc(Blog.BlogStatus status, Pageable pageable);

    Optional<Blog> findBySlugAndStatus(String slug, Blog.BlogStatus status);
}

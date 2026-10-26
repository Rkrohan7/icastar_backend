package com.icastar.platform.repository;

import com.icastar.platform.entity.RecruiterCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruiterCategoryRepository extends JpaRepository<RecruiterCategory, Long> {
    
    Optional<RecruiterCategory> findByName(String name);

    Optional<RecruiterCategory> findByDisplayNameIgnoreCase(String displayName);
    
    List<RecruiterCategory> findByIsActiveTrueOrderBySortOrder();
    
    @Query("SELECT rc FROM RecruiterCategory rc WHERE rc.isActive = true ORDER BY rc.sortOrder")
    List<RecruiterCategory> findAllActiveCategories();
}

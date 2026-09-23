package com.icastar.platform.repository;

import com.icastar.platform.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    List<SystemSetting> findByCategory(String category);

    @Query("SELECT s FROM SystemSetting s WHERE s.settingKey IN :keys")
    List<SystemSetting> findBySettingKeyIn(@Param("keys") List<String> keys);

    boolean existsBySettingKey(String settingKey);
}

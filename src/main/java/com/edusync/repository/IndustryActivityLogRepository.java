package com.edusync.repository;

import com.edusync.model.IndustryActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndustryActivityLogRepository extends JpaRepository<IndustryActivityLog, Long> {

    List<IndustryActivityLog> findTop50ByOrderByTimestampDesc();

    List<IndustryActivityLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);
}

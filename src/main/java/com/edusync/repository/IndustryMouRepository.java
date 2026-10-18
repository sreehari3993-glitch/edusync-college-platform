package com.edusync.repository;

import com.edusync.model.IndustryMou;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IndustryMouRepository extends JpaRepository<IndustryMou, Long> {

    List<IndustryMou> findByStatus(IndustryMou.MouStatus status);

    List<IndustryMou> findByCompanyId(Long companyId);

    List<IndustryMou> findAllByOrderByCreatedAtDesc();

    long countByStatus(IndustryMou.MouStatus status);

    @Query("SELECT m FROM IndustryMou m WHERE m.status = 'ACTIVE' AND m.validTo <= :thresholdDate")
    List<IndustryMou> findExpiringMous(@Param("thresholdDate") LocalDate thresholdDate);
}

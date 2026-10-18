package com.edusync.repository;

import com.edusync.model.IndustryVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndustryVisitRepository extends JpaRepository<IndustryVisit, Long> {

    List<IndustryVisit> findByProposedByIdOrderByCreatedAtDesc(Long proposedById);

    List<IndustryVisit> findByDepartmentOrderByCreatedAtDesc(String department);

    List<IndustryVisit> findByStage(IndustryVisit.VisitStage stage);

    List<IndustryVisit> findByStatus(IndustryVisit.VisitStatus status);

    List<IndustryVisit> findAllByOrderByCreatedAtDesc();

    long countByStatus(IndustryVisit.VisitStatus status);
}

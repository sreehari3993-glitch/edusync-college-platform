package com.edusync.repository;

import com.edusync.model.InternshipApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternshipApplicationRepository extends JpaRepository<InternshipApplication, Long> {

    List<InternshipApplication> findByStudentIdOrderByAppliedAtDesc(Long studentId);

    List<InternshipApplication> findByInternshipIdOrderByAppliedAtDesc(Long internshipId);

    Optional<InternshipApplication> findByInternshipIdAndStudentId(Long internshipId, Long studentId);

    boolean existsByInternshipIdAndStudentId(Long internshipId, Long studentId);

    List<InternshipApplication> findByStatus(InternshipApplication.ApplicationStatus status);

    long countByInternshipId(Long internshipId);
}

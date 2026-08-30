package com.edusync.repository;

import com.edusync.model.AdvisorRequest;
import com.edusync.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvisorRequestRepository extends JpaRepository<AdvisorRequest, Long> {

    List<AdvisorRequest> findByFacultyAndStatusOrderByCreatedAtDesc(User faculty, AdvisorRequest.Status status);

    List<AdvisorRequest> findByFacultyOrderByCreatedAtDesc(User faculty);

    List<AdvisorRequest> findByStudentOrderByCreatedAtDesc(User student);

    Optional<AdvisorRequest> findFirstByStudentOrderByCreatedAtDesc(User student);

    Optional<AdvisorRequest> findByStudentAndFacultyAndStatus(User student, User faculty, AdvisorRequest.Status status);

    long countByFacultyAndStatus(User faculty, AdvisorRequest.Status status);
}

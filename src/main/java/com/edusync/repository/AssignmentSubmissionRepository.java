package com.edusync.repository;

import com.edusync.model.Assignment;
import com.edusync.model.AssignmentSubmission;
import com.edusync.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    List<AssignmentSubmission> findByAssignment(Assignment assignment);
    List<AssignmentSubmission> findByStudentOrderBySubmittedAtDesc(User student);
    Optional<AssignmentSubmission> findByAssignmentAndStudent(Assignment assignment, User student);
}

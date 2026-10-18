package com.edusync.repository;

import com.edusync.model.Internship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternshipRepository extends JpaRepository<Internship, Long> {

    List<Internship> findByStatus(Internship.InternshipStatus status);

    List<Internship> findByCompanyId(Long companyId);

    List<Internship> findAllByOrderByCreatedAtDesc();

    long countByStatus(Internship.InternshipStatus status);

    @Query("SELECT i FROM Internship i WHERE i.status = 'OPEN' AND (i.departments IS NULL OR i.departments LIKE %:dept%) ORDER BY i.deadline ASC")
    List<Internship> findOpenByDepartment(@Param("dept") String dept);
}

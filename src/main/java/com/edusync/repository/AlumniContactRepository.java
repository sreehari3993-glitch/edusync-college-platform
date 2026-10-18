package com.edusync.repository;

import com.edusync.model.AlumniContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlumniContactRepository extends JpaRepository<AlumniContact, Long> {

    List<AlumniContact> findAllByOrderByCreatedAtDesc();

    List<AlumniContact> findByCurrentCompanyContainingIgnoreCase(String company);

    List<AlumniContact> findByDepartment(String department);

    List<AlumniContact> findByWillingToMentorTrue();

    List<AlumniContact> findByWillingToReferTrue();
}

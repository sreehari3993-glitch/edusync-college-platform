package com.edusync.repository;

import com.edusync.model.IndustryProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndustryProjectRepository extends JpaRepository<IndustryProject, Long> {

    List<IndustryProject> findAllByOrderByCreatedAtDesc();

    List<IndustryProject> findByCompanyId(Long companyId);

    List<IndustryProject> findByStatus(IndustryProject.ProjectStatus status);

    List<IndustryProject> findByDepartment(String department);
}

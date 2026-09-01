package com.edusync.repository;

import com.edusync.model.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    List<StudyMaterial> findByDepartmentAndSemesterOrderByCreatedAtDesc(String department, String semester);
    List<StudyMaterial> findByDepartmentOrderByCreatedAtDesc(String department);
    List<StudyMaterial> findAllByOrderByCreatedAtDesc();
}

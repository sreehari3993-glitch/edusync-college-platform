package com.edusync.repository;

import com.edusync.model.GuestLecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuestLectureRepository extends JpaRepository<GuestLecture, Long> {

    List<GuestLecture> findAllByOrderByDateDesc();

    List<GuestLecture> findByStatusOrderByDateDesc(GuestLecture.LectureStatus status);

    List<GuestLecture> findByDepartmentsContainingIgnoreCase(String department);
}

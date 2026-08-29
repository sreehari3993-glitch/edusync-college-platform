package com.edusync.service;

import com.edusync.dto.AppDto;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.model.InternalMark;
import com.edusync.model.User;
import com.edusync.repository.InternalMarkRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalMarkService {

    private final InternalMarkRepository internalMarkRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public List<InternalMark> saveBatchMarks(Long facultyId, AppDto.BatchInternalMarkDto dto) {
        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty", facultyId));

        String series = dto.getSeriesName() != null ? dto.getSeriesName() : "Series I";
        String subject = dto.getSubjectName() != null ? dto.getSubjectName() : "Core Subject";
        String dept = dto.getDepartment() != null ? dto.getDepartment() : (faculty.getDepartment() != null ? faculty.getDepartment() : "CSE");

        List<InternalMark> savedList = new ArrayList<>();
        if (dto.getMarks() != null) {
            for (AppDto.InternalMarkItem item : dto.getMarks()) {
                if (item.getStudentId() == null) continue;
                User student = userRepository.findById(item.getStudentId()).orElse(null);
                if (student == null) continue;

                double test = item.getTestMarks() != null ? Math.max(0, Math.min(30, item.getTestMarks())) : 0.0;
                double assign = item.getAssignmentMarks() != null ? Math.max(0, Math.min(10, item.getAssignmentMarks())) : 0.0;
                double prac = item.getPracticalMarks() != null ? Math.max(0, Math.min(10, item.getPracticalMarks())) : 0.0;
                double total = test + assign + prac;

                String grade = calculateGrade(total);

                InternalMark mark = internalMarkRepository
                        .findByStudentIdAndSubjectNameAndSeriesName(student.getId(), subject, series)
                        .orElse(InternalMark.builder()
                                .student(student)
                                .subjectName(subject)
                                .seriesName(series)
                                .build());

                mark.setTestMarks(test);
                mark.setAssignmentMarks(assign);
                mark.setPracticalMarks(prac);
                mark.setTotalMarks(total);
                mark.setMaxMarks(50.0);
                mark.setGrade(grade);
                mark.setSemester(student.getSemester() != null ? student.getSemester() : "S2");
                mark.setDepartment(dept);
                mark.setEnteredBy(faculty.getName());
                mark.setUpdatedAt(LocalDateTime.now());

                savedList.add(internalMarkRepository.save(mark));
            }
        }

        auditService.log(
                "MARKS_SAVED",
                faculty.getEmail(),
                faculty.getName(),
                faculty.getRole(),
                "InternalMark",
                series,
                "Saved " + savedList.size() + " marks records for " + series + " (" + dept + ")"
        );

        return savedList;
    }

    public List<InternalMark> getMarksForStudent(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        return internalMarkRepository.findByStudentOrderByCreatedAtDesc(student);
    }

    public List<InternalMark> getMarksByDepartmentAndSeries(String department, String series) {
        return internalMarkRepository.findByDepartmentAndSeriesNameOrderByCreatedAtDesc(department, series);
    }

    private String calculateGrade(double total) {
        if (total >= 45) return "A+";
        if (total >= 40) return "A";
        if (total >= 35) return "B+";
        if (total >= 30) return "B";
        if (total >= 25) return "C";
        if (total >= 20) return "D";
        return "F";
    }
}

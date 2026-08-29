package com.edusync.service;

import com.edusync.dto.AppDto;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.model.Attendance;
import com.edusync.model.Subject;
import com.edusync.model.User;
import com.edusync.repository.AttendanceRepository;
import com.edusync.repository.SubjectRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Attendance service.
 * Faculty marks attendance; students view summaries with % per subject.
 */
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepo;
    private final UserRepository userRepo;
    private final SubjectRepository subjectRepo;

    // ─── Faculty: Mark attendance ─────────────────────────────────────────

    @Transactional
    public Attendance markAttendance(Long facultyId, AppDto.AttendanceMark dto) {
        User student = userRepo.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", dto.getStudentId()));

        Subject subject = null;
        if (dto.getSubjectId() != null) {
            subject = subjectRepo.findById(dto.getSubjectId()).orElse(null);
        }
        if (subject == null) {
            String dept = student.getDepartment() != null && !student.getDepartment().trim().isEmpty() ? student.getDepartment().trim() : "CSE";
            String sem = student.getSemester() != null && !student.getSemester().trim().isEmpty() ? student.getSemester().trim() : "S2";
            List<Subject> subjects = subjectRepo.findByDepartmentAndSemester(dept, sem);
            if (!subjects.isEmpty()) {
                subject = subjects.get(0);
            } else {
                subject = subjectRepo.save(Subject.builder()
                        .name("Programming in C & Data Structures")
                        .code(dept + "201")
                        .department(dept)
                        .semester(sem)
                        .credits(4)
                        .active(true)
                        .build());
            }
        }

        User faculty = userRepo.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty", facultyId));

        // Update if already marked, else create new
        Attendance existing = attendanceRepo
                .findByStudentSubjectDate(student, subject, dto.getDate())
                .orElse(null);

        if (existing != null) {
            existing.setStatus(dto.getStatus());
            existing.setMarkedBy(faculty);
            return attendanceRepo.save(existing);
        }

        Attendance att = Attendance.builder()
                .student(student)
                .subject(subject)
                .markedBy(faculty)
                .date(dto.getDate())
                .status(dto.getStatus())
                .periodNumber(dto.getPeriodNumber())
                .build();

        return attendanceRepo.save(att);
    }

    // ─── Student: Get attendance summary per subject ───────────────────────

    public List<AppDto.AttendanceSummary> getStudentSummary(Long studentId) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        // Find all subjects the student has attendance records for
        List<Attendance> allRecords = attendanceRepo.findByStudent(student);

        // Group by subject
        List<Subject> subjects = allRecords.stream()
                .map(Attendance::getSubject)
                .distinct()
                .toList();

        List<AppDto.AttendanceSummary> summaries = new ArrayList<>();
        for (Subject subject : subjects) {
            long total  = attendanceRepo.countTotal(student, subject);
            long present = attendanceRepo.countPresent(student, subject);
            double pct   = total > 0 ? (present * 100.0 / total) : 0;

            summaries.add(AppDto.AttendanceSummary.builder()
                    .subjectName(subject.getName())
                    .subjectCode(subject.getCode())
                    .totalClasses(total)
                    .attended(present)
                    .percentage(Math.round(pct * 10.0) / 10.0)
                    .build());
        }
        return summaries;
    }

    // ─── Faculty: View attendance for a subject on a date ─────────────────

    public List<Attendance> getAttendanceBySubjectAndDate(Long subjectId, LocalDate date) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
        return attendanceRepo.findBySubjectAndDate(subject, date);
    }

    // ─── Student: Raw records ─────────────────────────────────────────────

    public List<Attendance> getStudentAttendance(Long studentId) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        return attendanceRepo.findByStudent(student);
    }
}

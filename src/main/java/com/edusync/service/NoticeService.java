package com.edusync.service;

import com.edusync.dto.AppDto;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.model.Notice;
import com.edusync.model.User;
import com.edusync.repository.NoticeRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notice board service — create, list, delete notices.
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepo;
    private final UserRepository userRepo;
    private final AuditService auditService;

    @Transactional
    public Notice createNotice(Long adminId, AppDto.NoticeDto dto) {
        User creator = userRepo.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminId));

        Notice notice = Notice.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory() != null ? dto.getCategory() : Notice.Category.ACADEMIC)
                .visibility(dto.getVisibility() != null ? dto.getVisibility() : Notice.Visibility.ENTIRE_COLLEGE)
                .createdBy(creator)
                .expiresAt(dto.getExpiresAt())
                .build();

        Notice saved = noticeRepo.save(notice);

        auditService.log(
                "NOTICE_PUBLISHED",
                creator.getEmail(),
                creator.getName(),
                creator.getRole(),
                "Notice",
                saved.getId().toString(),
                "Published " + saved.getCategory() + " notice: \"" + saved.getTitle() + "\""
        );

        return saved;
    }

    public List<Notice> getActiveNotices() {
        return noticeRepo.findActiveNotices(LocalDateTime.now());
    }

    @Transactional
    public void deleteNotice(Long id) {
        Notice notice = noticeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", id));
        notice.setActive(false);
        noticeRepo.save(notice);

        auditService.log(
                "NOTICE_DELETED",
                "admin@tkmit.ac.in",
                "Administrator",
                User.Role.ADMIN,
                "Notice",
                id.toString(),
                "Archived notice #" + id + " (\"" + notice.getTitle() + "\")"
        );
    }

    public List<Notice> getAllNotices() {
        return noticeRepo.findByActiveTrueOrderByCreatedAtDesc();
    }
}

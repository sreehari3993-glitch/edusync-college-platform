package com.edusync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object for Real-Time SSE Notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {

    private String id;
    private String type;         // LEAVE_UPDATE, LEAVE_SUBMITTED, NOTICE_PUBLISHED, ASSIGNMENT_NEW, GRIEVANCE_REPLY
    private String title;
    private String message;
    private String link;          // optional navigation target e.g. #page-leaves
    private String actorName;
    private String department;
    private String targetRole;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private Map<String, Object> metadata;
}

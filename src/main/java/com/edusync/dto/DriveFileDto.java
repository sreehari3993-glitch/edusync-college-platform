package com.edusync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriveFileDto {
    private String fileId;
    private String fileName;
    private String originalFileName;
    private String mimeType;
    private long fileSize;
    private String subfolder;
    private String webViewLink;
    private String downloadUrl;
    private String category;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}

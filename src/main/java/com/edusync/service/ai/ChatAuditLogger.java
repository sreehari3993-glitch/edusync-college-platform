package com.edusync.service.ai;

import com.edusync.dto.AiDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service responsible for asynchronous logging of chat interactions.
 * Persists student chat logs to JSONL and human-readable audit files in ktu-docs/.
 */
@Slf4j
@Service
public class ChatAuditLogger {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Persists student chat logs to JSONL and human-readable audit files asynchronously.
     */
    @Async
    public void recordChatAuditLog(Long userId, String query, AiDto.ChatResponse response) {
        if (query == null || query.isBlank() || response == null) {
            return;
        }

        try {
            Path logDir = Path.of("ktu-docs");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            // 1. Append structured JSONL
            Path jsonlPath = logDir.resolve("student_chat_audit_log.jsonl");
            Map<String, Object> logEntry = new LinkedHashMap<>();
            logEntry.put("timestamp", LocalDateTime.now().toString());
            logEntry.put("userId", userId);
            logEntry.put("userQuery", query);
            logEntry.put("intent", response.getIntent());
            logEntry.put("fromLocalLlm", response.isFromLocalLlm());
            logEntry.put("reply", response.getReply());

            String jsonLine = OBJECT_MAPPER.writeValueAsString(logEntry) + System.lineSeparator();
            Files.writeString(jsonlPath, jsonLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            // 2. Append Human-Readable Summary
            Path txtPath = logDir.resolve("student_chat_history.txt");
            String humanEntry = String.format(
                    "[%s] User: %s | Intent: %s\nSTUDENT: %s\nAI AGENT:\n%s\n%s\n\n",
                    LocalDateTime.now().format(DATE_TIME_FORMATTER),
                    userId != null ? "ID#" + userId : "Guest/Student",
                    response.getIntent(),
                    query,
                    response.getReply(),
                    "--------------------------------------------------------------------------------"
            );
            Files.writeString(txtPath, humanEntry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.warn("Failed to write chat audit log: {}", e.getMessage());
        }
    }
}

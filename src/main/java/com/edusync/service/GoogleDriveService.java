package com.edusync.service;

import com.edusync.rag.RagService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Service for bi-directional synchronization with Google Drive:
 * 1. Uploads live student chat histories & mistake audit logs to the Google Drive folder.
 * 2. Downloads new training documents (PDF, Excel, CSV, TXT) from Google Drive to ./ktu-docs/ for RAG ingestion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveService {

    private final RagService ragService;

    @Value("${google.drive.folder-id:1pNjlidTeVa71u0FH4_-PBHhVG6jTtiI4}")
    private String folderId;

    @Value("${google.drive.credentials-path:credentials.json}")
    private String credentialsPath;

    @Value("${google.drive.auto-sync.enabled:true}")
    private boolean autoSyncEnabled;

    private Drive driveService;
    private boolean isConfigured = false;

    @PostConstruct
    public void init() {
        try {
            Path credPath = Path.of(credentialsPath);
            Path altCredPath = Path.of("credentials.json.json");
            InputStream in = null;

            if (Files.exists(credPath)) {
                in = new FileInputStream(credPath.toFile());
            } else if (Files.exists(altCredPath)) {
                in = new FileInputStream(altCredPath.toFile());
            } else {
                String envCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                if (envCreds != null && Files.exists(Path.of(envCreds))) {
                    in = new FileInputStream(envCreds);
                }
            }

            if (in != null) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                        .createScoped(Collections.singletonList(DriveScopes.DRIVE));
                NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                this.driveService = new Drive.Builder(httpTransport, GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                        .setApplicationName("EduSync-Campus-AI")
                        .build();
                this.isConfigured = true;
                log.info("☁️ [Google Drive] Successfully connected to Google Drive API (Target Folder: {})", folderId);
                
                // Trigger initial sync in background
                syncAll();
            } else {
                log.info("ℹ️ [Google Drive] credentials.json not found in root directory. To enable auto-upload to Google Drive folder [{}], place your Google Service Account JSON file as 'credentials.json'.", folderId);
            }
        } catch (Exception e) {
            log.warn("⚠️ [Google Drive] Initialization notice: {}", e.getMessage());
        }
    }

    /**
     * Bi-directional sync scheduled every 10 minutes (600,000 ms).
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 30000)
    public void scheduledSync() {
        if (autoSyncEnabled) {
            syncAll();
        }
    }

    /**
     * Executes full sync: Uploads chat logs -> Downloads new training files -> Triggers RAG index.
     */
    public Map<String, Object> syncAll() {
        if (!isConfigured) {
            init();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if (!isConfigured || driveService == null) {
            result.put("status", "NOT_CONFIGURED");
            result.put("message", "Place 'credentials.json' in root directory and share folder '" + folderId + "' with your service account email.");
            return result;
        }

        try {
            log.info("☁️ [Google Drive] Starting bi-directional cloud sync for folder: {}...", folderId);

            // 1. Upload local student chat logs to Google Drive
            int uploadedCount = uploadChatLogsToDrive();
            result.put("uploadedFiles", uploadedCount);

            // 2. Download new training documents from Google Drive to ktu-docs/
            List<String> downloadedFiles = downloadTrainingFilesFromDrive();
            result.put("downloadedFiles", downloadedFiles);

            // 3. Trigger RAG re-index if new files arrived
            if (!downloadedFiles.isEmpty()) {
                ragService.ingestLocalDocuments();
            }

            result.put("status", "SUCCESS");
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            log.info("✅ [Google Drive] Cloud sync complete (Uploaded: {}, Downloaded: {})", uploadedCount, downloadedFiles.size());
        } catch (Exception e) {
            log.error("❌ [Google Drive] Sync failed: {}", e.getMessage());
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * Uploads or updates student chat logs & master training guides into the target Google Drive folder.
     */
    private int uploadChatLogsToDrive() {
        int count = 0;
        List<String> fileNames = List.of(
                "student_chat_history.txt",
                "student_chat_audit_log.jsonl",
                "EduSync_AI_Campus_Master_Training_Guide.txt"
        );

        for (String name : fileNames) {
            Path localFile = Path.of("ktu-docs", name);
            if (Files.exists(localFile)) {
                try {
                    byte[] bytes = Files.readAllBytes(localFile);
                    uploadOrUpdateFile(name, "text/plain", bytes);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to upload [{}] to Google Drive: {}", name, e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * Downloads any new PDF, Excel, CSV, or TXT documents added to the Google Drive folder.
     */
    private List<String> downloadTrainingFilesFromDrive() {
        List<String> downloaded = new ArrayList<>();
        try {
            String q = "'" + folderId + "' in parents and trashed = false";
            FileList fileList = driveService.files().list()
                    .setQ(q)
                    .setSupportsAllDrives(true)
                    .setIncludeItemsFromAllDrives(true)
                    .setFields("files(id, name, mimeType, modifiedTime)")
                    .execute();

            if (fileList.getFiles() != null) {
                Path localDir = Path.of("ktu-docs");
                if (!Files.exists(localDir)) {
                    Files.createDirectories(localDir);
                }

                for (File file : fileList.getFiles()) {
                    String name = file.getName();
                    // Don't re-download uploaded logs
                    if (name.equals("student_chat_history.txt") || name.equals("student_chat_audit_log.jsonl")) {
                        continue;
                    }

                    Path target = localDir.resolve(name);
                    if (!Files.exists(target)) {
                        log.info("📥 [Google Drive] Downloading new training document: {} (ID: {})...", name, file.getId());
                        try (OutputStream out = new FileOutputStream(target.toFile())) {
                            driveService.files().get(file.getId())
                                    .setSupportsAllDrives(true)
                                    .executeMediaAndDownloadTo(out);
                            downloaded.add(name);
                            log.info("✅ [Google Drive] Downloaded [{}] to ./ktu-docs/", name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch files from Google Drive: {}", e.getMessage());
        }
        return downloaded;
    }

    /**
     * Creates or updates a file in the target Google Drive folder.
     */
    public void uploadOrUpdateFile(String fileName, String mimeType, byte[] data) throws Exception {
        // Check if file already exists in folder
        String q = "'" + folderId + "' in parents and name = '" + fileName.replace("'", "\\'") + "' and trashed = false";
        FileList result = driveService.files().list()
                .setQ(q)
                .setSupportsAllDrives(true)
                .setIncludeItemsFromAllDrives(true)
                .setFields("files(id, name)")
                .execute();

        ByteArrayContent mediaContent = new ByteArrayContent(mimeType, data);

        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            // Update existing file
            String existingId = result.getFiles().get(0).getId();
            File fileMetadata = new File();
            driveService.files().update(existingId, fileMetadata, mediaContent)
                    .setSupportsAllDrives(true)
                    .execute();
            log.debug("☁️ [Google Drive] Updated existing file [{}] (ID: {})", fileName, existingId);
        } else {
            // Create new file
            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(Collections.singletonList(folderId));
            File uploaded = driveService.files().create(fileMetadata, mediaContent)
                    .setSupportsAllDrives(true)
                    .setFields("id, name")
                    .execute();
            log.info("☁️ [Google Drive] Uploaded new file [{}] (ID: {})", fileName, uploaded.getId());
        }
    }

    public boolean isConfigured() {
        return isConfigured;
    }

    public String getFolderId() {
        return folderId;
    }
}

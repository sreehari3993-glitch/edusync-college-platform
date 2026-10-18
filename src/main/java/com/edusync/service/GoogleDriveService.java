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

    @Value("${google.drive.folder-id:${GOOGLE_DRIVE_FOLDER_ID:}}")
    private String folderId;

    @Value("${google.drive.shared-drive-id:${google.drive.shared-folder-id:${GOOGLE_DRIVE_SHARED_DRIVE_ID:${GOOGLE_DRIVE_SHARED_FOLDER_ID:}}}}")
    private String sharedDriveId;

    @Value("${edusync.storage.local-fallback-dir:${LOCAL_STORAGE_FALLBACK_DIR:uploads/fallback}}")
    private String localFallbackDir;

    @Value("${google.drive.credentials-path:credentials.json}")
    private String credentialsPath;

    @Value("${google.drive.auto-sync.enabled:true}")
    private boolean autoSyncEnabled;

    private Drive driveService;
    private boolean isConfigured = false;

    public String getEffectiveRootFolderId() {
        if (sharedDriveId != null && !sharedDriveId.trim().isEmpty()) {
            return sharedDriveId.trim();
        }
        return (folderId != null && !folderId.trim().isEmpty()) ? folderId.trim() : null;
    }

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
                log.info("☁️ [Google Drive] Successfully connected to Google Drive API (Effective Root Parent: {}, Shared Drive Configured: {})",
                        getEffectiveRootFolderId(), (sharedDriveId != null && !sharedDriveId.trim().isEmpty()));
                
                // Trigger initial sync in background
                syncAll();
            } else {
                log.info("ℹ️ [Google Drive] credentials.json not found in root directory. To enable auto-upload to Google Drive folder [{}], place your Google Service Account JSON file as 'credentials.json'.", getEffectiveRootFolderId());
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
            String effectiveRootId = getEffectiveRootFolderId();
            String q = "'" + effectiveRootId + "' in parents and trashed = false";
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
        uploadFileToFolder(getEffectiveRootFolderId(), fileName, mimeType, data);
    }

    /**
     * Uploads a file into a specific named subfolder (e.g., "Assignments", "Study-Materials", "Duty-Leave-Certificates").
     * If Google Drive upload fails (e.g. quota exceeded) or is unconfigured, automatically falls back to local storage.
     */
    public com.edusync.dto.DriveFileDto uploadFile(String subfolderName, String fileName, String mimeType, byte[] data) throws Exception {
        try {
            if (!isConfigured || driveService == null) {
                init();
            }
            if (!isConfigured || driveService == null) {
                throw new IllegalStateException("Google Drive service is not configured or credentials not found.");
            }

            String effectiveRootId = getEffectiveRootFolderId();
            String targetParentId = effectiveRootId;
            if (subfolderName != null && !subfolderName.trim().isEmpty()) {
                targetParentId = getOrCreateSubfolder(subfolderName.trim());
            }

            File uploaded = uploadFileToFolder(targetParentId, fileName, mimeType, data);
            String fileId = uploaded.getId();
            String webView = uploaded.getWebViewLink() != null ? uploaded.getWebViewLink() : "https://drive.google.com/file/d/" + fileId + "/view";
            String download = "https://drive.google.com/uc?id=" + fileId + "&export=download";

            return com.edusync.dto.DriveFileDto.builder()
                    .fileId(fileId)
                    .fileName(fileName)
                    .originalFileName(fileName)
                    .mimeType(mimeType)
                    .fileSize(data.length)
                    .subfolder(subfolderName)
                    .webViewLink(webView)
                    .downloadUrl(download)
                    .uploadedAt(java.time.LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.warn("⚠️ [Google Drive] Upload failed for file '{}' in subfolder '{}': {}. Falling back to local storage directory '{}'",
                    fileName, subfolderName, e.getMessage(), localFallbackDir);
            return saveToLocalFallback(subfolderName, fileName, mimeType, data);
        }
    }

    /**
     * Fallback to local storage on disk when Google Drive upload fails or is unavailable.
     */
    /**
     * Fallback to local storage on disk when Google Drive upload fails or is unavailable.
     */
    private com.edusync.dto.DriveFileDto saveToLocalFallback(String subfolderName, String fileName, String mimeType, byte[] data) {
        try {
            // Cap the incoming filename length before any filesystem operations
            String cappedInputName = (fileName != null && !fileName.trim().isEmpty()) ? fileName.trim() : "file";
            if (cappedInputName.length() > 500) {
                cappedInputName = cappedInputName.substring(0, 500);
            }

            // Extract base name safely without directory components
            String rawBase;
            try {
                Path p = Path.of(cappedInputName).getFileName();
                rawBase = (p != null) ? p.toString() : "file";
            } catch (Exception ex) {
                int lastSep = Math.max(cappedInputName.lastIndexOf('/'), cappedInputName.lastIndexOf('\\'));
                rawBase = (lastSep >= 0 && lastSep < cappedInputName.length() - 1) ? cappedInputName.substring(lastSep + 1) : "file";
            }
            if (rawBase.isBlank()) {
                rawBase = "file";
            }

            // Replace every character outside [A-Za-z0-9._-] by "_"
            String sanitized = rawBase.replaceAll("[^A-Za-z0-9._-]", "_");
            while (sanitized.contains("..")) {
                sanitized = sanitized.replace("..", "_");
            }
            if (sanitized.isBlank()) {
                sanitized = "file";
            }

            // Truncate to 100 characters with original extension preserved
            String finalBase;
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0 && lastDot < sanitized.length() - 1) {
                String ext = sanitized.substring(lastDot);
                String namePart = sanitized.substring(0, lastDot);
                if (ext.length() >= 100) {
                    finalBase = ext.substring(0, 100);
                } else if (namePart.length() + ext.length() > 100) {
                    finalBase = namePart.substring(0, 100 - ext.length()) + ext;
                } else {
                    finalBase = namePart + ext;
                }
            } else {
                finalBase = sanitized.length() > 100 ? sanitized.substring(0, 100) : sanitized;
            }

            String uuid = UUID.randomUUID().toString().replace("-", "");
            String storedName = uuid + "_" + finalBase;
            String localId = "LOCAL_" + storedName;

            String safeSubfolder = (subfolderName != null && !subfolderName.trim().isEmpty())
                    ? subfolderName.trim().replaceAll("[^a-zA-Z0-9_-]", "_")
                    : "general";

            Path fallbackRoot = Path.of(localFallbackDir).toAbsolutePath().normalize();
            Path dir = fallbackRoot.resolve(safeSubfolder).normalize();
            if (!dir.startsWith(fallbackRoot)) {
                throw new SecurityException("Path traversal attempt detected in subfolder: " + subfolderName);
            }
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Path targetPath = dir.resolve(storedName).normalize();
            if (!targetPath.startsWith(fallbackRoot)) {
                throw new SecurityException("Path traversal attempt detected in file name: " + fileName);
            }

            Files.write(targetPath, data);

            String downloadEndpointUrl = "/api/files/download/" + localId;

            log.warn("⚠️ [Local Storage Fallback] Google Drive upload failed or unavailable. Storing file '{}' as '{}' ({} bytes) to local disk at '{}'",
                    fileName, storedName, data.length, targetPath.toAbsolutePath());

            return com.edusync.dto.DriveFileDto.builder()
                    .fileId(localId)
                    .fileName(finalBase)
                    .originalFileName(fileName)
                    .mimeType(mimeType)
                    .fileSize(data.length)
                    .subfolder(subfolderName)
                    .webViewLink(downloadEndpointUrl)
                    .downloadUrl(downloadEndpointUrl)
                    .uploadedAt(java.time.LocalDateTime.now())
                    .build();
        } catch (Exception ex) {
            log.error("❌ [Local Storage Fallback] Failed to save file locally: {}", ex.getMessage(), ex);
            throw new RuntimeException("Both Google Drive upload and local storage fallback failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Finds or creates a subfolder inside the root Google Drive folder/Shared Drive.
     */
    public synchronized String getOrCreateSubfolder(String subfolderName) throws Exception {
        String effectiveRootId = getEffectiveRootFolderId();
        if (driveService == null) return effectiveRootId;

        String q = "'" + effectiveRootId + "' in parents and name = '" + subfolderName.replace("'", "\\'") + "' and mimeType = 'application/vnd.google-apps.folder' and trashed = false";
        FileList result = driveService.files().list()
                .setQ(q)
                .setSupportsAllDrives(true)
                .setIncludeItemsFromAllDrives(true)
                .setFields("files(id, name)")
                .execute();

        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }

        // Create new subfolder
        File folderMetadata = new File();
        folderMetadata.setName(subfolderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setParents(Collections.singletonList(effectiveRootId));

        File created = driveService.files().create(folderMetadata)
                .setSupportsAllDrives(true)
                .setFields("id, name")
                .execute();
        log.info("📁 [Google Drive] Created subfolder [{}] (ID: {}) in root folder [{}]", subfolderName, created.getId(), effectiveRootId);

        try {
            com.google.api.services.drive.model.Permission perm = new com.google.api.services.drive.model.Permission()
                    .setType("anyone")
                    .setRole("reader");
            driveService.permissions().create(created.getId(), perm)
                    .setSupportsAllDrives(true)
                    .execute();
        } catch (Exception ignored) {}

        return created.getId();
    }

    private File uploadFileToFolder(String parentFolderId, String fileName, String mimeType, byte[] data) throws Exception {
        if (driveService == null) throw new IllegalStateException("Google Drive API is not initialized. Please ensure credentials.json is present.");

        String q = "'" + parentFolderId + "' in parents and name = '" + fileName.replace("'", "\\'") + "' and trashed = false";
        FileList result = driveService.files().list()
                .setQ(q)
                .setSupportsAllDrives(true)
                .setIncludeItemsFromAllDrives(true)
                .setFields("files(id, name, webViewLink)")
                .execute();

        ByteArrayContent mediaContent = new ByteArrayContent(mimeType, data);

        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            String existingId = result.getFiles().get(0).getId();
            File fileMetadata = new File();
            File updated = driveService.files().update(existingId, fileMetadata, mediaContent)
                    .setSupportsAllDrives(true)
                    .setFields("id, name, webViewLink")
                    .execute();
            log.debug("☁️ [Google Drive] Updated existing file [{}] (ID: {})", fileName, existingId);
            return updated;
        } else {
            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(Collections.singletonList(parentFolderId));
            File uploaded = driveService.files().create(fileMetadata, mediaContent)
                    .setSupportsAllDrives(true)
                    .setFields("id, name, webViewLink")
                    .execute();
            log.info("☁️ [Google Drive] Uploaded new file [{}] (ID: {})", fileName, uploaded.getId());

            try {
                com.google.api.services.drive.model.Permission perm = new com.google.api.services.drive.model.Permission()
                        .setType("anyone")
                        .setRole("reader");
                driveService.permissions().create(uploaded.getId(), perm)
                        .setSupportsAllDrives(true)
                        .execute();
            } catch (Exception pe) {
                log.debug("Permission notice for {}: {}", fileName, pe.getMessage());
            }

            return uploaded;
        }
    }

    private void validateFileIdentifier(String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("File identifier cannot be empty");
        }
        if (fileId.contains("/") || fileId.contains("\\") || fileId.contains("..") || fileId.contains("\0")) {
            throw new IllegalArgumentException("Invalid file identifier: directory traversal or null byte detected");
        }
    }

    /**
     * Downloads file content as byte array by file ID, checking Google Drive and local storage fallback.
     */
    public byte[] downloadFile(String fileId) throws Exception {
        validateFileIdentifier(fileId);

        if (fileId != null && fileId.startsWith("LOCAL_")) {
            return downloadFromLocalFallback(fileId);
        }

        try {
            if (driveService != null) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                driveService.files().get(fileId)
                        .setSupportsAllDrives(true)
                        .executeMediaAndDownloadTo(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.warn("⚠️ [Google Drive] Download failed for fileId '{}': {}. Checking local storage fallback.", fileId, e.getMessage());
            byte[] localData = tryFindInLocalFallback(fileId);
            if (localData != null) {
                return localData;
            }
            throw e;
        }

        byte[] localData = tryFindInLocalFallback(fileId);
        if (localData != null) {
            return localData;
        }
        throw new IllegalStateException("Google Drive service not configured and file not found in local storage.");
    }

    private byte[] downloadFromLocalFallback(String fileId) throws Exception {
        byte[] data = tryFindInLocalFallback(fileId);
        if (data != null) return data;
        throw new java.io.FileNotFoundException("File not found in local storage fallback: " + fileId);
    }

    private byte[] tryFindInLocalFallback(String fileIdentifier) {
        try {
            validateFileIdentifier(fileIdentifier);
            Path root = Path.of(localFallbackDir).toAbsolutePath().normalize();
            if (!Files.exists(root)) return null;

            try (var stream = Files.walk(root)) {
                Optional<Path> match = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String pName = p.getFileName().toString();
                            // Exact matching on the identifier (LOCAL_ + stored name or exact stored name)
                            return fileIdentifier.equals("LOCAL_" + pName) || fileIdentifier.equals(pName);
                        })
                        .findFirst();
                if (match.isPresent()) {
                    return Files.readAllBytes(match.get());
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception ignored) {}
        return null;
    }

    public boolean isConfigured() {
        return isConfigured;
    }

    public String getFolderId() {
        return folderId;
    }

    public String getSharedDriveId() {
        return sharedDriveId;
    }

    public String getLocalFallbackDir() {
        return localFallbackDir;
    }
}

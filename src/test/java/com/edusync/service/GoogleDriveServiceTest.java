package com.edusync.service;

import com.edusync.dto.DriveFileDto;
import com.edusync.rag.RagService;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleDriveServiceTest {

    @Mock
    private RagService ragService;

    private GoogleDriveService driveService;
    private final String testTempDir = "target/test-storage-fallback";

    @BeforeEach
    void setUp() {
        driveService = new GoogleDriveService(ragService);
        ReflectionTestUtils.setField(driveService, "folderId", "default-root-folder-123");
        ReflectionTestUtils.setField(driveService, "sharedDriveId", "");
        ReflectionTestUtils.setField(driveService, "localFallbackDir", testTempDir);
        ReflectionTestUtils.setField(driveService, "isConfigured", false);
    }

    @AfterEach
    void tearDown() throws Exception {
        Path tempPath = Path.of(testTempDir);
        if (Files.exists(tempPath)) {
            try (var walk = Files.walk(tempPath)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    void testEffectiveRootFolderId_Default() {
        assertEquals("default-root-folder-123", driveService.getEffectiveRootFolderId());
    }

    @Test
    void testEffectiveRootFolderId_SharedDriveOverrides() {
        ReflectionTestUtils.setField(driveService, "sharedDriveId", "shared-drive-xyz-789");
        assertEquals("shared-drive-xyz-789", driveService.getEffectiveRootFolderId());
    }

    @Test
    void testUploadFile_FallsBackToLocalDiskWhenDriveFails() throws Exception {
        byte[] payload = "TKMIT Industry MoU Signed Copy".getBytes(StandardCharsets.UTF_8);
        String fileName = "MOU_Test_TechCorp_1.pdf";

        DriveFileDto result = driveService.uploadFile("Industry-MoUs", fileName, "application/pdf", payload);

        assertNotNull(result);
        assertNotNull(result.getFileId());
        assertTrue(result.getFileId().startsWith("LOCAL_"));
        assertEquals(fileName, result.getFileName());
        assertEquals("application/pdf", result.getMimeType());
        assertEquals(payload.length, result.getFileSize());
        assertEquals("Industry-MoUs", result.getSubfolder());
        assertEquals("/api/files/download/" + result.getFileId(), result.getWebViewLink());
        assertEquals("/api/files/download/" + result.getFileId(), result.getDownloadUrl());

        // Verify file physically exists on disk inside sandbox
        byte[] downloaded = driveService.downloadFile(result.getFileId());
        assertArrayEquals(payload, downloaded);
    }

    @Test
    void testDownloadFile_FromLocalFallback() throws Exception {
        byte[] payload = "Certificate of Completion - Student John Doe".getBytes(StandardCharsets.UTF_8);
        String fileName = "CERT_Test_John_1.pdf";

        DriveFileDto uploadResult = driveService.uploadFile("Internship-Certificates", fileName, "application/pdf", payload);
        assertNotNull(uploadResult);

        // Download using the generated fileId
        byte[] downloaded = driveService.downloadFile(uploadResult.getFileId());
        assertNotNull(downloaded);
        assertArrayEquals(payload, downloaded);
    }

    @Test
    @DisplayName("Path traversal sequences in fileName must stay strictly inside local fallback sandbox")
    void testPathTraversal_WriteStaysInsideSandbox() throws Exception {
        byte[] payload = "TRAVERSAL_PAYLOAD_TEST".getBytes(StandardCharsets.UTF_8);
        String maliciousFileName = "../../escaped_test_file.txt";

        DriveFileDto result = driveService.uploadFile("Industry-MoUs", maliciousFileName, "text/plain", payload);

        assertNotNull(result);
        assertTrue(result.getFileId().startsWith("LOCAL_"));

        Path sandbox = Path.of(testTempDir).toAbsolutePath().normalize();
        Path escapedLocation = Path.of("target/escaped_test_file.txt").toAbsolutePath().normalize();
        assertFalse(Files.exists(escapedLocation), "Malicious file must NOT be written outside the sandbox!");

        // File must be safely stored inside sandbox subfolder
        byte[] retrieved = driveService.downloadFile(result.getFileId());
        assertArrayEquals(payload, retrieved);
    }

    @Test
    @DisplayName("Absolute path in fileName must be stripped and stay inside sandbox")
    void testAbsolutePath_StaysInsideSandbox() throws Exception {
        byte[] payload = "ABSOLUTE_PATH_PAYLOAD".getBytes(StandardCharsets.UTF_8);
        String absoluteFileName = "C:\\evil_absolute_file.pdf";

        DriveFileDto result = driveService.uploadFile("Industry-MoUs", absoluteFileName, "application/pdf", payload);

        assertNotNull(result);
        assertTrue(result.getFileId().startsWith("LOCAL_"));
        assertFalse(Files.exists(Path.of("C:\\evil_absolute_file.pdf")));

        byte[] retrieved = driveService.downloadFile(result.getFileId());
        assertArrayEquals(payload, retrieved);
    }

    @Test
    @DisplayName("Long filename must be capped at 100 characters with original extension preserved")
    void testLongFileName_PreservesExtensionAndStaysInsideSandbox() throws Exception {
        byte[] payload = "LONG_FILENAME_PAYLOAD".getBytes(StandardCharsets.UTF_8);
        String longFileName = "VeryLongFileName_".repeat(15) + "_FinalReport.pdf"; // >200 chars

        DriveFileDto result = driveService.uploadFile("Study-Materials", longFileName, "application/pdf", payload);

        assertNotNull(result);
        assertTrue(result.getFileName().endsWith(".pdf"));
        assertTrue(result.getFileName().length() <= 100);

        byte[] retrieved = driveService.downloadFile(result.getFileId());
        assertArrayEquals(payload, retrieved);
    }

    @Test
    @DisplayName("Exact-match lookup: substring, partial or path-traversal identifiers must NOT match")
    void testExactMatchLookup_SubstringMustNotMatch() throws Exception {
        byte[] payload = "CONFIDENTIAL_PAYLOAD".getBytes(StandardCharsets.UTF_8);
        String fileName = "Confidential_Contract.pdf";

        DriveFileDto uploadResult = driveService.uploadFile("Industry-MoUs", fileName, "application/pdf", payload);
        assertNotNull(uploadResult);

        // 1. Substring matching must fail (must throw exception, never return data)
        assertThrows(FileNotFoundException.class, () -> driveService.downloadFile("LOCAL_Contract.pdf"));
        assertThrows(Exception.class, () -> driveService.downloadFile("Contract.pdf"));
        assertThrows(Exception.class, () -> driveService.downloadFile("Confidential_Contract.pdf"));

        // 2. Traversal or invalid characters must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> driveService.downloadFile("../../secret.txt"));
        assertThrows(IllegalArgumentException.class, () -> driveService.downloadFile("file\\path"));
        assertThrows(IllegalArgumentException.class, () -> driveService.downloadFile("null\0byte"));

        // 3. Exact identifier matching must succeed
        byte[] retrieved = driveService.downloadFile(uploadResult.getFileId());
        assertArrayEquals(payload, retrieved);
    }

    @Test
    @DisplayName("Drive 403 quota simulation must trigger local fallback and allow download")
    void testDrive403Simulation_FallsBackToLocalAndDownloads() throws Exception {
        byte[] payload = "DRIVE_403_FALLBACK_TEST".getBytes(StandardCharsets.UTF_8);
        String fileName = "MOU_Google_403_Quota.pdf";

        // Mock Google Drive client throwing 403 User Rate Limit Exceeded
        Drive mockDrive = mock(Drive.class, RETURNS_DEEP_STUBS);
        Drive.Files mockFiles = mock(Drive.Files.class, RETURNS_DEEP_STUBS);
        Drive.Files.List mockList = mock(Drive.Files.List.class, RETURNS_DEEP_STUBS);
        Drive.Files.Create mockCreate = mock(Drive.Files.Create.class, RETURNS_DEEP_STUBS);

        lenient().when(mockDrive.files()).thenReturn(mockFiles);
        lenient().when(mockFiles.list()).thenReturn(mockList);
        lenient().when(mockList.setQ(anyString())).thenReturn(mockList);
        lenient().when(mockList.setSupportsAllDrives(anyBoolean())).thenReturn(mockList);
        lenient().when(mockList.setIncludeItemsFromAllDrives(anyBoolean())).thenReturn(mockList);
        lenient().when(mockList.setFields(anyString())).thenReturn(mockList);

        FileList emptyList = new FileList();
        emptyList.setFiles(Collections.emptyList());
        lenient().when(mockList.execute()).thenReturn(emptyList);

        lenient().when(mockFiles.create(any(), any())).thenReturn(mockCreate);
        lenient().when(mockCreate.setSupportsAllDrives(anyBoolean())).thenReturn(mockCreate);
        lenient().when(mockCreate.setFields(anyString())).thenReturn(mockCreate);
        lenient().when(mockCreate.execute()).thenThrow(new IOException("403 Forbidden: User Rate Limit Exceeded"));

        ReflectionTestUtils.setField(driveService, "isConfigured", true);
        ReflectionTestUtils.setField(driveService, "driveService", mockDrive);

        // Upload triggers Drive 403, falls back to disk
        DriveFileDto result = driveService.uploadFile("Industry-MoUs", fileName, "application/pdf", payload);

        assertNotNull(result);
        assertTrue(result.getFileId().startsWith("LOCAL_"));
        assertEquals("/api/files/download/" + result.getFileId(), result.getWebViewLink());

        // Verify downloaded content matches payload
        byte[] downloaded = driveService.downloadFile(result.getFileId());
        assertArrayEquals(payload, downloaded);
    }
}

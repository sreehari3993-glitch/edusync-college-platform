package com.edusync.controller;

import com.edusync.dto.DriveFileDto;
import com.edusync.model.Assignment;
import com.edusync.model.StudyMaterial;
import com.edusync.model.User;
import com.edusync.repository.AssignmentRepository;
import com.edusync.repository.AssignmentSubmissionRepository;
import com.edusync.repository.StudyMaterialRepository;
import com.edusync.repository.UserRepository;
import com.edusync.service.GoogleDriveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.FileNotFoundException;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Mock
    private GoogleDriveService googleDriveService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyMaterialRepository studyMaterialRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @InjectMocks
    private FileUploadController fileUploadController;

    private MockMvc mockMvc;
    private User student;
    private User faculty;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileUploadController).build();

        student = User.builder()
                .id(1L)
                .name("Arun Kumar")
                .email("arun@student.tkmit.ac.in")
                .role(User.Role.STUDENT)
                .department("CSE")
                .rollNumber("TKM21CS001")
                .build();

        faculty = User.builder()
                .id(2L)
                .name("Prof. Vimal Sankar")
                .email("vimal@faculty.tkmit.ac.in")
                .role(User.Role.FACULTY)
                .department("CSE")
                .build();
    }

    @Test
    void testUploadGenericFileToGoogleDrive() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample_document.pdf",
                "application/pdf",
                "%PDF-1.4\nSample PDF document bytes".getBytes()
        );

        DriveFileDto mockResult = DriveFileDto.builder()
                .fileId("gdrive_file_123")
                .fileName("sample_document.pdf")
                .mimeType("application/pdf")
                .fileSize(file.getSize())
                .subfolder("Assignments")
                .webViewLink("https://drive.google.com/file/d/gdrive_file_123/view")
                .downloadUrl("https://drive.google.com/uc?id=gdrive_file_123&export=download")
                .build();

        when(googleDriveService.uploadFile(eq("Assignments"), anyString(), eq("application/pdf"), any())).thenReturn(mockResult);

        var response = fileUploadController.uploadFile(file, "ASSIGNMENT", student);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("gdrive_file_123", response.getBody().getData().getFileId());
    }

    @Test
    void testSubmitAssignmentToGoogleDrive() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "solution_code.pdf",
                "application/pdf",
                "%PDF-1.4\nAssignment solution PDF".getBytes()
        );

        Assignment assignment = Assignment.builder()
                .id(10L)
                .title("Programming Lab 4")
                .subject("Programming in C")
                .department("CSE")
                .build();

        DriveFileDto mockResult = DriveFileDto.builder()
                .fileId("sub_file_456")
                .fileName("solution_code.pdf")
                .webViewLink("https://drive.google.com/file/d/sub_file_456/view")
                .downloadUrl("https://drive.google.com/uc?id=sub_file_456&export=download")
                .build();

        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(googleDriveService.uploadFile(eq("Assignments"), anyString(), eq("application/pdf"), any())).thenReturn(mockResult);
        when(assignmentSubmissionRepository.findByAssignmentAndStudent(any(), any())).thenReturn(Optional.empty());
        when(assignmentSubmissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = fileUploadController.submitAssignment(10L, file, student);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("https://drive.google.com/file/d/sub_file_456/view", response.getBody().getData().getFileUrl());
    }

    @Test
    void testUploadStudyMaterial() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Module3_Notes.pdf",
                "application/pdf",
                "%PDF-1.4\nModule 3 differential equations lecture notes".getBytes()
        );

        DriveFileDto mockResult = DriveFileDto.builder()
                .fileId("mat_file_789")
                .fileName("Module3_Notes.pdf")
                .webViewLink("https://drive.google.com/file/d/mat_file_789/view")
                .downloadUrl("https://drive.google.com/uc?id=mat_file_789&export=download")
                .build();

        when(googleDriveService.uploadFile(eq("Study-Materials"), anyString(), eq("application/pdf"), any())).thenReturn(mockResult);
        when(studyMaterialRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = fileUploadController.uploadStudyMaterial(
                file,
                "Module 3 Differential Equations",
                "Mathematics",
                "Module 3",
                "S4",
                faculty
        );

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Module 3 Differential Equations", response.getBody().getData().getTitle());
    }

    @Test
    @DisplayName("Fake PDF with HTML content must be rejected with 400 Bad Request")
    void testUploadFakePdfWithHtmlContent_RejectedWith400() {
        MockMultipartFile fakePdf = new MockMultipartFile(
                "file",
                "fake_invoice.pdf",
                "application/pdf",
                "<html><body><h1>This is an HTML page pretending to be a PDF</h1></body></html>".getBytes()
        );

        var response = fileUploadController.uploadFile(fakePdf, "ASSIGNMENT", student);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("magic bytes"));
    }

    @Test
    @DisplayName("Disallowed file extension must be rejected with 400 Bad Request")
    void testUploadDisallowedExtension_RejectedWith400() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malicious_tool.exe",
                "application/octet-stream",
                "MZ\0\0dummy executable content".getBytes()
        );

        var response = fileUploadController.uploadFile(exeFile, "ASSIGNMENT", student);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("allowed extension"));
    }

    @Test
    @DisplayName("Duty leave certificate with non-strict extension must be rejected with 400")
    void testUploadCertificateWithNonStrictExtension_RejectedWith400() {
        MockMultipartFile docFile = new MockMultipartFile(
                "file",
                "certificate.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "PK\0\0dummy docx content".getBytes()
        );

        var response = fileUploadController.uploadFile(docFile, "DUTY_LEAVE", student);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("MoU and certificate"));
    }

    @Test
    @DisplayName("MockMvc: Download endpoint returns 200, Content-Type, Content-Disposition and file content")
    void testDownloadFile_MockMvc_Success() throws Exception {
        String fileId = "LOCAL_0123456789abcdef0123456789abcdef_Annual_Report.pdf";
        byte[] fileData = "%PDF-1.4\nTest report data".getBytes();

        when(googleDriveService.downloadFile(fileId)).thenReturn(fileData);

        mockMvc.perform(get("/api/files/download/{fileId}", fileId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("Annual_Report.pdf")))
                .andExpect(content().bytes(fileData));
    }

    @Test
    @DisplayName("MockMvc: Download endpoint rejects path traversal sequences with 400 Bad Request")
    void testDownloadFile_MockMvc_PathTraversal_RejectedWith400() throws Exception {
        mockMvc.perform(get("/api/files/download/{fileId}", "..escaped_file.txt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("MockMvc: Download endpoint returns 404 when file is not found")
    void testDownloadFile_MockMvc_NotFound_Returns404() throws Exception {
        String fileId = "LOCAL_nonexistent_file.pdf";
        when(googleDriveService.downloadFile(fileId)).thenThrow(new FileNotFoundException("File not found"));

        mockMvc.perform(get("/api/files/download/{fileId}", fileId))
                .andExpect(status().isNotFound());
    }
}

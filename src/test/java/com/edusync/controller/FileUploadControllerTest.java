package com.edusync.controller;

import com.edusync.dto.DriveFileDto;
import com.edusync.model.Assignment;
import com.edusync.model.User;
import com.edusync.repository.AssignmentRepository;
import com.edusync.repository.AssignmentSubmissionRepository;
import com.edusync.repository.StudyMaterialRepository;
import com.edusync.repository.UserRepository;
import com.edusync.service.GoogleDriveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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

    private User student;
    private User faculty;

    @BeforeEach
    void setUp() {
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
                "PDF content test bytes".getBytes()
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
                "Assignment solution pdf".getBytes()
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
                "Module 3 differential equations lecture notes".getBytes()
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
}

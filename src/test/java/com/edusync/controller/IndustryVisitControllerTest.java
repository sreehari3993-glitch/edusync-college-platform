package com.edusync.controller;

import com.edusync.dto.AppDto;
import com.edusync.dto.IndustryDto;
import com.edusync.model.IndustryVisit;
import com.edusync.model.User;
import com.edusync.repository.UserRepository;
import com.edusync.security.JwtUtil;
import com.edusync.service.IndustryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndustryVisitControllerTest {

    @Mock
    private IndustryService industryService;

    @Mock
    private UserRepository userRepo;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Authentication auth;

    @InjectMocks
    private IndustryController industryController;

    private User facultyUser;
    private User hodUser;
    private User principalUser;

    @BeforeEach
    void setUp() {
        facultyUser = User.builder()
                .id(10L)
                .name("Dr. Maya")
                .email("maya@tkmit.ac.in")
                .role(User.Role.FACULTY)
                .department("CSE")
                .build();

        hodUser = User.builder()
                .id(20L)
                .name("Dr. Rajesh HOD")
                .email("rajesh@tkmit.ac.in")
                .role(User.Role.HOD)
                .department("CSE")
                .build();

        principalUser = User.builder()
                .id(30L)
                .name("Dr. Principal")
                .email("principal@tkmit.ac.in")
                .role(User.Role.PRINCIPAL)
                .build();
    }

    @Test
    @DisplayName("POST /visits/propose returns 200 with visit in ICH_REVIEW stage")
    void testProposeVisit() {
        when(auth.getName()).thenReturn("maya@tkmit.ac.in");
        when(userRepo.findByEmailIgnoreCase("maya@tkmit.ac.in")).thenReturn(Optional.of(facultyUser));

        IndustryDto.IndustryVisitRequest req = IndustryDto.IndustryVisitRequest.builder()
                .companyId(1L)
                .department("CSE")
                .date(LocalDate.now().plusDays(10))
                .studentCount(45)
                .purpose("Industrial Exposure")
                .overnight(false)
                .outsideKerala(false)
                .build();

        IndustryDto.IndustryVisitDto expectedDto = IndustryDto.IndustryVisitDto.builder()
                .id(501L)
                .companyId(1L)
                .department("CSE")
                .stage(IndustryVisit.VisitStage.ICH_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .build();

        when(industryService.proposeVisit(eq(req), eq(10L), eq("maya@tkmit.ac.in"))).thenReturn(expectedDto);

        ResponseEntity<AppDto.ApiResponse<IndustryDto.IndustryVisitDto>> response = industryController.proposeVisit(req, auth);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(IndustryVisit.VisitStage.ICH_REVIEW, response.getBody().getData().getStage());
        verify(industryService, times(1)).proposeVisit(eq(req), eq(10L), eq("maya@tkmit.ac.in"));
    }

    @Test
    @DisplayName("PUT /visits/{id}/ich-action on approve transitions to HOD_REVIEW")
    void testIchVisitAction() {
        when(auth.getName()).thenReturn("ich@tkmit.ac.in");

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.APPROVED)
                .remark("ICH approved")
                .build();

        IndustryDto.IndustryVisitDto expectedDto = IndustryDto.IndustryVisitDto.builder()
                .id(501L)
                .stage(IndustryVisit.VisitStage.HOD_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .ichStatus(IndustryVisit.ApprovalStatus.APPROVED)
                .build();

        when(industryService.ichVisitAction(eq(501L), eq(req), eq("ich@tkmit.ac.in"))).thenReturn(expectedDto);

        ResponseEntity<AppDto.ApiResponse<IndustryDto.IndustryVisitDto>> response = industryController.ichVisitAction(501L, req, auth);

        assertNotNull(response.getBody());
        assertEquals(IndustryVisit.VisitStage.HOD_REVIEW, response.getBody().getData().getStage());
    }

    @Test
    @DisplayName("PUT /visits/{id}/hod-action (non-Principal path): directly APPROVED")
    void testHodVisitAction_NonPrincipalPath() {
        when(auth.getName()).thenReturn("rajesh@tkmit.ac.in");
        when(userRepo.findByEmailIgnoreCase("rajesh@tkmit.ac.in")).thenReturn(Optional.of(hodUser));

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.APPROVED)
                .remark("HOD approved")
                .build();

        IndustryDto.IndustryVisitDto expectedDto = IndustryDto.IndustryVisitDto.builder()
                .id(501L)
                .stage(IndustryVisit.VisitStage.COMPLETED)
                .status(IndustryVisit.VisitStatus.APPROVED)
                .hodStatus(IndustryVisit.ApprovalStatus.APPROVED)
                .principalStatus(IndustryVisit.ApprovalStatus.NOT_REQUIRED)
                .build();

        when(industryService.hodVisitAction(eq(501L), eq(20L), eq(req), eq("rajesh@tkmit.ac.in"))).thenReturn(expectedDto);

        ResponseEntity<AppDto.ApiResponse<IndustryDto.IndustryVisitDto>> response = industryController.hodVisitAction(501L, req, auth);

        assertNotNull(response.getBody());
        assertEquals(IndustryVisit.VisitStage.COMPLETED, response.getBody().getData().getStage());
        assertEquals(IndustryVisit.VisitStatus.APPROVED, response.getBody().getData().getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.NOT_REQUIRED, response.getBody().getData().getPrincipalStatus());
    }

    @Test
    @DisplayName("PUT /visits/{id}/hod-action (Principal path): escalates to PRINCIPAL_REVIEW")
    void testHodVisitAction_PrincipalPath() {
        when(auth.getName()).thenReturn("rajesh@tkmit.ac.in");
        when(userRepo.findByEmailIgnoreCase("rajesh@tkmit.ac.in")).thenReturn(Optional.of(hodUser));

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.APPROVED)
                .remark("Overnight visit endorsed")
                .build();

        IndustryDto.IndustryVisitDto expectedDto = IndustryDto.IndustryVisitDto.builder()
                .id(502L)
                .stage(IndustryVisit.VisitStage.PRINCIPAL_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .hodStatus(IndustryVisit.ApprovalStatus.APPROVED)
                .principalStatus(IndustryVisit.ApprovalStatus.PENDING)
                .build();

        when(industryService.hodVisitAction(eq(502L), eq(20L), eq(req), eq("rajesh@tkmit.ac.in"))).thenReturn(expectedDto);

        ResponseEntity<AppDto.ApiResponse<IndustryDto.IndustryVisitDto>> response = industryController.hodVisitAction(502L, req, auth);

        assertNotNull(response.getBody());
        assertEquals(IndustryVisit.VisitStage.PRINCIPAL_REVIEW, response.getBody().getData().getStage());
        assertEquals(IndustryVisit.ApprovalStatus.PENDING, response.getBody().getData().getPrincipalStatus());
    }

    @Test
    @DisplayName("PUT /visits/{id}/principal-action: APPROVED and COMPLETED")
    void testPrincipalVisitAction_Approve() {
        when(auth.getName()).thenReturn("principal@tkmit.ac.in");

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.APPROVED)
                .remark("Sanctioned by Principal")
                .build();

        IndustryDto.IndustryVisitDto expectedDto = IndustryDto.IndustryVisitDto.builder()
                .id(502L)
                .stage(IndustryVisit.VisitStage.COMPLETED)
                .status(IndustryVisit.VisitStatus.APPROVED)
                .principalStatus(IndustryVisit.ApprovalStatus.APPROVED)
                .build();

        when(industryService.principalVisitAction(eq(502L), eq(req), eq("principal@tkmit.ac.in"))).thenReturn(expectedDto);

        ResponseEntity<AppDto.ApiResponse<IndustryDto.IndustryVisitDto>> response = industryController.principalVisitAction(502L, req, auth);

        assertNotNull(response.getBody());
        assertEquals(IndustryVisit.VisitStage.COMPLETED, response.getBody().getData().getStage());
        assertEquals(IndustryVisit.VisitStatus.APPROVED, response.getBody().getData().getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.APPROVED, response.getBody().getData().getPrincipalStatus());
    }

    @Test
    @DisplayName("PUT /visits/{id}/principal-action: REJECTED and COMPLETED")
    void testPrincipalVisitAction_Reject() {
        when(auth.getName()).thenReturn("principal@tkmit.ac.in");

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.REJECTED)
                .remark("Rejected due to semester exam schedule")
                .build();

        IndustryDto.IndustryVisitDto expectedDto = IndustryDto.IndustryVisitDto.builder()
                .id(502L)
                .stage(IndustryVisit.VisitStage.COMPLETED)
                .status(IndustryVisit.VisitStatus.REJECTED)
                .principalStatus(IndustryVisit.ApprovalStatus.REJECTED)
                .build();

        when(industryService.principalVisitAction(eq(502L), eq(req), eq("principal@tkmit.ac.in"))).thenReturn(expectedDto);

        ResponseEntity<AppDto.ApiResponse<IndustryDto.IndustryVisitDto>> response = industryController.principalVisitAction(502L, req, auth);

        assertNotNull(response.getBody());
        assertEquals(IndustryVisit.VisitStage.COMPLETED, response.getBody().getData().getStage());
        assertEquals(IndustryVisit.VisitStatus.REJECTED, response.getBody().getData().getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.REJECTED, response.getBody().getData().getPrincipalStatus());
    }

    @Test
    @DisplayName("GET /visits passes user and filters to service")
    void testGetVisits() {
        when(auth.getName()).thenReturn("maya@tkmit.ac.in");
        when(userRepo.findByEmailIgnoreCase("maya@tkmit.ac.in")).thenReturn(Optional.of(facultyUser));

        IndustryDto.IndustryVisitDto visitDto = IndustryDto.IndustryVisitDto.builder().id(501L).build();
        when(industryService.getVisits(eq(facultyUser), isNull(), isNull(), isNull())).thenReturn(List.of(visitDto));

        ResponseEntity<AppDto.ApiResponse<List<IndustryDto.IndustryVisitDto>>> response =
                industryController.getVisits(null, null, null, auth);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().size());
        verify(industryService, times(1)).getVisits(eq(facultyUser), isNull(), isNull(), isNull());
    }
}

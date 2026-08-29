package com.edusync.service.ai;

import com.edusync.dto.AiDto;
import com.edusync.model.User;
import com.edusync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtsScreeningServiceTest {

    @Mock
    private UserRepository userRepo;

    private AtsScreeningService atsScreeningService;

    @BeforeEach
    void setUp() {
        atsScreeningService = new AtsScreeningService(userRepo);
    }

    @Test
    void testExtractSkillsFromText() {
        String resumeText = "Experienced in Java, Spring Boot, MySQL, REST APIs, Docker, and React.";
        List<String> skills = atsScreeningService.extractSkillsFromText(resumeText);

        assertTrue(skills.contains("Java"));
        assertTrue(skills.contains("Spring Boot"));
        assertTrue(skills.contains("SQL & MySQL"));
        assertTrue(skills.contains("REST APIs"));
        assertTrue(skills.contains("Docker & Containers"));
        assertTrue(skills.contains("React"));
    }

    @Test
    void testScreenCandidateStrongFit() {
        AiDto.CandidateInput candidate = AiDto.CandidateInput.builder()
                .studentId(1L)
                .name("Anzal Rahman")
                .cgpa(8.8)
                .department("CSE")
                .resumeText("Built full-stack applications with Java, Spring Boot, REST APIs, MySQL, Git, and Docker. Completed internship with live project.")
                .build();

        AiDto.ResumeScreenRequest job = AiDto.ResumeScreenRequest.builder()
                .companyName("TCS Digital")
                .jobRole("Software Developer")
                .minCgpa(7.5)
                .requiredSkills(List.of("Java", "Spring Boot", "SQL & MySQL", "REST APIs", "Git & GitHub"))
                .build();

        AiDto.ResumeScreenResult result = atsScreeningService.screenCandidate(candidate, job);

        assertNotNull(result);
        assertTrue(result.getAtsScore() >= 80, "Expected high ATS score for matching candidate");
        assertEquals("STRONG_FIT", result.getRecommendation());
        assertEquals("green", result.getRecommendationBadge());
        assertTrue(result.getMatchedSkills().contains("Java"));
    }

    @Test
    void testScreenCandidateBelowThreshold() {
        AiDto.CandidateInput candidate = AiDto.CandidateInput.builder()
                .studentId(2L)
                .name("Test Student")
                .cgpa(5.5)
                .department("ME")
                .resumeText("Basic familiarity with HTML.")
                .build();

        AiDto.ResumeScreenRequest job = AiDto.ResumeScreenRequest.builder()
                .companyName("Infosys")
                .jobRole("Full Stack Developer")
                .minCgpa(7.0)
                .requiredSkills(List.of("Java", "Spring Boot", "SQL & MySQL", "Docker & Containers"))
                .build();

        AiDto.ResumeScreenResult result = atsScreeningService.screenCandidate(candidate, job);

        assertNotNull(result);
        assertTrue(result.getAtsScore() < 60);
        assertEquals("BELOW_THRESHOLD", result.getRecommendation());
        assertEquals("red", result.getRecommendationBadge());
    }

    @Test
    void testAnalyzeStudentResume() {
        AiDto.ResumeAnalyzeRequest request = AiDto.ResumeAnalyzeRequest.builder()
                .resumeText("Proficient in Python, Machine Learning, TensorFlow, SQL, and Git.")
                .targetRole("AI/ML Engineer")
                .build();

        AiDto.ResumeAnalyzeResponse response = atsScreeningService.analyzeStudentResume(request, null);

        assertNotNull(response);
        assertTrue(response.getAtsScore() >= 55);
        assertNotNull(response.getVerdict());
        assertTrue(response.getIdentifiedSkills().contains("Python"));
        assertFalse(response.getRecommendedSkills().isEmpty());
    }
}

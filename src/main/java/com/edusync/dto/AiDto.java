package com.edusync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AiDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatRequest {
        private String message;
        private String role;
        private String conversationId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatResponse {
        private String reply;
        private String intent;
        private List<String> suggestedActions;
        private boolean fromLocalLlm;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResumeScreenRequest {
        private Long driveId;
        private String jobRole;
        private String companyName;
        private String jobDescription;
        private List<String> requiredSkills;
        private Double minCgpa;
        private String eligibleBranches;
        private List<CandidateInput> candidates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CandidateInput {
        private Long studentId;
        private String name;
        private String email;
        private String department;
        private Double cgpa;
        private String resumeText;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResumeScreenResult {
        private Long studentId;
        private String studentName;
        private String department;
        private Double cgpa;
        private int atsScore;
        private int skillMatchPct;
        private List<String> matchedSkills;
        private List<String> missingSkills;
        private String recommendation; // STRONG_FIT, GOOD_MATCH, MODERATE_FIT, BELOW_THRESHOLD
        private String recommendationBadge; // green, blue, yellow, red
        private List<String> strengths;
        private List<String> areasOfImprovement;
        private String summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchScreenResponse {
        private String companyName;
        private String jobRole;
        private int totalCandidates;
        private int shortlistedCount;
        private List<ResumeScreenResult> rankedResults;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResumeAnalyzeRequest {
        private String resumeText;
        private String targetRole;
        private String targetCompany;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResumeAnalyzeResponse {
        private int atsScore;
        private String verdict;
        private List<String> identifiedSkills;
        private List<String> recommendedSkills;
        private List<String> keyStrengths;
        private List<String> improvementTips;
        private String roleReadiness;
    }
}

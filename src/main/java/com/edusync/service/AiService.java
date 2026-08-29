package com.edusync.service;

import com.edusync.dto.AiDto;
import com.edusync.rag.RagService;
import com.edusync.service.ai.AtsScreeningService;
import com.edusync.service.ai.ChatAuditLogger;
import com.edusync.service.ai.KtuKnowledgeBaseService;
import com.edusync.service.ai.OllamaClientService;
import com.edusync.service.ai.StudentContextResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI Service Facade orchestrating campus assistant interactions,
 * RAG document retrieval, local LLM queries, student context resolution,
 * and ATS resume screening.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RagService ragService;
    private final StudentContextResolver studentContextResolver;
    private final OllamaClientService ollamaClientService;
    private final KtuKnowledgeBaseService ktuKnowledgeBaseService;
    private final AtsScreeningService atsScreeningService;
    private final ChatAuditLogger chatAuditLogger;

    /**
     * Process student or campus query and generate an instant, helpful response.
     */
    public AiDto.ChatResponse chat(AiDto.ChatRequest request, Long authenticatedUserId) {
        String msg = request.getMessage() != null ? request.getMessage().trim() : "";
        if (msg.isEmpty()) {
            AiDto.ChatResponse resp = AiDto.ChatResponse.builder()
                    .reply("### 🤖 EduSync Campus AI Copilot\n\n" +
                           "> 🔒 **Quality & Training Notice:** *Please note that conversations with this AI assistant are recorded and analyzed to continually train and improve the AI agent's accuracy and performance.*\n\n" +
                           "Hello! How can I assist you today? Ask me about **KTU attendance rules, duty leave applications, your personal CGPA/marks, or campus placement drives!**")
                    .intent("GREETING")
                    .suggestedActions(List.of("What is KTU attendance rule?", "How do I apply for Duty Leave?", "Check my current attendance", "Upcoming placement drives"))
                    .fromLocalLlm(false)
                    .build();
            chatAuditLogger.recordChatAuditLog(authenticatedUserId, msg, resp);
            return resp;
        }

        String lower = msg.toLowerCase();
        AiDto.ChatResponse response;

        // 1. Instant friendly greetings & introductions
        if (lower.matches("^(hi|hello|hey|hola|greetings|good morning|good afternoon|good evening|namaste|hai|how are you|who are you|what can you do|what are you)(\\s+.*|!|\\.|\\?)?$")) {
            response = AiDto.ChatResponse.builder()
                    .reply("👋 **Hello! I am EduSync AI Copilot**, your college assistant for TKMIT & KTU.\n\n" +
                           "> 🔒 **Notice:** *Conversations are recorded for the continuous training, evaluation, and quality improvement of this AI agent.*\n\n" +
                           "Here is what I can do for you:\n" +
                           "- 📊 **Live Records:** Check your real-time attendance %, duty leave status, and internal marks\n" +
                           "- 📘 **KTU Regulations:** Minimum attendance (75%), Condonation (60%), Activity points (120 pts), B.Tech 2024 scheme rules\n" +
                           "- 💼 **Placements & Resumes:** Campus recruitment drives, ATS resume screening, and eligibility\n\n" +
                           "How can I help you today?")
                    .intent("GREETING")
                    .suggestedActions(List.of("What is KTU attendance rule?", "Check my attendance percentage", "How do I apply for Duty Leave?", "KTU Activity Points criteria"))
                    .fromLocalLlm(false)
                    .build();
            chatAuditLogger.recordChatAuditLog(authenticatedUserId, msg, response);
            return response;
        }

        // 2. Personal Context Queries (Live Database Data) - Check first because LLM doesn't have live DB access
        boolean isPersonalQuery = studentContextResolver.isPersonalStudentQuery(lower);
        if (isPersonalQuery) {
            AiDto.ChatResponse personalResp = studentContextResolver.resolvePersonalQuery(msg, authenticatedUserId);
            if (personalResp != null) {
                chatAuditLogger.recordChatAuditLog(authenticatedUserId, msg, personalResp);
                return personalResp;
            }
        }

        // 3. RAG Semantic Search over Ingested KTU Documents
        try {
            String ragAnswer = ragService.answerWithRag(msg);
            if (ragAnswer != null && !ragAnswer.isBlank()) {
                response = AiDto.ChatResponse.builder()
                        .reply(ragAnswer.trim())
                        .intent("KTU_RAG_DOCUMENT")
                        .suggestedActions(List.of("Check my attendance", "KTU Activity Points criteria", "How do I apply for OD letter?"))
                        .fromLocalLlm(true)
                        .build();
                chatAuditLogger.recordChatAuditLog(authenticatedUserId, msg, response);
                return response;
            }
        } catch (Exception e) {
            log.debug("RAG search failed, falling back to direct LLM: {}", e.getMessage());
        }

        // 4. KTU Regulations & Knowledge Base -> Query local fine-tuned LLM (Ollama)
        String localLlmAnswer = ollamaClientService.tryQueryLocalLlm(msg);
        if (localLlmAnswer != null && !localLlmAnswer.isBlank()) {
            response = AiDto.ChatResponse.builder()
                    .reply(localLlmAnswer.trim())
                    .intent("LOCAL_LLM")
                    .suggestedActions(List.of("Check my attendance", "How do I apply for OD letter?", "KTU Activity Points"))
                    .fromLocalLlm(true)
                    .build();
            chatAuditLogger.recordChatAuditLog(authenticatedUserId, msg, response);
            return response;
        }

        // 5. Built-in Deterministic Knowledge Engine Fallback (if Ollama is offline or warming up)
        response = ktuKnowledgeBaseService.resolveKnowledgeQuery(msg);
        chatAuditLogger.recordChatAuditLog(authenticatedUserId, msg, response);
        return response;
    }

    /**
     * Extracts recognized technical skills from freeform resume text.
     */
    public List<String> extractSkillsFromText(String text) {
        return atsScreeningService.extractSkillsFromText(text);
    }

    /**
     * Evaluates a candidate resume against a target job description and required skills.
     */
    public AiDto.ResumeScreenResult screenCandidate(AiDto.CandidateInput candidate, AiDto.ResumeScreenRequest job) {
        return atsScreeningService.screenCandidate(candidate, job);
    }

    /**
     * Batch screens multiple candidate applications for a campus drive.
     */
    public AiDto.BatchScreenResponse screenCandidatesForDrive(AiDto.ResumeScreenRequest request) {
        return atsScreeningService.screenCandidatesForDrive(request);
    }

    /**
     * Analyzes an individual student's resume and provides ATS score + actionable advice.
     */
    public AiDto.ResumeAnalyzeResponse analyzeStudentResume(AiDto.ResumeAnalyzeRequest request, Long userId) {
        return atsScreeningService.analyzeStudentResume(request, userId);
    }
}

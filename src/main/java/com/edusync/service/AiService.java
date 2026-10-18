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

        // 1. Instant natural conversational responses (greetings, small talk, gratitude, identity, jokes)
        AiDto.ChatResponse convResponse = resolveConversationalQuery(lower, msg);
        if (convResponse != null) {
            chatAuditLogger.recordChatAuditLog(authenticatedUserId, msg, convResponse);
            return convResponse;
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

    /**
     * Handles natural conversational questions, greetings, small talk, gratitude, identity, and jokes.
     */
    private AiDto.ChatResponse resolveConversationalQuery(String lower, String msg) {
        // 1. "How are you" / "How's it going"
        if (lower.matches(".*\\b(how are you|how r u|how are you doing|hows it going|how's it going|how do you do|how have you been|how are things)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("😊 **I'm doing great, thank you for asking!** I'm running smoothly and ready to assist you today.\n\n" +
                           "Whether you need to check your attendance %, view KTU 2024 academic regulations, calculate CGPA/internal marks, or prepare for campus placement drives—I'm here to help!\n\n" +
                           "How are you doing today? What can I help you with?")
                    .intent("CONVERSATIONAL")
                    .suggestedActions(List.of("Check my attendance", "What is KTU attendance rule?", "Calculate my CGPA", "Upcoming placement drives"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 2. "Nothing" / "Not much" / "Nothing special" / "Nothing for now"
        if (lower.matches("^(\\s*nothing(\\s+(much|special|really|at\\s+all|for\\s+now|right\\s+now|rn|else|bro|thanks|thx))?|not\\s+much|just\\s+chilling|nada|none|no\\s+nothing|nothing\\s+else|i\\s+don'?t\\s+need\\s+anything|have\\s+no\\s+questions|need\\s+nothing)(\\s*|!|\\.)*$")) {
            return AiDto.ChatResponse.builder()
                    .reply("No problem at all! 😊 Take your time. Whenever you need to check your attendance, look up KTU rules, or prepare for campus drives, I'll be right here!\n\nHave a great day ahead!")
                    .intent("CONVERSATIONAL")
                    .suggestedActions(List.of("Check my attendance", "What is KTU attendance rule?", "Upcoming placement drives"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 3. "No" / "Nope" / "Nah" / "No thanks" / "No thank you" / "No need"
        if (lower.matches("^(\\s*no|nope|nah|no\\s+thanks|no\\s+thank\\s+you|no\\s+need|not\\s+needed|no\\s+bro|nah\\s+im\\s+good|nah\\s+i'?m\\s+good)(\\s*|!|\\.)*$")) {
            return AiDto.ChatResponse.builder()
                    .reply("Understood! 👍 Feel free to reach out whenever you need any help with your college academics, attendance, or campus activities. Enjoy your day!")
                    .intent("CONVERSATIONAL")
                    .suggestedActions(List.of("Check my attendance", "KTU Activity Points criteria"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 4. "I'm good" / "All good" / "I am good" / "I'm fine" / "Doing good" / "Doing well"
        if (lower.matches("^(\\s*(i'?m\\s+good|all\\s+good|i\\s+am\\s+good|i'?m\\s+fine|doing\\s+good|doing\\s+well|everything\\s+is\\s+good|all\\s+set|i'?m\\s+okay|im\\s+ok)(\\s+(thanks|thx|bro|man|now))?)(\\s*|!|\\.)*$")
                || lower.matches(".*\\b(i'?m good|all good|i am good|i'?m fine|doing good|doing well|everything is good|all set)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("Glad to hear that! 😄 Keep up the great energy. Whenever you're ready to check your attendance %, internal marks, or placement drives, I'm here to help!")
                    .intent("CONVERSATIONAL")
                    .suggestedActions(List.of("Check my attendance", "What is KTU attendance rule?", "Upcoming placement drives"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 5. "Never mind" / "Nevermind" / "Nvm" / "Leave it" / "Forget it"
        if (lower.matches("^(\\s*(never\\s*mind|nvm|leave\\s+it|forget\\s+it|drop\\s+it|don'?t\\s+worry)(\\s+(about\\s+it|bro|thanks|thx))?)(\\s*|!|\\.)*$")) {
            return AiDto.ChatResponse.builder()
                    .reply("No worries! 😊 Whenever something comes up—whether it's attendance, internal marks, or KTU regulations—just ask anytime!")
                    .intent("CONVERSATIONAL")
                    .suggestedActions(List.of("Check my attendance", "What is KTU attendance rule?"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 6. "Ok" / "Okay" / "Cool" / "Sure" / "Got it" / "Alright" / "Understood" / "Noted"
        if (lower.matches("^(\\s*(ok|okay|k|cool|sure|got\\s+it|alright|understood|noted|perfect|fine|roger\\s+that|done)(\\s+(bro|thanks|thx|thank\\s+you|man|cool|got\\s+it))?)(\\s*|!|\\.)*$")) {
            return AiDto.ChatResponse.builder()
                    .reply("Awesome! 👍 Let me know whenever you need anything else—like attendance tracking, syllabus details, or exam rules.")
                    .intent("CONVERSATIONAL")
                    .suggestedActions(List.of("Check my attendance", "What is KTU attendance rule?", "How do I apply for OD letter?"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 7. "Great" / "Awesome" / "Nice" / "Good" / "Super" / "Perfect" / "Wonderful"
        if (lower.matches("^(\\s*(great|awesome|nice|good|super|perfect|wonderful)(\\s+(thanks|thx|bro|man|work|job))?)(\\s*|!|\\.)*$")) {
            return AiDto.ChatResponse.builder()
                    .reply("Fantastic! 🚀 Always glad to help. Feel free to reach out anytime you have questions about your academics or campus activities.")
                    .intent("CONVERSATIONAL")
                    .suggestedActions(List.of("Check my attendance", "KTU Activity Points criteria"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 8. "Thank you" / "Thanks"
        if (lower.matches(".*\\b(thank you|thanks|thx|thank u|tysm|appreciate it|thankyou)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("🌟 **You're very welcome!** Always happy to help.\n\n" +
                           "If you have more questions about your classes, attendance, or university rules, feel free to ask anytime. Have a wonderful day ahead!")
                    .intent("CONVERSATIONAL")
                    .suggestedActions(List.of("Check my attendance", "How do I apply for OD letter?", "KTU Activity Points criteria"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 3. "What's up" / "Sup"
        if (lower.matches(".*\\b(what's up|whats up|what is up|sup|wassup)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("👋 **Hey there!** Not much, just here and ready to help you navigate your college day! 🚀\n\n" +
                           "Need to check your attendance, look up an academic rule, or view upcoming campus drives? Let me know!")
                    .intent("CONVERSATIONAL")
                    .suggestedActions(List.of("Check my attendance", "What is KTU attendance rule?", "Upcoming placement drives"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 4. "Who are you" / "Who made you" / "What is your name" / "What is EduSync"
        if (lower.matches(".*\\b(who are you|what is your name|who made you|who created you|what is edusync|tell me about yourself|what are you)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("🤖 **I am EduSync AI Copilot**, your college assistant for **TKMIT & KTU**.\n\n" +
                           "> 🔒 **Notice:** *Conversations are recorded for the continuous training, evaluation, and quality improvement of this AI agent.*\n\n" +
                           "Here is what I can do for you:\n" +
                           "- 📊 **Live Records:** Check your real-time attendance %, duty leave status, and internal marks\n" +
                           "- 📘 **KTU Regulations:** Minimum attendance (75%), Condonation (60%), Activity points (120 pts), B.Tech 2024 scheme rules\n" +
                           "- 📄 **Document Intelligence:** Answer factual questions citing official KTU academic manuals and circulars\n" +
                           "- 💼 **Placements & Resumes:** Campus recruitment drives, ATS resume screening, and eligibility\n\n" +
                           "How can I help you today?")
                    .intent("ABOUT")
                    .suggestedActions(List.of("What is KTU attendance rule?", "Check my attendance percentage", "How do I apply for Duty Leave?", "KTU Activity Points criteria"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 5. "Tell me a joke" / "Joke"
        if (lower.matches(".*\\b(tell me a joke|tell a joke|joke|make me laugh)\\b.*")) {
            String[] jokes = {
                "Why do programmers always prefer dark mode?\n\n*Because light attracts bugs!* 🐛💻",
                "Why did the engineering student cross the road?\n\n*Because the algorithm told them to minimize path cost!* 🛣️🤖",
                "There are 10 types of people in the world:\n\n*Those who understand binary, and those who don't!* 🔢😄"
            };
            int idx = (int) (System.currentTimeMillis() % jokes.length);
            return AiDto.ChatResponse.builder()
                    .reply("😄 **Here's one for you:**\n\n> " + jokes[idx] + "\n\nHope that brought a smile to your study break! What else can I do for you today?")
                    .intent("ENTERTAINMENT")
                    .suggestedActions(List.of("Check my attendance", "KTU Activity Points criteria", "Tell me another joke"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 6. Time of day greetings: Good morning / afternoon / evening / night
        if (lower.matches(".*\\b(good morning|morning)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("🌅 **Good morning!** Wishing you an inspiring and productive day ahead at TKMIT! How can I assist you with your classes, attendance, or campus queries today?")
                    .intent("GREETING")
                    .suggestedActions(List.of("Check my attendance", "What is KTU attendance rule?", "Upcoming placement drives"))
                    .fromLocalLlm(false)
                    .build();
        }
        if (lower.matches(".*\\b(good afternoon)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("☀️ **Good afternoon!** Hope your day is going smoothly! Let me know if you need to check your attendance, internal marks, or KTU regulations.")
                    .intent("GREETING")
                    .suggestedActions(List.of("Check my attendance", "What is KTU attendance rule?", "How do I apply for OD letter?"))
                    .fromLocalLlm(false)
                    .build();
        }
        if (lower.matches(".*\\b(good evening)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("🌆 **Good evening!** Hope you had a rewarding day of lectures! How can I help you wrap up your day with academic or campus information?")
                    .intent("GREETING")
                    .suggestedActions(List.of("Check my attendance", "Calculate my CGPA", "Upcoming placement drives"))
                    .fromLocalLlm(false)
                    .build();
        }
        if (lower.matches(".*\\b(good night|goodnight)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("🌙 **Good night!** Rest well and best of luck with your studies tomorrow. Feel free to ask anytime if you need assistance!")
                    .intent("GREETING")
                    .suggestedActions(List.of("Check my attendance", "What is KTU attendance rule?"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 7. Farewells: Bye / Goodbye / See you
        if (lower.matches(".*\\b(bye|goodbye|see you|see ya|tata|cya|farewell)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("👋 **Goodbye!** It was a pleasure assisting you. Have a great day ahead, and feel free to reach out whenever you have questions!")
                    .intent("FAREWELL")
                    .suggestedActions(List.of("Check my attendance", "What is KTU attendance rule?"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 8. General help / What can you do / Features
        if (lower.matches(".*\\b(what can you do|help me|features|commands|menu|what do you do)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("### 🚀 EduSync AI Copilot — Capabilities\n\n" +
                           "Here is a quick overview of what I can help you with:\n\n" +
                           "1. 📊 **Attendance & Duty Leave:** Check your live attendance %, see if you meet the 75% KTU minimum, or check medical condonation rules.\n" +
                           "2. 🎯 **CGPA & Grades:** Use conversion formulas (`10 × CGPA - 2.5`), internal marks normalization, and SGPA calculations.\n" +
                           "3. 📘 **KTU Regulations (RAG):** Inquire about Honours/Minor degrees, credit requirements (170 credits), and project-based courses.\n" +
                           "4. 💼 **Placements & ATS Screening:** Analyze resumes against job descriptions, calculate ATS match scores, and explore active campus drives.\n" +
                           "5. 🎓 **MOOC & Credit Transfer:** Review NPTEL/SWAYAM credit transfer guidelines.\n\n" +
                           "What would you like to explore?")
                    .intent("HELP")
                    .suggestedActions(List.of("What is KTU attendance rule?", "How do I apply for Duty Leave?", "Check my current attendance", "Upcoming placement drives"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 9. Standard greetings: Hi / Hello / Hey / Namaste / Hai
        if (lower.matches("^(hi|hello|hey|hola|greetings|namaste|hai|yo)(\\s+.*|!|\\.|\\?)?$")) {
            return AiDto.ChatResponse.builder()
                    .reply("👋 **Hello! I am EduSync AI Copilot**, your college assistant for TKMIT & KTU.\n\n" +
                           "> 🔒 **Notice:** *Conversations are recorded for continuous training and quality improvement.*\n\n" +
                           "Here is what I can do for you:\n" +
                           "- 📊 **Live Records:** Check your real-time attendance %, duty leave status, and internal marks\n" +
                           "- 📘 **KTU Regulations:** Minimum attendance (75%), Condonation (60%), Activity points (120 pts), B.Tech 2024 scheme rules\n" +
                           "- 💼 **Placements & Resumes:** Campus recruitment drives, ATS resume screening, and eligibility\n\n" +
                           "How can I help you today?")
                    .intent("GREETING")
                    .suggestedActions(List.of("What is KTU attendance rule?", "Check my attendance percentage", "How do I apply for Duty Leave?", "KTU Activity Points criteria"))
                    .fromLocalLlm(false)
                    .build();
        }

        return null;
    }
}

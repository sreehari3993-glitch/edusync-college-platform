package com.edusync.service;

import com.edusync.dto.AiDto;
import com.edusync.rag.RagService;
import com.edusync.service.ai.AtsScreeningService;
import com.edusync.service.ai.ChatAuditLogger;
import com.edusync.service.ai.KtuKnowledgeBaseService;
import com.edusync.service.ai.OllamaClientService;
import com.edusync.service.ai.StudentContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private RagService ragService;
    @Mock
    private StudentContextResolver studentContextResolver;
    @Mock
    private OllamaClientService ollamaClientService;
    @Mock
    private KtuKnowledgeBaseService ktuKnowledgeBaseService;
    @Mock
    private AtsScreeningService atsScreeningService;
    @Mock
    private ChatAuditLogger chatAuditLogger;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiService(
                ragService,
                studentContextResolver,
                ollamaClientService,
                ktuKnowledgeBaseService,
                atsScreeningService,
                chatAuditLogger
        );
    }

    @Test
    void testChatGreeting() {
        AiDto.ChatRequest request = new AiDto.ChatRequest();
        request.setMessage("hello");

        AiDto.ChatResponse response = aiService.chat(request, 1L);

        assertNotNull(response);
        assertEquals("GREETING", response.getIntent());
        assertFalse(response.isFromLocalLlm());
        verify(chatAuditLogger, times(1)).recordChatAuditLog(eq(1L), eq("hello"), eq(response));
    }

    @Test
    void testChatPersonalQueryResolution() {
        AiDto.ChatRequest request = new AiDto.ChatRequest();
        request.setMessage("my attendance");

        when(studentContextResolver.isPersonalStudentQuery("my attendance")).thenReturn(true);
        AiDto.ChatResponse mockPersonalResponse = AiDto.ChatResponse.builder()
                .reply("Your attendance is 85%")
                .intent("PERSONAL_ATTENDANCE")
                .build();
        when(studentContextResolver.resolvePersonalQuery("my attendance", 1L)).thenReturn(mockPersonalResponse);

        AiDto.ChatResponse response = aiService.chat(request, 1L);

        assertNotNull(response);
        assertEquals("PERSONAL_ATTENDANCE", response.getIntent());
        assertEquals("Your attendance is 85%", response.getReply());
        verify(chatAuditLogger, times(1)).recordChatAuditLog(eq(1L), eq("my attendance"), eq(mockPersonalResponse));
    }

    @Test
    void testChatRagResolution() {
        AiDto.ChatRequest request = new AiDto.ChatRequest();
        request.setMessage("What is SFA?");

        when(studentContextResolver.isPersonalStudentQuery("what is sfa?")).thenReturn(false);
        when(ragService.answerWithRag("What is SFA?")).thenReturn("SFA is Student Faculty Advisor according to KTU docs.");

        AiDto.ChatResponse response = aiService.chat(request, null);

        assertNotNull(response);
        assertEquals("KTU_RAG_DOCUMENT", response.getIntent());
        assertTrue(response.isFromLocalLlm());
        verify(chatAuditLogger, times(1)).recordChatAuditLog(isNull(), eq("What is SFA?"), eq(response));
    }

    @Test
    void testChatFallbackToKnowledgeBase() {
        AiDto.ChatRequest request = new AiDto.ChatRequest();
        request.setMessage("What are MOOC course rules?");

        when(studentContextResolver.isPersonalStudentQuery("what are mooc course rules?")).thenReturn(false);
        when(ragService.answerWithRag("What are MOOC course rules?")).thenReturn(null);
        when(ollamaClientService.tryQueryLocalLlm("What are MOOC course rules?")).thenReturn(null);

        AiDto.ChatResponse mockKbResponse = AiDto.ChatResponse.builder()
                .reply("MOOC courses must be 12-14 weeks")
                .intent("MOOC_RULES")
                .build();
        when(ktuKnowledgeBaseService.resolveKnowledgeQuery("What are MOOC course rules?")).thenReturn(mockKbResponse);

        AiDto.ChatResponse response = aiService.chat(request, null);

        assertNotNull(response);
        assertEquals("MOOC_RULES", response.getIntent());
        assertEquals("MOOC courses must be 12-14 weeks", response.getReply());
        verify(chatAuditLogger, times(1)).recordChatAuditLog(isNull(), eq("What are MOOC course rules?"), eq(mockKbResponse));
    }
}

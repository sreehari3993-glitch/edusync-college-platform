package com.edusync.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Isolated HTTP client for communicating with local or remote Ollama LLM instances.
 */
@Slf4j
@Service
public class OllamaClientService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${edusync.ai.ollama-url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${edusync.ai.primary-model:edusync-ai}")
    private String primaryModel;

    @Value("${edusync.ai.fallback-model:llama3}")
    private String fallbackModel;

    private static final String SYSTEM_PROMPT =
            "You are EduSync AI, an advanced AI Copilot engineered on the conversational and technical excellence of Google Gemini, Claude 3.5 Sonnet, and ChatGPT. " +
            "You assist engineering college students, faculty, and campus placement aspirants.\n\n" +
            "Response Guidelines:\n" +
            "1. STRUCTURE: Use clear Markdown headings (###), bold key terms, numbered lists, and bullet points.\n" +
            "2. CODE: When writing code (Java, Python, C++, SQL, Web), provide clean, complete, idiomatic code blocks with complexity analysis.\n" +
            "3. CAREER & EXAMS: Provide structured, actionable advice (e.g. STAR method for HR interviews, core CS fundamentals, KTU strategies).\n" +
            "4. TONE: Intelligent, empathetic, encouraging, and highly informative.";

    /**
     * Attempts to query local Ollama LLM endpoint with 30s timeout.
     * Returns null if offline or model not found.
     */
    public String tryQueryLocalLlm(String prompt) {
        // 1. Try primary fine-tuned model first
        String response = queryOllamaModel(primaryModel, prompt);
        if (response != null && !response.isBlank()) {
            return response;
        }
        // 2. Fallback to generic model if available
        return queryOllamaModel(fallbackModel, prompt);
    }

    public String queryOllamaModel(String modelName, String prompt) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            ObjectNode payload = OBJECT_MAPPER.createObjectNode();
            payload.put("model", modelName);
            payload.put("system", SYSTEM_PROMPT);
            payload.put("prompt", prompt);
            payload.put("stream", false);

            String jsonBody = OBJECT_MAPPER.writeValueAsString(payload);

            String endpoint = ollamaUrl.endsWith("/") ? ollamaUrl + "api/generate" : ollamaUrl + "/api/generate";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                JsonNode root = OBJECT_MAPPER.readTree(resp.body());
                if (root.has("response")) {
                    String answer = root.path("response").asText();
                    if (answer != null && !answer.isBlank()) {
                        return answer;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Local Ollama call to model [{}] skipped or timed out: {}", modelName, e.getMessage());
        }
        return null;
    }
}

package com.edusync.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Embedded Local RAG (Retrieval-Augmented Generation) Engine.
 * 
 * Provides:
 * 1. Automatic PDF parsing & page-aware chunking for KTU manuals.
 * 2. Local vector embeddings via Ollama (nomic-embed-text).
 * 3. In-memory sub-millisecond cosine similarity vector search.
 * 4. Grounded citation synthesis passing exact source chunks to LLM.
 */
@Slf4j
@Service
public class RagService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EMBEDDING_MODEL = "nomic-embed-text";
    private static final String LLM_MODEL = "edusync-ai";
    private static final String FALLBACK_LLM_MODEL = "llama3";

    private final List<DocumentChunk> vectorStore = new CopyOnWriteArrayList<>();
    private final Set<String> indexedFiles = ConcurrentHashMap.newKeySet();

    @Value("${edusync.rag.docs-path:ktu-docs/}")
    private String docsPath;

    @Data
    @Builder
    public static class DocumentChunk {
        private String id;
        private String sourceName;
        private int pageNumber;
        private String text;
        private double[] embedding;
    }

    @Data
    @Builder
    public static class SearchResult {
        private DocumentChunk chunk;
        private double score;
    }

    @PostConstruct
    public void init() {
        // Asynchronously ingest documents so application startup is not blocked
        java.util.concurrent.CompletableFuture.runAsync(this::ingestLocalDocuments);
    }

    /**
     * Scans the root ktu-docs/ directory for KTU PDF, Excel (.xlsx, .xls), and CSV resources and indexes them.
     */
    public synchronized void ingestLocalDocuments() {
        try {
            log.info("🔍 [RAG] Scanning root 'ktu-docs/' directory for KTU documents (PDF, Excel, CSV)...");
            
            Path localDir = Paths.get("ktu-docs");
            if (!Files.exists(localDir)) {
                Files.createDirectories(localDir);
            }

            try (var stream = Files.walk(localDir, 2)) {
                stream.filter(Files::isRegularFile)
                      .filter(this::isSupportedDocument)
                      .forEach(this::indexFilePath);
            }

            log.info("✅ [RAG] Document Indexing Complete. Total vector chunks in memory: {}", vectorStore.size());
        } catch (Exception e) {
            log.warn("⚠️ [RAG] Document ingestion notice: {}", e.getMessage());
        }
    }

    private boolean isSupportedDocument(Path path) {
        return isSupportedFilename(path.getFileName().toString());
    }

    private boolean isSupportedFilename(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv") || lower.endsWith(".txt") || lower.endsWith(".md");
    }

    public void indexFilePath(Path path) {
        String filename = path.getFileName().toString();
        if (indexedFiles.contains(filename)) return;

        try {
            byte[] bytes = Files.readAllBytes(path);
            indexBytes(filename, bytes);
        } catch (Exception e) {
            log.error("Failed to read document [{}]: {}", filename, e.getMessage());
        }
    }

    public void indexStream(String filename, InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            indexBytes(filename, bytes);
        } catch (Exception e) {
            log.error("Failed to read stream for document [{}]: {}", filename, e.getMessage());
        }
    }

    public void indexBytes(String filename, byte[] bytes) {
        if (filename == null || bytes == null || bytes.length == 0 || indexedFiles.contains(filename)) return;

        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            indexPdfBytes(filename, bytes);
        } else if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            indexExcelBytes(filename, bytes);
        } else if (lower.endsWith(".csv")) {
            indexCsvBytes(filename, bytes);
        } else if (lower.endsWith(".txt") || lower.endsWith(".md")) {
            indexTextBytes(filename, bytes);
        }
    }

    /**
     * Parses plain text and markdown training knowledge files into semantic chunks.
     */
    public void indexTextBytes(String filename, byte[] bytes) {
        if (indexedFiles.contains(filename)) return;

        try {
            String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            if (content.isBlank()) return;

            log.info("📝 [RAG] Indexing Text Knowledge Guide: {}...", filename);
            List<String> chunks = splitIntoChunks(content, 600, 100);

            int chunkCount = 0;
            for (String chunkText : chunks) {
                double[] vector = generateEmbedding(chunkText);
                if (vector != null && vector.length > 0) {
                    DocumentChunk chunk = DocumentChunk.builder()
                            .id(filename + "-c" + chunkCount)
                            .sourceName(filename)
                            .pageNumber(1)
                            .text(chunkText)
                            .embedding(vector)
                            .build();
                    vectorStore.add(chunk);
                    chunkCount++;
                }
            }

            indexedFiles.add(filename);
            log.info("✅ [RAG] Indexed Text Guide [{}] successfully ({} chunks generated)", filename, chunkCount);
        } catch (Exception e) {
            log.error("❌ [RAG] Failed to index text guide [{}]: {}", filename, e.getMessage());
        }
    }

    /**
     * Parses Excel spreadsheet (.xlsx / .xls) and creates vector embeddings per sheet/table section.
     */
    public void indexExcelBytes(String filename, byte[] bytes) {
        if (indexedFiles.contains(filename)) return;

        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
             org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(bais)) {

            org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
            log.info("📊 [RAG] Indexing Excel spreadsheet: {} ({} sheets)...", filename, workbook.getNumberOfSheets());

            int chunkCount = 0;
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                int rowCount = sheet.getPhysicalNumberOfRows();
                if (rowCount == 0) continue;

                // Read header row
                List<String> headers = new ArrayList<>();
                org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    for (org.apache.poi.ss.usermodel.Cell cell : headerRow) {
                        headers.add(formatter.formatCellValue(cell).trim());
                    }
                }

                StringBuilder sheetText = new StringBuilder();
                sheetText.append(String.format("### Sheet: %s\n", sheetName));

                for (int r = 1; r < rowCount; r++) {
                    org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                    if (row == null) continue;

                    StringBuilder rowText = new StringBuilder();
                    int cellIdx = 0;
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        String val = formatter.formatCellValue(cell).trim();
                        if (!val.isBlank()) {
                            String header = cellIdx < headers.size() ? headers.get(cellIdx) : ("Col " + (cellIdx + 1));
                            rowText.append(header).append(": ").append(val).append(" | ");
                        }
                        cellIdx++;
                    }

                    if (!rowText.isEmpty()) {
                        sheetText.append("- Row ").append(r + 1).append(": ").append(rowText).append("\n");
                    }
                }

                List<String> chunks = splitIntoChunks(sheetText.toString(), 600, 100);
                for (String chunkText : chunks) {
                    double[] vector = generateEmbedding(chunkText);
                    if (vector != null && vector.length > 0) {
                        DocumentChunk chunk = DocumentChunk.builder()
                                .id(filename + "-s" + s + "-c" + chunkCount)
                                .sourceName(filename)
                                .pageNumber(s + 1) // Sheet index as page representation
                                .text(chunkText)
                                .embedding(vector)
                                .build();
                        vectorStore.add(chunk);
                        chunkCount++;
                    }
                }
            }

            indexedFiles.add(filename);
            log.info("✅ [RAG] Indexed Excel [{}] successfully ({} chunks generated)", filename, chunkCount);
        } catch (Exception e) {
            log.error("❌ [RAG] Failed to index Excel [{}]: {}", filename, e.getMessage());
        }
    }

    /**
     * Parses CSV files into structured table row chunks for vector search.
     */
    public void indexCsvBytes(String filename, byte[] bytes) {
        if (indexedFiles.contains(filename)) return;

        try {
            String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            String[] lines = content.split("\r?\n");
            if (lines.length == 0) return;

            String[] headers = lines[0].split(",");
            StringBuilder csvText = new StringBuilder();
            csvText.append(String.format("### CSV Table: %s\n", filename));

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isBlank()) continue;

                String[] values = line.split(",");
                StringBuilder rowText = new StringBuilder();
                for (int v = 0; v < values.length; v++) {
                    String h = v < headers.length ? headers[v].trim() : ("Col " + (v + 1));
                    rowText.append(h).append(": ").append(values[v].trim()).append(" | ");
                }
                csvText.append("- Row ").append(i + 1).append(": ").append(rowText).append("\n");
            }

            List<String> chunks = splitIntoChunks(csvText.toString(), 600, 100);
            int chunkCount = 0;
            for (String chunkText : chunks) {
                double[] vector = generateEmbedding(chunkText);
                if (vector != null && vector.length > 0) {
                    DocumentChunk chunk = DocumentChunk.builder()
                            .id(filename + "-c" + chunkCount)
                            .sourceName(filename)
                            .pageNumber(1)
                            .text(chunkText)
                            .embedding(vector)
                            .build();
                    vectorStore.add(chunk);
                    chunkCount++;
                }
            }

            indexedFiles.add(filename);
            log.info("✅ [RAG] Indexed CSV [{}] successfully ({} chunks generated)", filename, chunkCount);
        } catch (Exception e) {
            log.error("❌ [RAG] Failed to index CSV [{}]: {}", filename, e.getMessage());
        }
    }

    public void indexPdfBytes(String filename, byte[] bytes) {
        if (indexedFiles.contains(filename)) return;

        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();
            log.info("📄 [RAG] Indexing PDF: {} ({} pages)...", filename, totalPages);

            int chunkCount = 0;
            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();

                if (pageText.isBlank()) continue;

                List<String> chunks = splitIntoChunks(pageText, 500, 80);
                for (String chunkText : chunks) {
                    double[] vector = generateEmbedding(chunkText);
                    if (vector != null && vector.length > 0) {
                        DocumentChunk chunk = DocumentChunk.builder()
                                .id(filename + "-p" + page + "-c" + chunkCount)
                                .sourceName(filename)
                                .pageNumber(page)
                                .text(chunkText)
                                .embedding(vector)
                                .build();
                        vectorStore.add(chunk);
                        chunkCount++;
                    }
                }
            }

            indexedFiles.add(filename);
            log.info("✅ [RAG] Indexed [{}] successfully ({} chunks generated)", filename, chunkCount);
        } catch (Exception e) {
            log.error("❌ [RAG] Failed to index PDF [{}]: {}", filename, e.getMessage());
        }
    }

    /**
     * Splits text into overlapping character windows prioritizing sentence boundaries.
     */
    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String cleanText = text.replaceAll("\\s+", " ").trim();
        if (cleanText.length() <= chunkSize) {
            chunks.add(cleanText);
            return chunks;
        }

        int start = 0;
        while (start < cleanText.length()) {
            int end = Math.min(start + chunkSize, cleanText.length());

            // Try to break at sentence or clause end
            if (end < cleanText.length()) {
                int period = cleanText.lastIndexOf('.', end);
                int newline = cleanText.lastIndexOf('\n', end);
                int breakPoint = Math.max(period, newline);
                if (breakPoint > start + (chunkSize / 2)) {
                    end = breakPoint + 1;
                }
            }

            String chunk = cleanText.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end >= cleanText.length()) break;
            start = Math.max(start + 1, end - overlap);
        }
        return chunks;
    }

    /**
     * Performs semantic vector search and retrieves top K most relevant passages.
     */
    public List<SearchResult> search(String query, int topK, double minScoreThreshold) {
        if (vectorStore.isEmpty() || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        double[] queryVector = generateEmbedding(query);
        if (queryVector == null || queryVector.length == 0) {
            return Collections.emptyList();
        }

        List<SearchResult> results = new ArrayList<>();
        for (DocumentChunk chunk : vectorStore) {
            double score = cosineSimilarity(queryVector, chunk.getEmbedding());
            if (score >= minScoreThreshold) {
                results.add(SearchResult.builder().chunk(chunk).score(score).build());
            }
        }

        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results.stream().limit(topK).collect(Collectors.toList());
    }

    /**
     * Queries Ollama with retrieved RAG context for a grounded, accurate response.
     */
    public String answerWithRag(String query) {
        if (query == null || query.isBlank()) return null;

        // Retrieve top 4 matching chunks
        List<SearchResult> searchResults = search(query, 4, 0.30);
        if (searchResults.isEmpty()) {
            return null; // No relevant PDF context found -> let standard LLM or fallback answer
        }

        log.info("🎯 [RAG] Retrieved {} relevant chunk(s) (Top Score: {}) for query: '{}'",
                searchResults.size(),
                String.format("%.3f", searchResults.get(0).getScore()),
                query);

        StringBuilder contextBuilder = new StringBuilder();
        Set<String> citations = new LinkedHashSet<>();

        for (SearchResult res : searchResults) {
            DocumentChunk c = res.getChunk();
            contextBuilder.append(String.format("--- [Source: %s | Page %d] ---\n%s\n\n",
                    c.getSourceName(), c.getPageNumber(), c.getText()));
            citations.add(String.format("📄 *%s* (Page %d)", c.getSourceName(), c.getPageNumber()));
        }

        String augmentedPrompt = String.format(
                "You are EduSync AI, a premier college AI copilot engineered with the intelligence and conversational excellence of Claude 3.5 and Google Gemini.\n\n" +
                "CONTEXTUAL UNIVERSITY & CURRICULUM DOCUMENTS (Use for factual rules, credits, and syllabus details when relevant):\n%s\n\n" +
                "STUDENT QUERY: %s\n\n" +
                "Response Guidelines:\n" +
                "1. REGULATION QUERIES: If the question is about official rules, attendance, marks, credits, or ordinances, provide accurate facts citing relevant clauses.\n" +
                "2. COMPARATIVE & CAREER ADVICE: If the student asks for branch comparisons (e.g. Computer Science vs Civil, AI vs Data Science), career scope, or study advice, provide a balanced, insightful, and comprehensive analysis (industry demand, core vs IT scope, career paths, pros & cons, placement packages) rather than forcing literal textbook excerpts.\n" +
                "3. FORMATTING: Structure responses with Markdown headings (###), bold key terms, and bullet points.",
                contextBuilder.toString(),
                query
        );

        String answer = queryLlm(LLM_MODEL, augmentedPrompt);
        if (answer == null || answer.isBlank()) {
            answer = queryLlm(FALLBACK_LLM_MODEL, augmentedPrompt);
        }

        if (answer != null && !answer.isBlank()) {
            StringBuilder finalReply = new StringBuilder(answer.trim());
            if (!citations.isEmpty()) {
                finalReply.append("\n\n**Sources & References:**\n");
                for (String cit : citations) {
                    finalReply.append("- ").append(cit).append("\n");
                }
            }
            return finalReply.toString();
        }

        return null;
    }

    /**
     * Generates a 768-dim vector embedding via Ollama nomic-embed-text endpoint.
     */
    public double[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) return null;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            ObjectNode body = OBJECT_MAPPER.createObjectNode();
            body.put("model", EMBEDDING_MODEL);
            body.put("prompt", text.replaceAll("\\s+", " ").trim());

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/embeddings"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                JsonNode root = OBJECT_MAPPER.readTree(resp.body());
                if (root.has("embedding") && root.get("embedding").isArray()) {
                    JsonNode array = root.get("embedding");
                    double[] vector = new double[array.size()];
                    for (int i = 0; i < array.size(); i++) {
                        vector[i] = array.get(i).asDouble();
                    }
                    return vector;
                }
            }
        } catch (Exception e) {
            log.debug("Embedding generation skipped: {}", e.getMessage());
        }
        return null;
    }

    private String queryLlm(String modelName, String prompt) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            ObjectNode payload = OBJECT_MAPPER.createObjectNode();
            payload.put("model", modelName);
            payload.put("system", "You are EduSync AI, an elite college AI assistant designed on the principles of Claude 3.5 and Google Gemini. " +
                    "Provide clear, beautifully formatted responses with Markdown headings, bullet points, and exact clause citations.");
            payload.put("prompt", prompt);
            payload.put("stream", false);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(25))
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(payload)))
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
            log.debug("LLM query to model [{}] failed: {}", modelName, e.getMessage());
        }
        return null;
    }

    private double cosineSimilarity(double[] vecA, double[] vecB) {
        if (vecA == null || vecB == null || vecA.length != vecB.length) return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public int getIndexedChunkCount() {
        return vectorStore.size();
    }

    public Set<String> getIndexedFiles() {
        return Collections.unmodifiableSet(indexedFiles);
    }
}

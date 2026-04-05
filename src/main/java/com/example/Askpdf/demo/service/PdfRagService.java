package com.example.Askpdf.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PdfRagService {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;

    // Each uploaded PDF gets its own vector store
    // Key = conversationId, Value = vector store for that PDF
    private final Map<String, SimpleVectorStore> pdfVectorStores
            = new ConcurrentHashMap<>();

    public PdfRagService(ChatClient.Builder builder,
                         EmbeddingModel embeddingModel) {

        this.embeddingModel = embeddingModel;

        this.chatClient = builder
                .defaultSystem("""
                        You are a helpful assistant that answers questions
                        based on the content of uploaded PDF documents.
                        Answer ONLY using the context provided.
                        If the answer is not in the context, clearly say:
                        "I couldn't find that information in the PDF."
                        Be concise and accurate.
                        """)
                .build();
    }

    // Called when user uploads a PDF
    public String ingestPdf(MultipartFile file) throws IOException {

        // Generate unique ID for this PDF session
        String conversationId = UUID.randomUUID().toString();

        // Save uploaded file to temp location
        java.io.File tempFile = java.io.File.createTempFile(
                "upload-", ".pdf");
        file.transferTo(tempFile);

        // Read PDF — Spring AI's PagePdfDocumentReader
        // reads page by page
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                new org.springframework.core.io.FileSystemResource(tempFile)
        );
        List<Document> pages = pdfReader.get();

        // Split pages into smaller chunks
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(pages);

        // Create a fresh vector store for this PDF
        SimpleVectorStore vectorStore = SimpleVectorStore
                .builder(embeddingModel)
                .build();

        // Embed and store all chunks
        vectorStore.add(chunks);

        // Store under the conversation ID
        pdfVectorStores.put(conversationId, vectorStore);

        // Cleanup temp file
        tempFile.delete();

        System.out.println("PDF ingested: " + file.getOriginalFilename()
                + " → " + chunks.size() + " chunks → session: "
                + conversationId);

        return conversationId;
    }

    // Called when user sends a question
    public String askPdf(String conversationId, String question) {

        SimpleVectorStore vectorStore = pdfVectorStores.get(conversationId);

        if (vectorStore == null) {
            return "No PDF found for this session. "
                    + "Please upload a PDF first.";
        }

        // Search for relevant chunks
        List<Document> relevantChunks = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(4)
                        .build()
        );

        if (relevantChunks.isEmpty()) {
            return "I couldn't find relevant content in the PDF "
                    + "to answer your question.";
        }

        // Build context from relevant chunks
        String context = relevantChunks.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // Inject context into prompt
        String promptWithContext = """
                Answer the question using ONLY this context from the PDF:
                
                """ + context + """
                
                Question: """ + question;

        return chatClient
                .prompt(promptWithContext)
                .call()
                .content();
    }

    public Flux<String> askPdfStream(String conversationId, String question) {

        SimpleVectorStore vectorStore = pdfVectorStores.get(conversationId);

        if (vectorStore == null) {
            return Flux.just("No PDF found for this session. "
                    + "Please upload a PDF first.");
        }

        // Search for relevant chunks
        List<Document> relevantChunks = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(4)
                        .build()
        );

        if (relevantChunks.isEmpty()) {
            return Flux.just("I couldn't find relevant content in the PDF to answer your question.");
        }

        // Build context from relevant chunks
        String context = relevantChunks.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // Inject context into prompt
        String promptWithContext = """
                Answer the question using ONLY this context from the PDF:
                
                """ + context + """
                
                Question: """ + question;

        return chatClient
                .prompt(promptWithContext)
                .stream()
                .content();
    }

    // How many PDFs are currently loaded
    public int getActiveSessions() {
        return pdfVectorStores.size();
    }

    // Check if a session exists
    public boolean sessionExists(String conversationId) {
        return pdfVectorStores.containsKey(conversationId);
    }

    // Clear a session when done
    public void clearSession(String conversationId) {
        pdfVectorStores.remove(conversationId);
    }
}
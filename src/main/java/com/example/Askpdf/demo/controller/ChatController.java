package com.example.Askpdf.demo.controller;


import com.example.Askpdf.demo.dto.ChatRequest;
import com.example.Askpdf.demo.dto.ChatResponse;
import com.example.Askpdf.demo.service.PdfRagService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final PdfRagService pdfRagService;

    public ChatController(PdfRagService pdfRagService) {
        this.pdfRagService = pdfRagService;
    }

    // POST /api/chat
    // Send a question about the uploaded PDF
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {

        if (request.conversationId() == null
                || request.conversationId().isBlank()) {
            return new ChatResponse(
                    "Please upload a PDF first before asking questions.",
                    null);
        }

        String reply = pdfRagService.askPdf(
                request.conversationId(),
                request.message()
        );

        return new ChatResponse(reply, request.conversationId());
    }
    @PostMapping(value = "/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat1(@RequestBody ChatRequest request) {
        if (request.conversationId() == null
                || request.conversationId().isBlank()) {
            return Flux.just("Please upload a PDF first before asking questions.");
        }
        return pdfRagService.askPdfStream(request.conversationId(),
                request.message());
    }
}

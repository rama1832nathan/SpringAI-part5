package com.example.Askpdf.demo.controller;


import com.example.Askpdf.demo.dto.UploadResponse;
import com.example.Askpdf.demo.service.PdfRagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final PdfRagService pdfRagService;

    public PdfController(PdfRagService pdfRagService) {
        this.pdfRagService = pdfRagService;
    }

    // POST /api/pdf/upload
    // Accepts a PDF file, returns a conversationId
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadPdf(
            @RequestParam("file") MultipartFile file) {

        // Validate file type
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(false,
                            "File is empty", null, 0));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(false,
                            "Only PDF files are allowed", null, 0));
        }

        try {
            String conversationId = pdfRagService.ingestPdf(file);
            return ResponseEntity.ok(new UploadResponse(
                    true,
                    "PDF uploaded successfully! You can now ask questions.",
                    conversationId,
                    0
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new UploadResponse(false,
                            "Failed to process PDF: " + e.getMessage(),
                            null, 0));
        }
    }

    // DELETE /api/pdf/{conversationId}
    // Clears a session
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<String> clearSession(
            @PathVariable String conversationId) {
        pdfRagService.clearSession(conversationId);
        return ResponseEntity.ok("Session cleared.");
    }
}

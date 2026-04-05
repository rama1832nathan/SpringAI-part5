package com.example.Askpdf.demo.dto;

public record UploadResponse(boolean success,
                            String message,
                            String conversationId,
                            int totalChunks) {
}

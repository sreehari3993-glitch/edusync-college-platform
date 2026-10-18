package com.edusync.util;

import com.edusync.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Validates uploaded files against allowlists, magic bytes, and size limits.
 * Derives secure MIME types from validated extensions rather than trusting the client.
 */
public final class FileUploadValidator {

    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB multipart limit

    public static final Set<String> GENERAL_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg",
            "doc", "docx", "ppt", "pptx",
            "xls", "xlsx", "txt", "csv", "zip"
    );

    public static final Set<String> STRICT_MOU_AND_CERT_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg"
    );

    public static final Set<String> IMAGE_ONLY_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg"
    );

    // Magic byte signatures
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}; // PNG signature
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}; // JPEG SOI

    private FileUploadValidator() {}

    public static class ValidatedFile {
        private final String sanitizedFilename;
        private final String extension;
        private final String derivedMimeType;
        private final byte[] bytes;

        public ValidatedFile(String sanitizedFilename, String extension, String derivedMimeType, byte[] bytes) {
            this.sanitizedFilename = sanitizedFilename;
            this.extension = extension;
            this.derivedMimeType = derivedMimeType;
            this.bytes = bytes;
        }

        public String getSanitizedFilename() { return sanitizedFilename; }
        public String getExtension() { return extension; }
        public String getDerivedMimeType() { return derivedMimeType; }
        public byte[] getBytes() { return bytes; }
    }

    /**
     * Validates generic and strict uploads.
     * @param file Multipart file
     * @param strictMouOrCert if true, only pdf, png, jpg, jpeg are allowed
     */
    public static ValidatedFile validateUpload(MultipartFile file, boolean strictMouOrCert) {
        Set<String> allowed = strictMouOrCert ? STRICT_MOU_AND_CERT_EXTENSIONS : GENERAL_EXTENSIONS;
        return validate(file, allowed, strictMouOrCert ? "MoU and certificate" : "Uploaded");
    }

    /**
     * Validates image-only uploads (e.g. avatar / profile photos).
     */
    public static ValidatedFile validateImageUpload(MultipartFile file) {
        return validate(file, IMAGE_ONLY_EXTENSIONS, "Profile photo");
    }

    private static ValidatedFile validate(MultipartFile file, Set<String> allowedExtensions, String contextLabel) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required and cannot be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed limit of 10MB.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty() || !originalName.contains(".")) {
            throw new BadRequestException("File must have a valid extension.");
        }

        String rawName = originalName.trim();
        String ext = rawName.substring(rawName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT).trim();

        if (ext.isEmpty() || !allowedExtensions.contains(ext)) {
            throw new BadRequestException(contextLabel + " files must have an allowed extension. Allowed: "
                    + String.join(", ", allowedExtensions) + ". Received: ." + ext);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read file content: " + e.getMessage());
        }

        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("File content cannot be empty.");
        }

        if (bytes.length > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed limit of 10MB.");
        }

        // Verify magic bytes for supported binary types
        verifyMagicBytes(ext, bytes);

        String derivedMime = deriveMimeType(ext);
        String sanitizedName = sanitizeFilename(rawName);

        return new ValidatedFile(sanitizedName, ext, derivedMime, bytes);
    }

    public static void verifyMagicBytes(String extension, byte[] bytes) {
        switch (extension.toLowerCase(Locale.ROOT)) {
            case "pdf" -> {
                if (!matchesMagic(bytes, PDF_MAGIC)) {
                    throw new BadRequestException("File content does not match PDF format (invalid %PDF- magic bytes).");
                }
            }
            case "png" -> {
                if (!matchesMagic(bytes, PNG_MAGIC)) {
                    throw new BadRequestException("File content does not match PNG format (invalid PNG magic bytes).");
                }
            }
            case "jpg", "jpeg" -> {
                if (!matchesMagic(bytes, JPEG_MAGIC)) {
                    throw new BadRequestException("File content does not match JPEG format (invalid JPEG magic bytes).");
                }
            }
            default -> {
                // Other extensions (txt, csv, zip, doc, etc.) do not have mandatory single magic bytes here
            }
        }
    }

    private static boolean matchesMagic(byte[] data, byte[] magic) {
        if (data == null || data.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    public static String deriveMimeType(String extension) {
        if (extension == null) return "application/octet-stream";
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        // Extract base name without directory components
        int lastSlash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String base = (lastSlash >= 0 && lastSlash < filename.length() - 1)
                ? filename.substring(lastSlash + 1)
                : filename;

        return base.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

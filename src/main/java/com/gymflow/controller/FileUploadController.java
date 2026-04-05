package com.gymflow.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@Slf4j
public class FileUploadController {

    private static final String UPLOAD_DIR = "/app/uploads/";
    private static final String[] ALLOWED = {".jpg", ".jpeg", ".png", ".webp", ".gif"};

    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(defaultValue = "general") String type) {
        if (file.isEmpty()) throw new RuntimeException("No file provided");

        String original = file.getOriginalFilename();
        String ext = original != null && original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".jpg";
        boolean valid = false;
        for (String a : ALLOWED) if (ext.equalsIgnoreCase(a)) { valid = true; break; }
        if (!valid) throw new RuntimeException("Only image files allowed: jpg, png, webp, gif");

        String filename = type + "_" + UUID.randomUUID() + ext;
        try {
            Path dir = Paths.get(UPLOAD_DIR + type);
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String url = "/api/upload/files/" + type + "/" + filename;
            log.info("Uploaded: {} -> {}", original, url);
            return ResponseEntity.ok(Map.of("url", url, "filename", filename));
        } catch (IOException e) {
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/files/{type}/{filename}")
    public ResponseEntity<byte[]> serve(@PathVariable String type, @PathVariable String filename) {
        try {
            Path file = Paths.get(UPLOAD_DIR + type).resolve(filename);
            byte[] data = Files.readAllBytes(file);
            String ct = filename.endsWith(".png") ? "image/png" : filename.endsWith(".webp") ? "image/webp" : "image/jpeg";
            return ResponseEntity.ok().header("Content-Type", ct)
                    .header("Cache-Control", "public, max-age=86400").body(data);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

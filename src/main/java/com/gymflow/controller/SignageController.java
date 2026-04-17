package com.gymflow.controller;

import com.gymflow.dto.Dtos.*;
import com.gymflow.service.SignageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/signage") @RequiredArgsConstructor
public class SignageController {
    private final SignageService svc;

    // ===== DEVICES =====
    @GetMapping("/devices")
    public ResponseEntity<List<SignageDeviceResponse>> getDevices(@RequestParam UUID branchId) {
        return ResponseEntity.ok(svc.getDevices(branchId));
    }

    @PostMapping("/devices")
    public ResponseEntity<SignageDeviceResponse> createDevice(@Valid @RequestBody SignageDeviceRequest req,
            @RequestParam UUID branchId, @RequestParam UUID companyId) {
        return ResponseEntity.ok(svc.createDevice(req, branchId, companyId));
    }

    @PostMapping("/devices/{id}/assign-playlist")
    public ResponseEntity<SignageDeviceResponse> assignPlaylist(@PathVariable UUID id, @RequestParam UUID playlistId) {
        return ResponseEntity.ok(svc.assignPlaylist(id, playlistId));
    }

    @DeleteMapping("/devices/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID id) {
        svc.deleteDevice(id); return ResponseEntity.noContent().build();
    }

    // ===== CONTENT =====
    @GetMapping("/content")
    public ResponseEntity<List<SignageContentResponse>> getContent(@RequestParam UUID branchId) {
        return ResponseEntity.ok(svc.getContent(branchId));
    }

    @PostMapping("/content/upload")
    public ResponseEntity<SignageContentResponse> uploadContent(
            @RequestParam("file") MultipartFile file,
            @RequestParam String name,
            @RequestParam(defaultValue = "VIDEO") String contentType,
            @RequestParam(required = false) Integer durationSeconds,
            @RequestParam UUID branchId, @RequestParam UUID companyId) throws IOException {
        // Save file locally (in production → S3)
        String dir = "/app/uploads/signage";
        new File(dir).mkdirs();
        String ext = file.getOriginalFilename() != null ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.')) : ".mp4";
        String storedName = UUID.randomUUID() + ext;
        File dest = new File(dir, storedName);
        file.transferTo(dest);

        // Calculate checksum
        String checksum;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(file.getBytes());
            checksum = new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) { checksum = UUID.randomUUID().toString(); }

        String fileUrl = "/api/signage/media/" + storedName;
        return ResponseEntity.ok(svc.createContent(name, contentType, file.getOriginalFilename(),
            fileUrl, file.getContentType(), file.getSize(), durationSeconds, checksum, branchId, companyId));
    }

    @DeleteMapping("/content/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable UUID id) {
        svc.deleteContent(id); return ResponseEntity.noContent().build();
    }

    // Serve media files
    @GetMapping("/media/{filename}")
    public ResponseEntity<org.springframework.core.io.Resource> serveMedia(@PathVariable String filename) throws IOException {
        File file = new File("/app/uploads/signage", filename);
        if (!file.exists()) return ResponseEntity.notFound().build();
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);
        String mime = filename.endsWith(".mp4") ? "video/mp4" : filename.endsWith(".webm") ? "video/webm"
            : filename.endsWith(".jpg") || filename.endsWith(".jpeg") ? "image/jpeg" : filename.endsWith(".png") ? "image/png" : "application/octet-stream";
        return ResponseEntity.ok().header("Content-Type", mime)
            .header("Accept-Ranges", "bytes").body(resource);
    }

    // ===== PLAYLISTS =====
    @GetMapping("/playlists")
    public ResponseEntity<List<SignagePlaylistResponse>> getPlaylists(@RequestParam UUID branchId) {
        return ResponseEntity.ok(svc.getPlaylists(branchId));
    }

    @PostMapping("/playlists")
    public ResponseEntity<SignagePlaylistResponse> createPlaylist(@Valid @RequestBody SignagePlaylistRequest req,
            @RequestParam UUID branchId, @RequestParam UUID companyId) {
        return ResponseEntity.ok(svc.createPlaylist(req, branchId, companyId));
    }

    @PutMapping("/playlists/{id}")
    public ResponseEntity<SignagePlaylistResponse> updatePlaylist(@PathVariable UUID id, @RequestBody SignagePlaylistRequest req) {
        return ResponseEntity.ok(svc.updatePlaylist(id, req));
    }

    @DeleteMapping("/playlists/{id}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable UUID id) {
        svc.deletePlaylist(id); return ResponseEntity.noContent().build();
    }

    // ===== DEVICE API (called by Android TV app) =====
    @PostMapping("/device-api/pair")
    public ResponseEntity<SignageDeviceResponse> pairDevice(@Valid @RequestBody DevicePairRequest req) {
        return ResponseEntity.ok(svc.pairDevice(req));
    }

    @PostMapping("/device-api/heartbeat")
    public ResponseEntity<Void> deviceHeartbeat(@RequestBody DeviceHeartbeatRequest req) {
        svc.heartbeat(req); return ResponseEntity.ok().build();
    }

    @GetMapping("/device-api/sync/{deviceId}")
    public ResponseEntity<DeviceSyncResponse> syncDevice(@PathVariable String deviceId) {
        return ResponseEntity.ok(svc.getDeviceSync(deviceId));
    }
}

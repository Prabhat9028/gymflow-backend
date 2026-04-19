package com.gymflow.controller;

import com.gymflow.dto.Dtos.*;
import com.gymflow.service.SignageService;
import com.gymflow.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/signage") @RequiredArgsConstructor @Slf4j
public class SignageController {
    private final SignageService svc;
    private final StorageService storage;

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

        var result = storage.upload(file, contentType);
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";

        return ResponseEntity.ok(svc.createContent(
            name != null && !name.isBlank() ? name : originalName,
            contentType, originalName, result.fileUrl(), file.getContentType(),
            result.fileSize(), durationSeconds, result.checksum(), branchId, companyId));
    }

    @DeleteMapping("/content/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable UUID id) {
        svc.deleteContent(id); return ResponseEntity.noContent().build();
    }

    // Serve local media files
    @GetMapping("/media/{filename:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> serveMedia(@PathVariable String filename) {
        File file = new File("/app/uploads/signage", filename);
        if (!file.exists()) return ResponseEntity.notFound().build();
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);
        String fl = filename.toLowerCase();
        String mime = fl.endsWith(".mp4") ? "video/mp4" : fl.endsWith(".webm") ? "video/webm" :
            fl.endsWith(".jpg") || fl.endsWith(".jpeg") ? "image/jpeg" : fl.endsWith(".png") ? "image/png" : "application/octet-stream";
        return ResponseEntity.ok().header("Content-Type", mime).header("Accept-Ranges", "bytes")
            .header("Cache-Control", "public, max-age=86400").header("Access-Control-Allow-Origin", "*").body(resource);
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

    // ===== DEVICE API (called by Android TV app — NO AUTH) =====

    @PostMapping("/device-api/pair")
    public ResponseEntity<SignageDeviceResponse> pairDevice(@Valid @RequestBody DevicePairRequest req) {
        log.info("PAIR request: code={}, deviceId={}, model={}", req.getDeviceCode(), req.getDeviceId(), req.getDeviceModel());
        var result = svc.pairDevice(req);
        log.info("PAIR success: device={}, status={}", result.getDeviceName(), result.getStatus());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/device-api/heartbeat")
    public ResponseEntity<Void> deviceHeartbeat(@RequestBody DeviceHeartbeatRequest req) {
        log.debug("HEARTBEAT: deviceId={}", req.getDeviceId());
        svc.heartbeat(req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/device-api/sync")
    public ResponseEntity<DeviceSyncResponse> syncDevice(@RequestParam String deviceId) {
        log.info("SYNC request: deviceId={}", deviceId);
        var result = svc.getDeviceSync(deviceId);
        log.info("SYNC response: playlistId={}, items={}", result.getPlaylistId(), result.getItems() != null ? result.getItems().size() : 0);
        return ResponseEntity.ok(result);
    }
}

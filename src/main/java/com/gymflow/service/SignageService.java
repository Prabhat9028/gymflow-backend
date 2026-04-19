package com.gymflow.service;

import com.gymflow.dto.Dtos.*;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.IntStream;

@Service @RequiredArgsConstructor @Slf4j
public class SignageService {
    private final SignageDeviceRepository deviceRepo;
    private final SignageContentRepository contentRepo;
    private final SignagePlaylistRepository playlistRepo;
    private final PlaylistItemRepository itemRepo;
    private final BranchRepository branchRepo;
    private final CompanyRepository companyRepo;
    private final StorageService storageService;

    // ==================== DEVICES ====================

    public SignageDeviceResponse createDevice(SignageDeviceRequest req, UUID branchId, UUID companyId) {
        Branch branch = branchRepo.findById(branchId).orElseThrow(() -> new RuntimeException("Branch not found"));
        Company company = companyRepo.findById(companyId).orElseThrow(() -> new RuntimeException("Company not found"));
        String code = generatePairingCode();
        SignageDevice d = deviceRepo.save(SignageDevice.builder()
            .company(company).branch(branch).deviceCode(code)
            .deviceName(req.getDeviceName()).locationLabel(req.getLocationLabel())
            .status(SignageDevice.DeviceStatus.PENDING).isActive(true).build());
        return toDeviceResponse(d);
    }

    public List<SignageDeviceResponse> getDevices(UUID branchId) {
        return deviceRepo.findByBranchIdAndIsActiveTrue(branchId).stream().map(this::toDeviceResponse).toList();
    }

    public SignageDeviceResponse pairDevice(DevicePairRequest req) {
        SignageDevice d = deviceRepo.findByDeviceCode(req.getDeviceCode())
            .orElseThrow(() -> new RuntimeException("Invalid pairing code"));
        if (d.getStatus() != SignageDevice.DeviceStatus.PENDING)
            throw new RuntimeException("Device already paired");
        d.setDeviceId(req.getDeviceId());
        d.setDeviceModel(req.getDeviceModel());
        d.setScreenResolution(req.getScreenResolution());
        d.setAppVersion(req.getAppVersion());
        d.setStatus(SignageDevice.DeviceStatus.PAIRED);
        d.setLastHeartbeat(LocalDateTime.now());
        return toDeviceResponse(deviceRepo.save(d));
    }

    public void heartbeat(DeviceHeartbeatRequest req) {
        deviceRepo.findByDeviceId(req.getDeviceId()).ifPresent(d -> {
            d.setLastHeartbeat(LocalDateTime.now());
            d.setStatus(SignageDevice.DeviceStatus.ONLINE);
            if (req.getIpAddress() != null) d.setIpAddress(req.getIpAddress());
            if (req.getAppVersion() != null) d.setAppVersion(req.getAppVersion());
            deviceRepo.save(d);
        });
    }

    public SignageDeviceResponse assignPlaylist(UUID deviceId, UUID playlistId) {
        SignageDevice d = deviceRepo.findById(deviceId).orElseThrow(() -> new RuntimeException("Device not found"));
        SignagePlaylist p = playlistRepo.findById(playlistId).orElseThrow(() -> new RuntimeException("Playlist not found"));
        d.setActivePlaylist(p);
        return toDeviceResponse(deviceRepo.save(d));
    }

    public void deleteDevice(UUID id) {
        deviceRepo.findById(id).ifPresent(d -> { d.setIsActive(false); deviceRepo.save(d); });
    }

    // ==================== CONTENT ====================

    @Transactional
    public SignageContentResponse createContent(String name, String contentType, String fileName,
            String fileUrl, String mimeType, Long fileSize, Integer durationSeconds,
            String checksum, UUID branchId, UUID companyId) {
        Branch branch = branchRepo.findById(branchId).orElseThrow(() -> new RuntimeException("Branch not found"));
        Company company = companyRepo.findById(companyId).orElseThrow(() -> new RuntimeException("Company not found"));
        SignageContent c = contentRepo.save(SignageContent.builder()
            .company(company).branch(branch)
            .name(name).fileName(fileName).fileUrl(fileUrl).mimeType(mimeType)
            .contentType(SignageContent.ContentType.valueOf(contentType.toUpperCase()))
            .fileSize(fileSize).durationSeconds(durationSeconds)
            .checksum(checksum).isActive(true).build());
        log.info("Content created: {} ({}) - {}", c.getName(), c.getContentType(), c.getFileUrl());
        return toContentResponse(c);
    }

    public List<SignageContentResponse> getContent(UUID branchId) {
        return contentRepo.findByBranchIdAndIsActiveTrue(branchId).stream().map(this::toContentResponse).toList();
    }

    public void deleteContent(UUID id) {
        contentRepo.findById(id).ifPresent(c -> {
            c.setIsActive(false);
            contentRepo.save(c);
            storageService.delete(c.getFileUrl());
        });
    }

    // ==================== PLAYLISTS ====================

    @Transactional
    public SignagePlaylistResponse createPlaylist(SignagePlaylistRequest req, UUID branchId, UUID companyId) {
        Branch branch = branchRepo.findById(branchId).orElseThrow(() -> new RuntimeException("Branch not found"));
        Company company = companyRepo.findById(companyId).orElseThrow(() -> new RuntimeException("Company not found"));
        SignagePlaylist p = playlistRepo.save(SignagePlaylist.builder()
            .company(company).branch(branch)
            .name(req.getName()).description(req.getDescription())
            .mode(req.getMode() != null ? SignagePlaylist.PlaylistMode.valueOf(req.getMode()) : SignagePlaylist.PlaylistMode.SEQUENTIAL)
            .loopPlaylist(req.getLoopPlaylist() != null ? req.getLoopPlaylist() : true)
            .scheduleEnabled(req.getScheduleEnabled() != null ? req.getScheduleEnabled() : false)
            .scheduleStart(req.getScheduleStart() != null ? LocalTime.parse(req.getScheduleStart()) : null)
            .scheduleEnd(req.getScheduleEnd() != null ? LocalTime.parse(req.getScheduleEnd()) : null)
            .scheduleDays(req.getScheduleDays())
            .isActive(true).build());

        if (req.getItems() != null && !req.getItems().isEmpty()) savePlaylistItems(p, req.getItems());
        return toPlaylistResponse(p);
    }

    @Transactional
    public SignagePlaylistResponse updatePlaylist(UUID id, SignagePlaylistRequest req) {
        SignagePlaylist p = playlistRepo.findById(id).orElseThrow(() -> new RuntimeException("Playlist not found"));
        if (req.getName() != null) p.setName(req.getName());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getMode() != null) p.setMode(SignagePlaylist.PlaylistMode.valueOf(req.getMode()));
        if (req.getLoopPlaylist() != null) p.setLoopPlaylist(req.getLoopPlaylist());
        if (req.getScheduleEnabled() != null) p.setScheduleEnabled(req.getScheduleEnabled());
        if (req.getScheduleStart() != null) p.setScheduleStart(LocalTime.parse(req.getScheduleStart()));
        if (req.getScheduleEnd() != null) p.setScheduleEnd(LocalTime.parse(req.getScheduleEnd()));
        if (req.getScheduleDays() != null) p.setScheduleDays(req.getScheduleDays());
        playlistRepo.save(p);

        if (req.getItems() != null) {
            itemRepo.deleteByPlaylistId(p.getId());
            savePlaylistItems(p, req.getItems());
        }
        return toPlaylistResponse(p);
    }

    public List<SignagePlaylistResponse> getPlaylists(UUID branchId) {
        return playlistRepo.findByBranchIdAndIsActiveTrue(branchId).stream().map(this::toPlaylistResponse).toList();
    }

    public void deletePlaylist(UUID id) {
        playlistRepo.findById(id).ifPresent(p -> { p.setIsActive(false); playlistRepo.save(p); });
    }

    // ==================== DEVICE SYNC (Android TV API) ====================

    public DeviceSyncResponse getDeviceSync(String deviceId) {
        SignageDevice d = deviceRepo.findByDeviceId(deviceId)
            .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));
        d.setLastSync(LocalDateTime.now());
        d.setStatus(SignageDevice.DeviceStatus.ONLINE);
        deviceRepo.save(d);

        if (d.getActivePlaylist() == null) {
            log.info("Device {} has no playlist assigned", deviceId);
            return DeviceSyncResponse.builder()
                .serverTimestamp(System.currentTimeMillis()).items(List.of()).build();
        }

        SignagePlaylist p = d.getActivePlaylist();
        List<PlaylistItemResponse> items = itemRepo.findByPlaylistIdOrderBySortOrderAsc(p.getId())
            .stream().map(this::toItemResponse).toList();

        log.info("Sync for device {}: playlist={}, items={}", deviceId, p.getName(), items.size());
        return DeviceSyncResponse.builder()
            .playlistId(p.getId()).playlistName(p.getName())
            .mode(p.getMode().name()).loop(p.getLoopPlaylist())
            .serverTimestamp(System.currentTimeMillis()).items(items).build();
    }

    // ==================== HELPERS ====================

    private void savePlaylistItems(SignagePlaylist playlist, List<PlaylistItemInput> inputs) {
        IntStream.range(0, inputs.size()).forEach(i -> {
            PlaylistItemInput inp = inputs.get(i);
            SignageContent c = contentRepo.findById(inp.getContentId())
                .orElseThrow(() -> new RuntimeException("Content not found: " + inp.getContentId()));
            itemRepo.save(PlaylistItem.builder()
                .playlist(playlist).content(c).sortOrder(i)
                .displayDuration(inp.getDisplayDuration() != null ? inp.getDisplayDuration() : (c.getDurationSeconds() != null ? c.getDurationSeconds() : 10))
                .transitionType(inp.getTransitionType() != null ? inp.getTransitionType() : "fade")
                .build());
        });
    }

    private String generatePairingCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom rng = new SecureRandom();
        String code;
        do { code = IntStream.range(0, 6).mapToObj(i -> String.valueOf(chars.charAt(rng.nextInt(chars.length())))).reduce("", String::concat); }
        while (deviceRepo.findByDeviceCode(code).isPresent());
        return code;
    }

    private SignageDeviceResponse toDeviceResponse(SignageDevice d) {
        return SignageDeviceResponse.builder()
            .id(d.getId()).deviceCode(d.getDeviceCode()).deviceName(d.getDeviceName())
            .deviceId(d.getDeviceId()).deviceModel(d.getDeviceModel())
            .screenResolution(d.getScreenResolution()).locationLabel(d.getLocationLabel())
            .status(d.getStatus().name())
            .playlistId(d.getActivePlaylist() != null ? d.getActivePlaylist().getId() : null)
            .playlistName(d.getActivePlaylist() != null ? d.getActivePlaylist().getName() : null)
            .lastHeartbeat(d.getLastHeartbeat()).lastSync(d.getLastSync())
            .ipAddress(d.getIpAddress()).appVersion(d.getAppVersion()).isActive(d.getIsActive())
            .branchId(d.getBranch().getId()).branchName(d.getBranch().getName()).build();
    }

    private SignageContentResponse toContentResponse(SignageContent c) {
        return SignageContentResponse.builder()
            .id(c.getId()).name(c.getName()).fileName(c.getFileName()).fileUrl(c.getFileUrl())
            .thumbnailUrl(c.getThumbnailUrl()).contentType(c.getContentType().name())
            .mimeType(c.getMimeType()).fileSize(c.getFileSize()).durationSeconds(c.getDurationSeconds())
            .width(c.getWidth()).height(c.getHeight()).checksum(c.getChecksum())
            .isActive(c.getIsActive()).createdAt(c.getCreatedAt()).build();
    }

    private SignagePlaylistResponse toPlaylistResponse(SignagePlaylist p) {
        List<PlaylistItemResponse> items = itemRepo.findByPlaylistIdOrderBySortOrderAsc(p.getId())
            .stream().map(this::toItemResponse).toList();
        long deviceCount = deviceRepo.findByBranchIdAndIsActiveTrue(p.getBranch().getId()).stream()
            .filter(d -> d.getActivePlaylist() != null && d.getActivePlaylist().getId().equals(p.getId())).count();
        return SignagePlaylistResponse.builder()
            .id(p.getId()).name(p.getName()).description(p.getDescription())
            .mode(p.getMode().name()).loopPlaylist(p.getLoopPlaylist())
            .scheduleEnabled(p.getScheduleEnabled())
            .scheduleStart(p.getScheduleStart() != null ? p.getScheduleStart().toString() : null)
            .scheduleEnd(p.getScheduleEnd() != null ? p.getScheduleEnd().toString() : null)
            .scheduleDays(p.getScheduleDays()).isActive(p.getIsActive()).createdAt(p.getCreatedAt())
            .items(items).deviceCount((int) deviceCount).build();
    }

    private PlaylistItemResponse toItemResponse(PlaylistItem i) {
        SignageContent c = i.getContent();
        return PlaylistItemResponse.builder()
            .id(i.getId()).contentId(c.getId()).contentName(c.getName())
            .contentType(c.getContentType().name()).fileUrl(c.getFileUrl())
            .thumbnailUrl(c.getThumbnailUrl()).checksum(c.getChecksum())
            .fileSize(c.getFileSize()).durationSeconds(c.getDurationSeconds())
            .displayDuration(i.getDisplayDuration()).sortOrder(i.getSortOrder())
            .transitionType(i.getTransitionType()).build();
    }
}

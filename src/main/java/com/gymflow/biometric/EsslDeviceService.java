package com.gymflow.biometric;

import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ESSL/ZKTeco Device Communication Service
 *
 * Supports two modes:
 * 1. TCP Pull Mode — Backend connects to device IP:port and sends ZK protocol commands
 * 2. ADMS Push Mode — Device pushes attendance logs to /api/iclock endpoints (handled by EsslAdmsController)
 *
 * ZKTeco TCP Protocol Commands:
 * - CMD_CONNECT (1000): Connect to device
 * - CMD_EXIT (1001): Disconnect
 * - CMD_ENROLL_FP (1009): Start fingerprint enrollment for a user
 * - CMD_USER_WRQ (8): Write user info to device
 * - CMD_ATTLOG_RRQ (13): Read attendance logs
 * - CMD_CLEAR_ATTLOG (15): Clear attendance logs after reading
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EsslDeviceService {

    private final BiometricDeviceRepository deviceRepo;
    private final MemberRepository memberRepo;
    private final AttendanceRepository attendanceRepo;
    private final BranchRepository branchRepo;
    private final SimpMessagingTemplate wsTemplate;

    private static final int CMD_CONNECT = 1000;
    private static final int CMD_EXIT = 1001;
    private static final int CMD_USER_WRQ = 8;
    private static final int CMD_ENROLL_FP = 1009;
    private static final int CMD_ATTLOG_RRQ = 13;
    private static final int CMD_CLEAR_ATTLOG = 15;
    private static final int CMD_ACK_OK = 2000;

    /**
     * Step 1: Register user on device (prerequisite for fingerprint enrollment)
     * Sends CMD_USER_WRQ with user ID = member_code numeric part
     */
    @Transactional
    public Map<String, Object> registerUserOnDevice(UUID memberId, String deviceSerial) {
        Member member = memberRepo.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        BiometricDevice device = deviceRepo.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceSerial));

        // Generate numeric device user ID from member code (e.g., GF000123 -> 123)
        String deviceUserId = member.getMemberCode().replaceAll("[^0-9]", "");
        if (deviceUserId.isEmpty()) deviceUserId = String.valueOf(System.currentTimeMillis() % 100000);

        try {
            // Connect to device and register user
            try (Socket socket = new Socket(device.getDeviceIp(), device.getDevicePort())) {
                socket.setSoTimeout(10000);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                // CMD_CONNECT
                sendCommand(out, CMD_CONNECT, new byte[0]);
                byte[] connResp = readResponse(in);
                if (!isAck(connResp)) throw new RuntimeException("Device connection failed");

                // CMD_USER_WRQ — write user info
                String userData = String.format("PIN=%s\tName=%s %s\tPri=0\tPasswd=\tCard=\tGrp=1\tTZ=0000000100000000\n",
                        deviceUserId, member.getFirstName(), member.getLastName());
                sendCommand(out, CMD_USER_WRQ, userData.getBytes());
                byte[] userResp = readResponse(in);

                if (isAck(userResp)) {
                    member.setDeviceUserId(deviceUserId);
                    memberRepo.save(member);

                    log.info("User {} registered on device {} as ID {}", member.getMemberCode(), deviceSerial, deviceUserId);
                    return Map.of("success", true, "deviceUserId", deviceUserId, "message", "User registered on device");
                } else {
                    throw new RuntimeException("Device rejected user registration");
                }
            }
        } catch (IOException e) {
            log.error("Device communication error: {}", e.getMessage());
            // Fallback: register locally even if device unreachable
            member.setDeviceUserId(deviceUserId);
            memberRepo.save(member);
            return Map.of("success", true, "deviceUserId", deviceUserId,
                    "message", "User registered locally. Device offline — sync when device is online.",
                    "deviceOffline", true);
        }
    }

    /**
     * Step 2: Initiate fingerprint enrollment on device
     * Device screen shows "Place Finger" and user scans on hardware
     */
    @Transactional
    public Map<String, Object> enrollFingerprint(UUID memberId, String deviceSerial) {
        Member member = memberRepo.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (member.getDeviceUserId() == null) {
            // Auto-register first
            registerUserOnDevice(memberId, deviceSerial);
            member = memberRepo.findById(memberId).orElseThrow();
        }

        BiometricDevice device = deviceRepo.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceSerial));

        try {
            try (Socket socket = new Socket(device.getDeviceIp(), device.getDevicePort())) {
                socket.setSoTimeout(30000); // 30s timeout for enrollment
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                // Connect
                sendCommand(out, CMD_CONNECT, new byte[0]);
                readResponse(in);

                // CMD_ENROLL_FP with user ID and finger index 0
                String enrollData = String.format("%s\t0", member.getDeviceUserId());
                sendCommand(out, CMD_ENROLL_FP, enrollData.getBytes());

                // Wait for enrollment response (device will show "Place Finger")
                log.info("Enrollment started for {} on device {}. Waiting for finger scan...", member.getMemberCode(), deviceSerial);

                byte[] enrollResp = readResponse(in);

                if (isAck(enrollResp)) {
                    member.setBiometricEnrolled(true);
                    memberRepo.save(member);

                    // Notify via WebSocket
                    wsTemplate.convertAndSend("/topic/biometric",
                            Map.of("event", "ENROLLMENT_SUCCESS", "memberId", memberId,
                                    "memberName", member.getFirstName() + " " + member.getLastName()));

                    log.info("Fingerprint enrolled for member {}", member.getMemberCode());
                    return Map.of("success", true, "message", "Fingerprint enrolled successfully",
                            "memberId", memberId, "memberName", member.getFirstName() + " " + member.getLastName());
                } else {
                    return Map.of("success", false, "message", "Enrollment failed or timed out. Please try again.");
                }
            }
        } catch (IOException e) {
            log.error("Enrollment error: {}", e.getMessage());
            return Map.of("success", false, "message", "Cannot reach device: " + e.getMessage(),
                    "hint", "Ensure device is powered on and IP " + device.getDeviceIp() + ":" + device.getDevicePort() + " is reachable");
        }
    }

    /**
     * Pull attendance logs from device (TCP pull mode)
     * Called periodically or on-demand
     */
    @Transactional
    public Map<String, Object> pullAttendanceLogs(String deviceSerial) {
        BiometricDevice device = deviceRepo.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        int processed = 0;
        try {
            try (Socket socket = new Socket(device.getDeviceIp(), device.getDevicePort())) {
                socket.setSoTimeout(15000);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                sendCommand(out, CMD_CONNECT, new byte[0]);
                readResponse(in);

                sendCommand(out, CMD_ATTLOG_RRQ, new byte[0]);
                byte[] logResp = readResponse(in);

                String logData = new String(logResp);
                String[] lines = logData.split("\n");

                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    try {
                        processed += processAttendanceLine(line.trim(), device);
                    } catch (Exception e) {
                        log.warn("Skipping log line: {} — {}", line, e.getMessage());
                    }
                }

                // Clear logs after reading
                if (processed > 0) {
                    sendCommand(out, CMD_CLEAR_ATTLOG, new byte[0]);
                    readResponse(in);
                }

                sendCommand(out, CMD_EXIT, new byte[0]);
            }

            device.setLastHeartbeat(LocalDateTime.now());
            deviceRepo.save(device);

            return Map.of("success", true, "processed", processed, "device", deviceSerial);
        } catch (IOException e) {
            log.error("Pull failed from {}: {}", deviceSerial, e.getMessage());
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Process a single attendance log line from device
     * Format: PIN\tTimestamp\tStatus\tVerify
     * Example: 123\t2024-01-15 09:30:00\t0\t1
     */
    private int processAttendanceLine(String line, BiometricDevice device) {
        String[] parts = line.split("\t");
        if (parts.length < 2) return 0;

        String deviceUserId = parts[0].trim();
        String timestamp = parts[1].trim();

        Member member = memberRepo.findByDeviceUserId(deviceUserId).orElse(null);
        if (member == null) {
            log.warn("No member mapped for device user ID: {}", deviceUserId);
            return 0;
        }

        LocalDateTime checkTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Check for duplicate
        var existing = attendanceRepo.findByBranchBetween(device.getBranch().getId(),
                checkTime.minusMinutes(1), checkTime.plusMinutes(1));
        boolean isDuplicate = existing.stream().anyMatch(a -> a.getMember().getId().equals(member.getId()));
        if (isDuplicate) return 0;

        // Check if this is check-in or check-out
        var openCheckIn = attendanceRepo.findOpenCheckIn(member.getId());
        if (openCheckIn.isPresent()) {
            openCheckIn.get().setCheckOutTime(checkTime);
            attendanceRepo.save(openCheckIn.get());
        } else {
            Attendance att = Attendance.builder()
                    .member(member).branch(device.getBranch())
                    .checkInTime(checkTime).verificationMethod("BIOMETRIC")
                    .deviceId(device.getDeviceSerial())
                    .deviceLogId(line).build();
            attendanceRepo.save(att);
        }

        // Real-time WebSocket notification
        wsTemplate.convertAndSend("/topic/attendance",
                Map.of("event", "CHECK_IN", "memberId", member.getId(),
                        "memberName", member.getFirstName() + " " + member.getLastName(),
                        "memberCode", member.getMemberCode(), "time", checkTime.toString(),
                        "branchId", device.getBranch().getId()));

        return 1;
    }

    // ===== ZKTeco TCP Protocol helpers =====

    private void sendCommand(OutputStream out, int command, byte[] data) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) command);
        buf.putShort((short) 0); // checksum
        buf.putShort((short) 0); // session
        buf.putShort((short) 0); // reply
        buf.put(data);
        out.write(buf.array());
        out.flush();
    }

    private byte[] readResponse(InputStream in) throws IOException {
        byte[] header = new byte[8];
        int read = in.read(header);
        if (read < 8) return new byte[0];

        ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int cmd = buf.getShort() & 0xFFFF;
        int checksum = buf.getShort() & 0xFFFF;
        int session = buf.getShort() & 0xFFFF;
        int replyId = buf.getShort() & 0xFFFF;

        // Read remaining data if available
        byte[] data = new byte[0];
        if (in.available() > 0) {
            data = in.readNBytes(Math.min(in.available(), 65536));
        }

        ByteBuffer resp = ByteBuffer.allocate(8 + data.length);
        resp.put(header);
        resp.put(data);
        return resp.array();
    }

    private boolean isAck(byte[] response) {
        if (response.length < 2) return false;
        int cmd = ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        return cmd == CMD_ACK_OK;
    }
}

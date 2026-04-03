package com.gymflow.biometric;

import com.gymflow.entity.*;
import com.gymflow.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * ESSL ADMS (Automatic Data Master Server) Push Protocol Handler
 *
 * ESSL/ZKTeco devices configured with "Push Data to Server" will call:
 * 1. GET /api/iclock/cdata?SN=xxx — Handshake (device sends serial number)
 * 2. POST /api/iclock/cdata?SN=xxx&table=ATTLOG — Push attendance logs
 * 3. GET /api/iclock/getrequest?SN=xxx — Device polls for pending commands
 *
 * Device Configuration:
 *   Push Server: http://<your-server-ip>:8080/api/iclock
 *   Push Mode: Enable
 */
@RestController
@RequestMapping("/api/iclock")
@RequiredArgsConstructor
@Slf4j
public class EsslAdmsController {

    private final BiometricDeviceRepository deviceRepo;
    private final MemberRepository memberRepo;
    private final AttendanceRepository attendanceRepo;
    private final SimpMessagingTemplate wsTemplate;

    /**
     * Handshake — device registers itself
     * GET /api/iclock/cdata?SN=ESSL001
     */
    @GetMapping("/cdata")
    public String handshake(@RequestParam("SN") String serialNumber) {
        log.info("ADMS Handshake from device: {}", serialNumber);

        deviceRepo.findByDeviceSerial(serialNumber).ifPresent(device -> {
            device.setLastHeartbeat(LocalDateTime.now());
            deviceRepo.save(device);
        });

        // Response tells device: send your records
        return "GET OPTION FROM:" + serialNumber + "\n"
             + "Stamp=0\n"
             + "OpStamp=0\n"
             + "ErrorDelay=60\n"
             + "Delay=30\n"
             + "TransTimes=00:00;23:59\n"
             + "TransInterval=1\n"
             + "TransFlag=1111000000\n"
             + "Realtime=1\n"
             + "ATTLOGStamp=0\n";
    }

    /**
     * Receive attendance logs pushed from device
     * POST /api/iclock/cdata?SN=ESSL001&table=ATTLOG&Stamp=...
     *
     * Body format (one record per line):
     * PIN\tDatetime\tStatus\tVerify\tWorkCode\tReserved
     * Example: 123\t2024-01-15 09:30:00\t0\t1\t0\t0
     */
    @PostMapping("/cdata")
    @Transactional
    public String receiveAttendance(
            @RequestParam("SN") String serialNumber,
            @RequestParam(value = "table", defaultValue = "ATTLOG") String table,
            @RequestBody String body) {

        log.info("ADMS Push from {} table={}: {}", serialNumber, table, body.substring(0, Math.min(200, body.length())));

        if (!"ATTLOG".equalsIgnoreCase(table)) {
            return "OK";
        }

        BiometricDevice device = deviceRepo.findByDeviceSerial(serialNumber).orElse(null);
        if (device == null) {
            log.warn("Unknown device: {}", serialNumber);
            return "OK";
        }

        device.setLastHeartbeat(LocalDateTime.now());
        deviceRepo.save(device);

        int processed = 0;
        String[] lines = body.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            try {
                String[] parts = line.split("\t");
                if (parts.length < 2) continue;

                String deviceUserId = parts[0].trim();
                String timestamp = parts[1].trim();

                Member member = memberRepo.findByDeviceUserId(deviceUserId).orElse(null);
                if (member == null) {
                    log.warn("ADMS: No member for device user ID: {}", deviceUserId);
                    continue;
                }

                LocalDateTime checkTime;
                try {
                    checkTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (Exception e) {
                    checkTime = LocalDateTime.now();
                }

                // Dedup: skip if same member checked in within last minute
                var recent = attendanceRepo.findByBranchBetween(device.getBranch().getId(),
                        checkTime.minusMinutes(1), checkTime.plusMinutes(1));
                boolean isDup = recent.stream().anyMatch(a -> a.getMember().getId().equals(member.getId()));
                if (isDup) continue;

                // Toggle: check-in if no open session, check-out if open session exists
                var open = attendanceRepo.findOpenCheckIn(member.getId());
                if (open.isPresent()) {
                    open.get().setCheckOutTime(checkTime);
                    attendanceRepo.save(open.get());
                    log.info("ADMS CHECK-OUT: {} at {}", member.getMemberCode(), checkTime);
                } else {
                    Attendance att = Attendance.builder()
                            .member(member).branch(device.getBranch())
                            .checkInTime(checkTime).verificationMethod("BIOMETRIC")
                            .deviceId(serialNumber).deviceLogId(line).build();
                    attendanceRepo.save(att);
                    log.info("ADMS CHECK-IN: {} at {}", member.getMemberCode(), checkTime);
                }

                // Real-time WebSocket push
                wsTemplate.convertAndSend("/topic/attendance",
                        Map.of("event", open.isPresent() ? "CHECK_OUT" : "CHECK_IN",
                                "memberId", member.getId(),
                                "memberName", member.getFirstName() + " " + member.getLastName(),
                                "memberCode", member.getMemberCode(),
                                "time", checkTime.toString(),
                                "branchId", device.getBranch().getId()));

                processed++;
            } catch (Exception e) {
                log.warn("ADMS parse error: {} — {}", line, e.getMessage());
            }
        }

        log.info("ADMS processed {} records from {}", processed, serialNumber);
        return "OK:" + processed;
    }

    /**
     * Device polls for pending commands (enrollment, user sync, etc.)
     * GET /api/iclock/getrequest?SN=ESSL001
     */
    @GetMapping("/getrequest")
    public String getRequest(@RequestParam("SN") String serialNumber) {
        // Return empty if no pending commands
        // In production, you'd queue enrollment commands here
        return "OK";
    }

    /**
     * Device sends operation results
     * POST /api/iclock/devicecmd?SN=xxx
     */
    @PostMapping("/devicecmd")
    public String deviceCmd(@RequestParam("SN") String serialNumber, @RequestBody String body) {
        log.info("Device command result from {}: {}", serialNumber, body);
        return "OK";
    }
}

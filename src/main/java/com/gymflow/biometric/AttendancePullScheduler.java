package com.gymflow.biometric;

import com.gymflow.repository.BiometricDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttendancePullScheduler {

    private final BiometricDeviceRepository deviceRepo;
    private final EsslDeviceService deviceService;

    /**
     * Pull attendance logs from all active devices every 2 minutes
     * This is the fallback mode when ADMS push is not configured
     */
    @Scheduled(fixedDelay = 120000, initialDelay = 30000)
    public void pullFromAllDevices() {
        var devices = deviceRepo.findAll().stream()
                .filter(d -> d.getIsActive() && d.getDeviceIp() != null)
                .toList();

        for (var device : devices) {
            try {
                var result = deviceService.pullAttendanceLogs(device.getDeviceSerial());
                log.debug("Pull from {}: {}", device.getDeviceSerial(), result);
            } catch (Exception e) {
                log.debug("Pull failed from {}: {}", device.getDeviceSerial(), e.getMessage());
            }
        }
    }
}

package com.gymflow.config;
import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Component @RequiredArgsConstructor @Slf4j
public class DataLoader implements CommandLineRunner {
    private final CompanyRepository companyRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;
    private final StaffRepository staffRepo;
    private final MembershipPlanRepository planRepo;
    private final BiometricDeviceRepository deviceRepo;
    private final PasswordEncoder encoder;

    @Override @Transactional
    public void run(String... args) {
        if (companyRepo.count() > 0) { log.info("Data exists, skip seeding."); return; }
        log.info("Seeding data...");
        String pw = encoder.encode("admin123");

        Company co = companyRepo.save(Company.builder().name("MaxOut Gym").code("MAXOUT")
            .email("info@maxoutgym.com").phone("+91 9876543210").address("Mumbai, Maharashtra").isActive(true).build());

        Branch b1 = branchRepo.save(Branch.builder().company(co).name("MaxOut Andheri").code("MAXOUT-AND")
            .address("Andheri West, Mumbai").city("Mumbai").phone("+91 9876543211").email("andheri@maxoutgym.com").isActive(true).build());
        Branch b2 = branchRepo.save(Branch.builder().company(co).name("MaxOut Bandra").code("MAXOUT-BAN")
            .address("Bandra West, Mumbai").city("Mumbai").phone("+91 9876543212").email("bandra@maxoutgym.com").isActive(true).build());
        Branch b3 = branchRepo.save(Branch.builder().company(co).name("MaxOut Powai").code("MAXOUT-POW")
            .address("Hiranandani, Powai").city("Mumbai").phone("+91 9876543213").email("powai@maxoutgym.com").isActive(true).build());
        Branch b4 = branchRepo.save(Branch.builder().company(co).name("MaxOut Borivali").code("MAXOUT-BVI")
            .address("Borivali West, Mumbai").city("Mumbai").phone("+91 9876543214").email("borivali@maxoutgym.com").isActive(true).build());

        User sa = userRepo.save(User.builder().company(co).email("admin@maxoutgym.com").passwordHash(pw).role(User.UserRole.SUPER_ADMIN).isActive(true).build());
        User u1 = userRepo.save(User.builder().company(co).branch(b1).email("rahul@maxoutgym.com").passwordHash(pw).role(User.UserRole.STAFF).isActive(true).build());
        User u2 = userRepo.save(User.builder().company(co).branch(b2).email("priya@maxoutgym.com").passwordHash(pw).role(User.UserRole.ADMIN).isActive(true).build());
        User u3 = userRepo.save(User.builder().company(co).branch(b3).email("amit@maxoutgym.com").passwordHash(pw).role(User.UserRole.ADMIN).isActive(true).build());

        staffRepo.save(Staff.builder().user(u1).company(co).branch(b1).staffCode("STF001").firstName("Rahul").lastName("Sharma")
            .email("rahul@maxoutgym.com").phone("9876543210").role("STAFF").department("Operations").designation("Front Desk")
            .salary(new BigDecimal("25000")).shiftStart(LocalTime.of(6,0)).shiftEnd(LocalTime.of(14,0)).isActive(true).build());
        staffRepo.save(Staff.builder().user(u2).company(co).branch(b2).staffCode("STF002").firstName("Priya").lastName("Patel")
            .email("priya@maxoutgym.com").phone("9876543211").role("ADMIN").department("Management").designation("Branch Manager")
            .salary(new BigDecimal("35000")).shiftStart(LocalTime.of(8,0)).shiftEnd(LocalTime.of(17,0)).isActive(true).build());
        staffRepo.save(Staff.builder().user(u3).company(co).branch(b3).staffCode("STF003").firstName("Amit").lastName("Kumar")
            .email("amit@maxoutgym.com").phone("9876543212").role("ADMIN").department("Management").designation("Branch Manager")
            .salary(new BigDecimal("40000")).shiftStart(LocalTime.of(8,0)).shiftEnd(LocalTime.of(17,0)).isActive(true).build());

        planRepo.saveAll(List.of(
            MembershipPlan.builder().company(co).branch(b1).name("Basic Monthly").description("Gym floor").durationDays(30).price(new BigDecimal("1999")).features(List.of("Gym Floor","Locker Room")).isActive(true).build(),
            MembershipPlan.builder().company(co).branch(b1).name("Premium Monthly").description("Full access").durationDays(30).price(new BigDecimal("3499")).features(List.of("Full Access","Classes","Sauna")).isActive(true).build(),
            MembershipPlan.builder().company(co).branch(b1).name("Annual Premium").description("Best value").durationDays(365).price(new BigDecimal("29999")).features(List.of("Full Access","Unlimited Classes","PT","Sauna")).isActive(true).build(),
            MembershipPlan.builder().company(co).branch(b2).name("Basic Monthly").description("Gym floor").durationDays(30).price(new BigDecimal("2199")).features(List.of("Gym Floor","Locker Room")).isActive(true).build(),
            MembershipPlan.builder().company(co).branch(b2).name("Premium Monthly").description("Full access").durationDays(30).price(new BigDecimal("3999")).features(List.of("Full Access","Classes","Sauna","Steam")).isActive(true).build(),
            MembershipPlan.builder().company(co).branch(b3).name("Basic Monthly").description("Gym floor").durationDays(30).price(new BigDecimal("1799")).features(List.of("Gym Floor","Locker Room")).isActive(true).build(),
            MembershipPlan.builder().company(co).branch(b3).name("Premium Monthly").description("Full access").durationDays(30).price(new BigDecimal("3299")).features(List.of("Full Access","Classes","Sauna")).isActive(true).build(),
            // Borivali plans matching the Excel data
            MembershipPlan.builder().company(co).branch(b4).name("Monthly").description("1 month gym membership").durationDays(30).price(new BigDecimal("4500")).features(List.of("Gym Floor","Locker Room")).isActive(true).build(),
            MembershipPlan.builder().company(co).branch(b4).name("Quarterly").description("3 months gym membership").durationDays(90).price(new BigDecimal("7000")).features(List.of("Gym Floor","Locker Room","Classes")).isActive(true).build(),
            MembershipPlan.builder().company(co).branch(b4).name("Half-Yearly").description("6 months gym membership").durationDays(180).price(new BigDecimal("10000")).features(List.of("Gym Floor","Classes","Locker Room")).isActive(true).build(),
            MembershipPlan.builder().company(co).branch(b4).name("Annual").description("12 months gym membership").durationDays(365).price(new BigDecimal("15000")).features(List.of("Full Access","Classes","Locker Room","Sauna")).isActive(true).build()));

        User u4 = userRepo.save(User.builder().company(co).branch(b4).email("prabhat@maxoutgym.com").passwordHash(pw).role(User.UserRole.ADMIN).isActive(true).build());
        staffRepo.save(Staff.builder().user(u4).company(co).branch(b4).staffCode("STF004").firstName("Prabhat").lastName("Singh")
            .email("prabhat@maxoutgym.com").phone("9876543214").role("ADMIN").department("Management").designation("Branch Manager")
            .salary(new BigDecimal("40000")).shiftStart(LocalTime.of(8,0)).shiftEnd(LocalTime.of(17,0)).isActive(true).build());

        // Biometric devices — update IPs to match your actual ESSL/ZKTeco hardware
        deviceRepo.save(BiometricDevice.builder().company(co).branch(b1)
            .deviceSerial("ESSL001").deviceName("ESSL Biometric - Andheri")
            .deviceIp("192.168.0.100").devicePort(4370).deviceType("ESSL_ZK")
            .isActive(true).build());
        deviceRepo.save(BiometricDevice.builder().company(co).branch(b2)
            .deviceSerial("ESSL002").deviceName("ESSL Biometric - Bandra")
            .deviceIp("192.168.0.101").devicePort(4370).deviceType("ESSL_ZK")
            .isActive(true).build());

        log.info("Seeded: 1 company, 3 branches, 4 users, 3 staff, 7 plans, 2 devices");
    }
}

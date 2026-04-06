package com.gymflow.controller;

import com.gymflow.entity.*;
import com.gymflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
public class ImportController {

    private final MemberRepository memberRepo;
    private final BranchRepository branchRepo;
    private final CompanyRepository companyRepo;
    private final MembershipPlanRepository planRepo;
    private final SubscriptionRepository subRepo;
    private final PaymentRepository payRepo;

    // Plan prices for matching
    private static final Map<Integer, String> PLAN_MAP = Map.of(
        365, "Annual", 180, "Half-Yearly", 90, "Quarterly", 30, "Monthly"
    );
    private static final Map<String, Integer> DURATION_KEYWORDS = new LinkedHashMap<>() {{
        put("12 MONTH", 365); put("YEARLY", 365); put("ANNUAL", 365);
        put("6 MONTH", 180); put("HALF", 180);
        put("3 MONTH", 90); put("QUARTER", 90); put("QUATERLY", 90);
        put("1 MONTH", 30); put("ONE MONTH", 30); put("MONTHLY", 30);
    }};
    private static final Map<Integer, BigDecimal> PLAN_PRICES = Map.of(
        365, new BigDecimal("15000"), 180, new BigDecimal("10000"),
        90, new BigDecimal("7000"), 30, new BigDecimal("4500")
    );

    @PostMapping("/members")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importMembers(
            @RequestParam("file") MultipartFile file,
            @RequestParam UUID branchId) {

        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        int imported = 0, skipped = 0, errors = 0;
        List<String> errorMessages = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                Sheet sheet = wb.getSheetAt(si);
                String sheetName = sheet.getSheetName();
                log.info("Processing sheet: {} ({} rows)", sheetName, sheet.getLastRowNum());

                // Parse header to get column indices
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;
                Map<String, Integer> cols = new HashMap<>();
                for (int c = 0; c <= headerRow.getLastCellNum(); c++) {
                    Cell cell = headerRow.getCell(c);
                    if (cell != null) {
                        String h = cellStr(cell).trim().toUpperCase();
                        cols.put(h, c);
                    }
                }

                // Map flexible column names
                int colGender = findCol(cols, "F/M");
                int colDate = findCol(cols, "DATE");
                int colSource = findCol(cols, "SOURCE");
                int colCounsellor = findCol(cols, "COUNCELLER", "COUNSELLOR");
                int colName = findCol(cols, "NAME");
                int colContact = findCol(cols, "CONTACT");
                int colSales = findCol(cols, "SALES");
                int colTotal = findCol(cols, "TOTAL");
                int colBal = findCol(cols, "BAL");
                int colRemarks = findCol(cols, "REMARKS", "GYM/PT");
                int colMop = findCol(cols, "MOP");

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    try {
                        String name = colName >= 0 ? cellStr(row.getCell(colName)).trim() : "";
                        String contact = colContact >= 0 ? cellStr(row.getCell(colContact)).trim() : "";

                        if (name.isEmpty() || contact.isEmpty()) { skipped++; continue; }

                        // Skip balance payment rows (we handle them differently)
                        String source = colSource >= 0 ? cellStr(row.getCell(colSource)).trim() : "";
                        String remarks = colRemarks >= 0 ? cellStr(row.getCell(colRemarks)).trim() : "";
                        if (source.toUpperCase().contains("BALANCE") || remarks.toUpperCase().contains("BALANCE CLEAR") || remarks.toUpperCase().contains("BALANC CLEAR")) {
                            skipped++;
                            continue;
                        }

                        // Skip one-day workout or trial
                        if (remarks.toUpperCase().contains("ONE DAY") || remarks.toUpperCase().contains("TRIAL")) {
                            skipped++;
                            continue;
                        }

                        // Handle multiple members (slash separated: "NISHAL/ MILI")
                        String[] names = name.contains("/") ? name.split("/") : new String[]{name};
                        String[] contacts = contact.contains("/") ? contact.split("/") : new String[]{contact};

                        String gender = colGender >= 0 ? cellStr(row.getCell(colGender)).trim() : "";
                        LocalDate enrollDate = colDate >= 0 ? parseDate(row.getCell(colDate)) : LocalDate.now();
                        String counsellor = colCounsellor >= 0 ? cellStr(row.getCell(colCounsellor)).trim() : "";
                        String mop = colMop >= 0 ? cellStr(row.getCell(colMop)).trim().toUpperCase() : "CASH";

                        // Parse amounts — handle JAN where SALES=plan price, TOTAL=amount paid
                        // vs FEB+ where TOTAL=plan price?, SALES=amount paid
                        double salesVal = colSales >= 0 ? cellNum(row.getCell(colSales)) : 0;
                        double totalVal = colTotal >= 0 ? cellNum(row.getCell(colTotal)) : 0;
                        String balStr = colBal >= 0 ? cellStr(row.getCell(colBal)).trim() : "0";
                        double balVal = 0;
                        try { balVal = Double.parseDouble(balStr.replaceAll("[^0-9.]", "")); } catch (Exception ignored) {}

                        // In JAN: SALES=price_paid, TOTAL=price_paid (same)
                        // In FEB+: TOTAL=total_amount, SALES=amount_paid
                        // Actually from data: both seem to be the amount paid (same values)
                        double amountPaid = Math.max(salesVal, totalVal);
                        if (totalVal > 0 && salesVal > 0 && totalVal != salesVal) {
                            amountPaid = Math.max(totalVal, salesVal); // take the larger as total paid
                        }

                        // Determine plan from REMARKS
                        int durationDays = detectDuration(remarks);
                        BigDecimal planPrice = PLAN_PRICES.getOrDefault(durationDays, new BigDecimal("15000"));

                        // Check if it's PT (personal training) — skip plan matching
                        boolean isPT = remarks.toUpperCase().contains("PT") && !remarks.toUpperCase().contains("MSHIP");

                        // For each name in slash-separated list
                        double perPersonAmount = amountPaid / names.length;
                        double perPersonBal = balVal / names.length;

                        for (int ni = 0; ni < names.length; ni++) {
                            String memberName = names[ni].trim();
                            String memberContact = ni < contacts.length ? contacts[ni].trim().replaceAll("[^0-9]", "") : "";

                            if (memberName.isEmpty()) continue;
                            if (memberContact.length() > 10) memberContact = memberContact.substring(0, 10);

                            // Check duplicate by phone (unique identifier per branch)
                            if (!memberContact.isEmpty() && memberRepo.findByPhoneAndBranchId(memberContact, branchId).isPresent()) {
                                skipped++;
                                continue;
                            }

                            // Parse gender
                            String genderStr = gender.toUpperCase();
                            String[] genders = genderStr.contains("/") ? genderStr.split("/") : new String[]{genderStr};
                            String thisGender = ni < genders.length ? genders[ni].trim() : genders[0].trim();
                            Member.Gender memberGender = "F".equals(thisGender) ? Member.Gender.FEMALE : Member.Gender.MALE;

                            // Split name into first/last
                            String[] nameParts = memberName.split("\\s+", 2);
                            String firstName = nameParts[0];
                            String lastName = nameParts.length > 1 ? nameParts[1] : "";

                            // Create member
                            Member member = Member.builder()
                                    .memberCode(genCode())
                                    .firstName(firstName).lastName(lastName)
                                    .phone(memberContact).gender(memberGender)
                                    .source(source.isEmpty() ? "WALKIN" : source)
                                    .counsellor(counsellor)
                                    .notes(remarks)
                                    .company(branch.getCompany()).branch(branch)
                                    .joinDate(enrollDate != null ? enrollDate : LocalDate.now())
                                    .isActive(true).biometricEnrolled(false)
                                    .build();
                            member = memberRepo.save(member);

                            // Create subscription if not purely PT
                            if (!isPT && durationDays > 0) {
                                MembershipPlan plan = planRepo.findByBranchIdAndIsActiveTrue(branchId).stream()
                                        .filter(p -> p.getDurationDays().equals(durationDays))
                                        .findFirst().orElse(null);

                                if (plan != null) {
                                    LocalDate startDate = enrollDate != null ? enrollDate : LocalDate.now();
                                    BigDecimal paid = BigDecimal.valueOf(perPersonAmount);
                                    BigDecimal discount = planPrice.subtract(paid.add(BigDecimal.valueOf(perPersonBal)));
                                    if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO;
                                    BigDecimal balance = BigDecimal.valueOf(perPersonBal);

                                    Subscription sub = subRepo.save(Subscription.builder()
                                            .member(member).plan(plan).branch(branch)
                                            .subType(Subscription.SubType.MEMBERSHIP)
                                            .startDate(startDate).endDate(startDate.plusDays(durationDays))
                                            .status(Subscription.MembershipStatus.ACTIVE)
                                            .amountPaid(paid).build());

                                    Payment.PaymentStatus payStatus = balance.compareTo(BigDecimal.ZERO) > 0
                                            ? Payment.PaymentStatus.PENDING : Payment.PaymentStatus.PAID;

                                    payRepo.save(Payment.builder()
                                            .member(member).subscription(sub).branch(branch)
                                            .amount(planPrice.subtract(discount))
                                            .discountAmount(discount).amountPaid(paid)
                                            .balanceAmount(balance)
                                            .paymentMethod(normalizePaymentMethod(mop))
                                            .status(payStatus)
                                            .transactionRef("IMP" + System.currentTimeMillis())
                                            .paymentDate(startDate.atStartOfDay())
                                            .build());
                                }
                            }

                            imported++;
                        }
                    } catch (Exception e) {
                        errors++;
                        errorMessages.add("Sheet " + sheetName + " Row " + (r+1) + ": " + e.getMessage());
                        log.warn("Import error sheet={} row={}: {}", sheetName, r+1, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }

        log.info("Import complete: {} imported, {} skipped, {} errors", imported, skipped, errors);
        return ResponseEntity.ok(Map.of(
                "imported", imported, "skipped", skipped, "errors", errors,
                "errorMessages", errorMessages.subList(0, Math.min(20, errorMessages.size())),
                "branchId", branchId
        ));
    }

    // ===== HELPERS =====

    private int detectDuration(String remarks) {
        if (remarks == null) return 365;
        String upper = remarks.toUpperCase();
        for (var entry : DURATION_KEYWORDS.entrySet()) {
            if (upper.contains(entry.getKey())) return entry.getValue();
        }
        return 365; // default annual
    }

    private String normalizePaymentMethod(String mop) {
        if (mop == null) return "CASH";
        String u = mop.toUpperCase().trim();
        if (u.contains("UPI") || u.contains("GOOGLE") || u.contains("PHONE")) return "UPI";
        if (u.contains("CARD") || u.contains("DEBIT") || u.contains("CREDIT")) return "CARD";
        if (u.contains("BANK") || u.contains("NEFT") || u.contains("TRANSFER")) return "BANK_TRANSFER";
        if (u.contains("CHEQUE") || u.contains("CHECK")) return "CHEQUE";
        return "CASH";
    }

    private int findCol(Map<String, Integer> cols, String... names) {
        for (String n : names) { Integer idx = cols.get(n); if (idx != null) return idx; }
        // Fuzzy match
        for (String n : names) {
            for (var e : cols.entrySet()) {
                if (e.getKey().contains(n) || n.contains(e.getKey())) return e.getValue();
            }
        }
        return -1;
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) yield cell.getLocalDateTimeCellValue().toString();
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && d < 1e15) yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> { try { yield cell.getStringCellValue(); } catch (Exception e) { try { yield String.valueOf(cell.getNumericCellValue()); } catch (Exception e2) { yield ""; } } }
            default -> "";
        };
    }

    private double cellNum(Cell cell) {
        if (cell == null) return 0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().replaceAll("[^0-9.]", "");
                return s.isEmpty() ? 0 : Double.parseDouble(s);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private LocalDate parseDate(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim();
                // Try various formats
                String[] parts = s.split("[/\\-]");
                if (parts.length == 3) {
                    int d = Integer.parseInt(parts[0].trim());
                    int m = Integer.parseInt(parts[1].trim());
                    int y = Integer.parseInt(parts[2].trim());
                    if (y < 100) y += 2000;
                    if (d > 31) { int tmp = d; d = y; y = tmp; } // swap if year is first
                    return LocalDate.of(y, m, d);
                }
            }
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
        } catch (Exception e) {
            log.debug("Date parse failed: {}", e.getMessage());
        }
        return null;
    }

    private String genCode() {
        String c;
        do { c = "GF" + String.format("%06d", ThreadLocalRandom.current().nextInt(1, 999999)); }
        while (memberRepo.findByMemberCode(c).isPresent());
        return c;
    }
}

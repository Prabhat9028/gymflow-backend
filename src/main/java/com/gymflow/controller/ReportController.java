package com.gymflow.controller;
import com.gymflow.dto.Dtos.*;
import com.gymflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/reports") @RequiredArgsConstructor
public class ReportController {
    private final ReportService reportSvc;
    private final ExcelExportService excelSvc;

    @GetMapping("/membership")
    public ResponseEntity<MembershipReport> membership(
            @RequestParam UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportSvc.getMembershipReport(branchId, startDate, endDate));
    }

    @GetMapping("/pending-payments")
    public ResponseEntity<PendingPaymentsReport> pending(@RequestParam UUID branchId) {
        return ResponseEntity.ok(reportSvc.getPendingPaymentsReport(branchId));
    }

    @GetMapping("/membership/export")
    public ResponseEntity<byte[]> exportMembership(
            @RequestParam UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var r = reportSvc.getMembershipReport(branchId, startDate, endDate);
        List<String> h = List.of("Member", "Code", "Plan", "End Date", "Status", "Days Left");
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("--- SUMMARY ---", "", "", "", "", ""));
        rows.add(List.of("Total Members", String.valueOf(r.getTotalMembers()), "Active Subs", String.valueOf(r.getActiveSubscriptions()), "Expired", String.valueOf(r.getExpiredSubscriptions())));
        rows.add(List.of("Expiring 7d", String.valueOf(r.getExpiringIn7Days()), "Expiring 30d", String.valueOf(r.getExpiringIn30Days()), "", ""));
        rows.add(List.of("--- EXPIRING MEMBERS ---", "", "", "", "", ""));
        if (r.getUpcomingExpiry() != null) for (var e : r.getUpcomingExpiry())
            rows.add(List.of(e.getMemberName(), e.getMemberCode(), e.getPlanName(), str(e.getEndDate()), e.getStatus(), String.valueOf(e.getDaysUntilExpiry())));
        return xlsx("membership_report.xlsx", "Membership", h, rows);
    }

    @GetMapping("/pending-payments/export")
    public ResponseEntity<byte[]> exportPending(@RequestParam UUID branchId) {
        var r = reportSvc.getPendingPaymentsReport(branchId);
        List<String> h = List.of("Member", "Code", "Plan", "Amount Due", "Paid", "Balance", "Due Date", "Status");
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Total Pending", String.valueOf(r.getTotalPending()), "Amount", str(r.getTotalPendingAmount()), "Overdue", String.valueOf(r.getOverdue()), "Overdue Amt", str(r.getOverdueAmount())));
        rows.add(List.of("--- DETAILS ---", "", "", "", "", "", "", ""));
        if (r.getEntries() != null) for (var e : r.getEntries())
            rows.add(List.of(e.getMemberName(), e.getMemberCode(), e.getPlanName(), str(e.getAmountDue()), str(e.getAmountPaid()), str(e.getBalance()), str(e.getDueDate()), e.getStatus()));
        return xlsx("pending_payments.xlsx", "Pending Payments", h, rows);
    }

    @GetMapping("/plan-distribution/export")
    public ResponseEntity<byte[]> exportPlans(@RequestParam UUID branchId) {
        var r = reportSvc.getMembershipReport(branchId, null, null);
        List<String> h = List.of("Plan Name", "Active Members", "Revenue (₹)", "Share (%)");
        List<List<String>> rows = new ArrayList<>();
        if (r.getPlanDistribution() != null) for (var p : r.getPlanDistribution())
            rows.add(List.of(p.getPlanName(), String.valueOf(p.getActiveCount()), str(p.getRevenue()), String.valueOf(p.getPercentage())));
        return xlsx("plan_distribution.xlsx", "Plan Distribution", h, rows);
    }

    private ResponseEntity<byte[]> xlsx(String filename, String sheet, List<String> h, List<List<String>> rows) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(excelSvc.export(sheet, h, rows));
    }
    private String str(Object o) { return o != null ? o.toString() : ""; }
}

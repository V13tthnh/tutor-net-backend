package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.dashboard.DashboardResponse;
import com.tutornet.tutor_net.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.*;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('report:read')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "day") String interval
    ) {
        ZoneId zone  = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);

        // Resolve preset
        if (preset != null) {
            switch (preset.toLowerCase()) {
                case "today"      -> { fromDate = today;                        toDate = today; }
                case "this_week"  -> { fromDate = today.with(DayOfWeek.MONDAY); toDate = today; }
                case "this_month" -> { fromDate = today.withDayOfMonth(1);      toDate = today; }
                case "this_year"  -> { fromDate = today.withDayOfYear(1);       toDate = today; }
            }
        }

        // Fallback mặc định 30 ngày qua
        if (toDate   == null) toDate   = today;
        if (fromDate == null) fromDate = toDate.minusDays(30);

        // Convert LocalDate → Instant (có timezone)
        Instant fromInstant = fromDate.atStartOfDay(zone).toInstant();
        Instant toInstant   = toDate.atTime(LocalTime.MAX).atZone(zone).toInstant();

        // SQL Injection protection
        String safeInterval = switch (interval.toLowerCase()) {
            case "week"  -> "week";
            case "month" -> "month";
            case "year"  -> "year";
            default      -> "day";
        };

        DashboardResponse response = dashboardService.getDashboardData(fromInstant, toInstant, safeInterval);
        return ResponseEntity.ok(ApiResponse.ok("Tải dữ liệu Dashboard thành công", response));
    }
}
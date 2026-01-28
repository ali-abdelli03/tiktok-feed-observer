package com.socialtracker.backend.controller;

import com.socialtracker.backend.dto.response.ApiResponse;
import com.socialtracker.backend.dto.response.DashboardStatsDto;
import com.socialtracker.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for analytics and dashboard data
 */
@RestController
@RequestMapping("/api/v2/analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    /**
     * Get dashboard statistics summary
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        DashboardStatsDto stats = analyticsService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

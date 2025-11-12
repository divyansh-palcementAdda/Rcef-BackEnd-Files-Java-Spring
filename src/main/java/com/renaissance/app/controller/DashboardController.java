package com.renaissance.app.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.payload.DashboardDto;
import com.renaissance.app.service.interfaces.IDashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/dashboard")
//@CrossOrigin(origins = {"http://localhost:4200"}, allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final IDashboardService dashboardService;

    @GetMapping
    public ResponseEntity<?> getDashboard(Authentication authentication) {
        try {
            String username = authentication.getName();
            DashboardDto dashboard = dashboardService.getDashboardData(username);
            return ResponseEntity.ok(dashboard);
        } catch (ResourcesNotFoundException ex) {
            log.warn("Dashboard data not found: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected dashboard error", ex);
            return ResponseEntity.status(500).body(Map.of("message", "Failed to fetch dashboard data"));
        }
    }
}

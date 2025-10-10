package com.renaissance.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renaissance.app.payload.DashboardDto;
import com.renaissance.app.service.interfaces.IDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {"http://localhost:4200"}, allowCredentials = "true")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardDto> getDashboard(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(dashboardService.getDashboardData(username));
    }
}

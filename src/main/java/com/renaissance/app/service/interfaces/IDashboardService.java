package com.renaissance.app.service.interfaces;

import com.renaissance.app.payload.DashboardDto;

public interface IDashboardService {
    DashboardDto getDashboardData(String username);
}

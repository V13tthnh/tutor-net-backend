package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.response.dashboard.DashboardResponse;

import java.time.Instant  ;

public interface DashboardService {
    DashboardResponse getDashboardData(Instant fromDate, Instant toDate, String interval);

}

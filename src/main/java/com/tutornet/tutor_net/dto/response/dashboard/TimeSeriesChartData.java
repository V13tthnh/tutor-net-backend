package com.tutornet.tutor_net.dto.response.dashboard;

import java.math.BigDecimal;

public record TimeSeriesChartData(
        String timePeriod, // Ngày, Tuần, Tháng (Format string để render)
        BigDecimal revenue,
        long contractCount
) {}

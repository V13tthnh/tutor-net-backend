package com.tutornet.tutor_net.repository.projection;

import java.math.BigDecimal;

// Hứng kết quả Query Nhóm theo thời gian
public interface TimeSeriesProjection {
    String getTimePeriod();
    BigDecimal getRevenue();
    Long getContractCount();
}

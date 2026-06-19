package com.tutornet.tutor_net.repository.projection;

import java.math.BigDecimal;

public interface RevenueContractProjection {
    String     getTimePeriod();
    BigDecimal getRevenue();
    Long       getContractCount();
}

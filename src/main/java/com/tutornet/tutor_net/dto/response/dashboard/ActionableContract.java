package com.tutornet.tutor_net.dto.response.dashboard;

import java.math.BigDecimal;

public record ActionableContract(Long id, String contractNumber, String deadline, BigDecimal amount) {}

package com.tutornet.tutor_net.mail.payload;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentReminderPayload(String name, String contractNumber, BigDecimal amount, Instant deadline) {}

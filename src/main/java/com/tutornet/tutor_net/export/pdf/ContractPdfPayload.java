package com.tutornet.tutor_net.export.pdf;

import com.tutornet.tutor_net.entity.Contract;
import java.time.Instant;

public record ContractPdfPayload(
        Contract contract,
        String ipAddress,
        Instant signedAt,
        String contractHash
) {}

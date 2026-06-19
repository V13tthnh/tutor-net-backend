package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.TransactionFilterRequest;
import com.tutornet.tutor_net.dto.response.TransactionResponse;
import com.tutornet.tutor_net.dto.response.TransactionSummaryResponse;
import com.tutornet.tutor_net.enums.TransactionStatus;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.mapper.TransactionMapper;
import com.tutornet.tutor_net.repository.TransactionRepository;
import com.tutornet.tutor_net.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(TransactionFilterRequest filter) {
        Instant[] range = resolveRange(filter);

        String safeSearch = (filter.search() != null && !filter.search().isBlank())
                ? filter.search().trim() : null;

        PageRequest pageable = PageRequest.of(filter.page(), filter.size());

        return transactionRepository
                .findAllForAdmin(
                        filter.status() != null ? filter.status().name() : null,
                        filter.paymentMethod() != null ? filter.paymentMethod().name() : null,
                        range[0],
                        range[1],
                        safeSearch,
                        pageable
                )
                .map(transactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionSummaryResponse getSummary(TransactionFilterRequest filter) {
        Instant[] range = resolveRange(filter);

        long total   = transactionRepository.countAllInRange(range[0], range[1]);
        long success = transactionRepository.countByStatusInRange(TransactionStatus.SUCCESS.name(), range[0], range[1]);
        long pending = transactionRepository.countByStatusInRange(TransactionStatus.PENDING.name(), range[0], range[1]);
        long failed  = transactionRepository.countByStatusInRange(TransactionStatus.FAILED.name(),  range[0], range[1]);
        BigDecimal revenue = transactionRepository.sumRevenueInRange(range[0], range[1]);

        return new TransactionSummaryResponse(total, success, pending, failed, revenue);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        return transactionRepository.findByIdWithDetails(id)
                .map(transactionMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch ID: " + id));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    /**
     * Convert LocalDate filter → Instant[from, to].
     * Trả về [null, null] nếu không truyền fromDate/toDate.
     */
    private Instant[] resolveRange(TransactionFilterRequest filter) {
        Instant from = filter.fromDate() != null
                ? filter.fromDate().atStartOfDay(ZONE).toInstant()
                : null;
        Instant to = filter.toDate() != null
                ? filter.toDate().atTime(LocalTime.MAX).atZone(ZONE).toInstant()
                : null;
        return new Instant[]{from, to};
    }
}
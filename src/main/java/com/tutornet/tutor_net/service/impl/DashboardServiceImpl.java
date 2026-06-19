package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.response.dashboard.*;
import com.tutornet.tutor_net.enums.TutorStatus;
import com.tutornet.tutor_net.repository.*;
import com.tutornet.tutor_net.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant ;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TransactionRepository transactionRepository;
    private final ClassRequestRepository classRequestRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final ContractRepository contractRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(Instant fromDate, Instant toDate, String interval) {

        // TÍNH TOÁN KPIs
        BigDecimal rawRevenue = transactionRepository.sumRevenueBetweenDates(fromDate, toDate);
        BigDecimal totalRevenue = rawRevenue != null ? rawRevenue : BigDecimal.ZERO;

        long totalClasses = classRequestRepository.countRequestsBetweenDates(fromDate, toDate);
        long matchedClasses = classRequestRepository.countMatchedRequestsBetweenDates(fromDate, toDate);
        double matchRate = totalClasses > 0
                ? BigDecimal.valueOf((double) matchedClasses / totalClasses * 100).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        long newTutors = tutorProfileRepository.countByCreatedAtBetween(fromDate, toDate);
        long pendingTutorsCount = tutorProfileRepository.countByStatus(TutorStatus.PENDING_REVIEW);

        KpiMetrics kpis = new KpiMetrics(totalRevenue, totalClasses, matchRate, newTutors, pendingTutorsCount);

        // BIỂU ĐỒ (CHARTS)
        List<TimeSeriesChartData> timeSeriesChart = transactionRepository.getRevenueAndContractChart(fromDate, toDate, interval)
                .stream()
                .map(p -> new TimeSeriesChartData(p.getTimePeriod(), p.getRevenue() != null ? p.getRevenue() : BigDecimal.ZERO, p.getContractCount()))
                .toList();

        List<TopSubjectData> statusChart = classRequestRepository.getClassStatusChart(fromDate, toDate)
                .stream().map(p -> new TopSubjectData(p.getCategoryName(), p.getCount())).toList();

        List<TopSubjectData> topSubjectsChart = classRequestRepository.getTopSubjectsChart(fromDate, toDate)
                .stream().map(p -> new TopSubjectData(p.getCategoryName(), p.getCount())).toList();

        // DANH SÁCH BÁO ĐỘNG ĐỎ (Limit 5 records)
        PageRequest top5 = PageRequest.of(0, 5);

        List<ActionableTutor> pendingTutors = tutorProfileRepository.findByStatus(TutorStatus.PENDING_REVIEW, top5)
                .stream().map(t -> new ActionableTutor(t.getId(), t.getUser().getFullName(), t.getUser().getEmail(), t.getCreatedAt().toString())).toList();

        List<ActionableContract> overdueContracts = contractRepository.findTop5OverdueContracts(Instant.now(), top5)
                .stream().map(c -> new ActionableContract(c.getId(), c.getContractNumber(), c.getFeePaymentDeadline().toString(), c.getIntroductionFee())).toList();

        List<ActionableReview> negativeReviews = reviewRepository.findTop5NegativeReviews(top5)
                .stream().map(r -> new ActionableReview(r.getId(), r.getTutor().getUser().getFullName(), r.getRating(), r.getComment())).toList();

        List<RecentTransactionData> recentTransactions = transactionRepository
                .findTop5RecentSuccessful(fromDate, toDate, top5)
                .stream()
                .map(t -> new RecentTransactionData(
                        t.id(), t.transactionCode(), t.tutorName(),
                        t.contractNumber(), t.amount(),
                        t.paymentMethod(), t.status(), t.paidAt()))
                .toList();

        return new DashboardResponse(
                kpis, timeSeriesChart, topSubjectsChart, statusChart,
                pendingTutors, overdueContracts, negativeReviews,
                recentTransactions
        );
    }
}

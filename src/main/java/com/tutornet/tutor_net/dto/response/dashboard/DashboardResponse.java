package com.tutornet.tutor_net.dto.response.dashboard;

import java.util.List;

public record DashboardResponse(
        KpiMetrics                   kpis,
        List<TimeSeriesChartData>    timeSeriesChart,
        List<TopSubjectData>         topSubjects,
        List<TopSubjectData>         classStatus, // Bổ sung phân bổ trạng thái yêu cầu lớp
        List<ActionableTutor>        pendingTutors,
        List<ActionableContract>     overdueContracts,
        List<ActionableReview>       negativeReviews,
        List<RecentTransactionData>  recentTransactions
) {}

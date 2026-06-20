package com.tutornet.tutor_net.job;

import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.repository.ContractRepository;
import com.tutornet.tutor_net.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class  PaymentReminderJob {

    private final ContractRepository contractRepository;
    private final MailService mailService;

    /**
     // "0 * * * * *" -> Chạy lặp lại mỗi 1 phút để test
     // "0 0 8 * * *" -> Khi nào xong, bạn đổi thành chuỗi này để chạy 1 lần vào 8h sáng mỗi ngày
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void autoRemindPaymentJob() {
        log.info("--- [Cron Job] Đang thực thi rà soát công nợ ---");

        // Các mốc ngày cần nhắc (tính theo ngày, không theo giờ giây)
        List<Integer> reminderDays = List.of(7, 3, 1);

        List<Contract> unpaidContracts = new ArrayList<>();

        for (int days : reminderDays) {
            // Lấy đầu ngày và cuối ngày của mốc đó (UTC)
            Instant startOfDay = Instant.now()
                    .plus(days, ChronoUnit.DAYS)
                    .truncatedTo(ChronoUnit.DAYS);
            Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS).minusMillis(1);

            unpaidContracts.addAll(
                    contractRepository.findUnpaidContractsByDeadlineRange(startOfDay, endOfDay)
            );
        }

        if (unpaidContracts.isEmpty()) {
            log.info("[Cron Job] Không phát hiện hợp đồng nào cần nhắc.");
            return;
        }

        for (Contract contract : unpaidContracts) {
            if (contract.getTutor() != null && contract.getTutor().getUser() != null) {
                String email = contract.getTutor().getUser().getEmail();
                String fullName = contract.getTutor().getUser().getFullName();

                // Thực hiện gửi email nhắc nợ, nhúng mã ContractNumber vào mẫu
                mailService.sendPaymentReminderEmail(
                        email,
                        fullName,
                        contract.getContractNumber(),
                        contract.getIntroductionFee(),
                        contract.getFeePaymentDeadline()
                );

                log.info("[Cron Job] Đã gửi email nhắc phí thành công cho gia sư: {} | Số HD: {}",
                        email, contract.getContractNumber());
            }
        }
    }
}
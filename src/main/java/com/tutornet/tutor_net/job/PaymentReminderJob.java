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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void autoRemindPaymentJob() {
        log.info("--- [Cron Job] Đang thực thi rà soát công nợ hợp đồng gia sư ---");

        // Tính toán các mốc thời gian chính xác muốn gửi mail nhắc nợ
        List<Instant> targetDates = List.of(
                Instant.now().plus(7, ChronoUnit.DAYS),  // Hạn chót còn đúng 7 ngày
                Instant.now().plus(3, ChronoUnit.DAYS),  // Hạn chót còn đúng 3 ngày
                Instant.now().plus(1, ChronoUnit.DAYS)   // Hạn chót còn đúng 1 ngày
        );

        // Để test dễ dàng dính mọi bản ghi, ta truyền ngày quét nới rộng ra (hạn chót <= hôm nay + 40 ngày)
        LocalDate scanDeadline = LocalDate.now().plusDays(35);

        // Lấy danh sách hợp đồng ACTIVE và chưa đóng tiền phí dịch vụ
        // List<Contract> unpaidContracts = contractRepository.findContractsPendingPayment(scanDeadline);

        // Chỉ lấy ra các hợp đồng có ngày hạn khớp khít với các ngày trong list trên
        List<Contract> unpaidContracts = contractRepository.findContractsBySpecificDeadlines(targetDates);

        if (unpaidContracts.isEmpty()) {
            log.info("[Cron Job] Hoàn thành. Không phát hiện hợp đồng nào quá hạn hoặc nợ phí.");
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
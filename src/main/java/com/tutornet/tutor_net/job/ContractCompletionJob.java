package com.tutornet.tutor_net.job;

import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.ContractStatus;
import com.tutornet.tutor_net.event.ContractCompletedEvent;
import com.tutornet.tutor_net.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractCompletionJob {

    private final ContractRepository contractRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Tự động quét và đóng các hợp đồng hết hạn vào 22h00 tối mỗi ngày
     */

//    @Scheduled(cron = "0 */1 * * * ?")
    @Scheduled(cron = "0 0 22 * * ?")
    @Transactional
    public void autoCompleteExpiredContracts() {
        log.info("--- [Cron Job] Đang rà soát các hợp đồng đã hết hạn giảng dạy ---");

        List<Contract> expiredContracts = contractRepository.findExpiredActiveContracts(
                ContractStatus.ACTIVE,
                Instant.now()
        );

        if (expiredContracts.isEmpty()) {
            log.info("[Cron Job] Không phát hiện hợp đồng nào quá hạn.");
            return;
        }

        for (Contract contract : expiredContracts) {
            try {
                contract.setStatus(ContractStatus.COMPLETED);
                contractRepository.save(contract);

                // Bóc tách dữ liệu triệt để ngay trong Session DB (Ngăn chặn hoàn toàn lỗi delay/lazy load)
                User studentUser = contract.getClassRequest().getUser();

                // Xử lý luồng kép: Nếu là Guest (studentUser == null) thì lấy thông tin vãng lai ngoài ClassRequest
                Long studentUserId = studentUser != null ? studentUser.getId() : null;
                String studentEmail = studentUser != null ? studentUser.getEmail() : contract.getClassRequest().getContactEmail();
                String studentName = studentUser != null ? studentUser.getFullName() : contract.getClassRequest().getContactName();
                String tutorName = contract.getTutor().getUser().getFullName();

                // Phát động Sự kiện với cấu trúc Record mới đồng bộ
                eventPublisher.publishEvent(new ContractCompletedEvent(
                        contract.getId(),
                        contract.getContractNumber(),
                        studentUserId,
                        studentEmail,
                        studentName,
                        tutorName
                ));

                log.info("[Cron Job] Đã tự động đóng và kích hoạt luồng Review cho HD: {}", contract.getContractNumber());

            } catch (Exception e) {
                log.error("Lỗi khi tự động xử lý hoàn thành cho hợp đồng số {}: {}",
                        contract.getContractNumber(), e.getMessage());
            }
        }
    }
}
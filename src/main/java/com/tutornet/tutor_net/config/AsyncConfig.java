package com.tutornet.tutor_net.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // Bật tính năng chạy ngầm cho toàn hệ thống
public class AsyncConfig {

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);    // Số thread luôn luôn chạy chực chờ
        executor.setMaxPoolSize(5);     // Số thread tối đa khi bị quá tải
        executor.setQueueCapacity(100); // Nếu > 5 task gửi mail đến cùng lúc, nhét 100 task tiếp theo vào hàng đợi
        executor.setThreadNamePrefix("MailSender-");
        executor.initialize();
        return executor;
    }

    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(5);
        exec.setQueueCapacity(200);
        exec.setThreadNamePrefix("notif-");
        exec.initialize();
        return exec;
    }
}

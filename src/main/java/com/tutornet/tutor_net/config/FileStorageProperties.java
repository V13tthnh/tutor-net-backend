package com.tutornet.tutor_net.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.upload")
public class FileStorageProperties {
    private String dir;
    private String baseUrl;
    private long maxSize;
    private List<String> allowedTypes;
}

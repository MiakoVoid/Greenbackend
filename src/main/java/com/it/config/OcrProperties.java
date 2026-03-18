package com.it.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ocr")
public class OcrProperties {
    private Boolean enabled = Boolean.TRUE;
    private String provider = "qwen-vl";
    private Integer requestTimeoutMs = 1500;
    private Integer maxRetries = 1;
}


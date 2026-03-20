// 文件路径: D:/DevelopmentTool/JavaProgarm/greenfinance/src/main/java/com/it/config/AIConfig.java

package com.it.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI配置类
 * 配置AI模型相关参数（预留Qwen2.5-7B模型配置）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIConfig {
    
    /**
     * AI模型API地址（预留，用于集成Qwen2.5-7B-Instruct）
     */
    private String modelApiUrl;
    
    /**
     * AI模型API密钥
     */
    
    private String modelApiKey;  // 移除了错误的@Priority注解

    private String modelId;

    private Boolean streamEnabled = Boolean.FALSE;
    
    /**
     * 是否启用AI模型（默认false，使用HanLP作为兜底方案）
     */
    private Boolean modelEnabled = Boolean.FALSE;
    
    /**
     * OCR识别开关（默认true）
     */
    private Boolean ocrEnabled = Boolean.TRUE;
    
    /**
     * 财务建议开关（默认true）
     */
    private Boolean adviceEnabled = Boolean.TRUE;
    
}

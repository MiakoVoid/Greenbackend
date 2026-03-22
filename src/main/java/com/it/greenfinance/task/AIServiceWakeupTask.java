package com.it.greenfinance.task;

import com.it.utils.QwenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI服务唤醒任务
 * 每日定时调用Qwen API以保持AI服务活跃，防止服务休眠
 */
@Slf4j
@Component
public class AIServiceWakeupTask {

    @Autowired
    private QwenUtil qwenUtil;

    /**
     * 每日早上7点执行AI服务唤醒任务
     * 使用轻量级API调用保持服务活跃
     */
    @Scheduled(cron = "0 0 7 * * ?")
    public void wakeupAIService() {
        log.info("开始执行AI服务每日唤醒任务");
        try {
            // 调用QwenUtil的轻量级方法唤醒服务
            String result = qwenUtil.getFinancialAdvice("请生成一条简单的财务建议");
            if (result != null) {
                log.info("AI服务唤醒成功，返回结果: {}", result);
            } else {
                log.warn("AI服务唤醒失败，未收到有效响应");
            }
        } catch (Exception e) {
            log.error("AI服务唤醒任务执行异常", e);
        }
    }
}
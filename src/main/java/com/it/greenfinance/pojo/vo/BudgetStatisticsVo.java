package com.it.greenfinance.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算统计信息VO
 */

@Data
@Builder
public class BudgetStatisticsVo {
    /**
     * 剩余金额 = 总预算 - 已使用 - 本月预计支出
     */
    private BigDecimal remaining;
    
    /**
     * 每日预算 = 剩余预算 / 剩余天数
     */
    private BigDecimal daily;
    
    /**
     * 今日剩余金额 = 每日预算 - 今日支出
     */
    private BigDecimal todayRemaining;
    
    /**
     * 本月预计支出
     */
    private BigDecimal expected;
    /**
     * 本月已使用支出 = 本月支出 - 今日支出
     */
    private BigDecimal used;
}
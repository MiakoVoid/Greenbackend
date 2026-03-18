package com.it.greenfinance.service;

import com.it.greenfinance.pojo.vo.FinancialAdviceVo;

import java.util.Date;
import java.util.List;

public interface ReportService {
    /**
     * 4. 获取指定时间区间的分类支出统计
     *
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分类支出列表
     */
    List<FinancialAdviceVo.CategoryExpenseVo> getCategoryExpenseByTimeRange(Long userId, Date startDate, Date endDate);
    
    /**
     * 5. 获取指定时间区间的每日收支趋势
     *
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日收支趋势列表
     */
    List<FinancialAdviceVo.DailyTrendVo> getDailyTrendByTimeRange(Long userId, Date startDate, Date endDate);
    
    /**
     * 7. 获取财务分析报告（仅包含 AI 建议和预计支出）
     *
     * @param userId 用户 ID
     * @return 财务建议 VO
     */
    FinancialAdviceVo getFinancialAdvice(Long userId);
}

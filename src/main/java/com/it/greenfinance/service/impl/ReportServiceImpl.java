package com.it.greenfinance.service.impl;

import com.it.greenfinance.mapper.BillMapper;
import com.it.greenfinance.pojo.vo.FinancialAdviceVo;
import com.it.greenfinance.service.AIService;
import com.it.greenfinance.service.ExpectedExpenseService;
import com.it.greenfinance.service.ReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    private BillMapper billMapper;
    private ExpectedExpenseService expectedExpenseService;
    private AIService aiService;
    
    public ReportServiceImpl(BillMapper billMapper, ExpectedExpenseService expectedExpenseService, AIService aiService) {
        this.billMapper = billMapper;
        this.expectedExpenseService = expectedExpenseService;
        this.aiService = aiService;
    }
    /**
     * 获取指定时间区间的分类支出统计
     */
    @Override
    public List<FinancialAdviceVo.CategoryExpenseVo> getCategoryExpenseByTimeRange(Long userId, Date startDate, Date endDate) {
        List<Map<String, Object>> stats = billMapper.getCategoryExpenseByTimeRange(userId, startDate, endDate);
        
        List<FinancialAdviceVo.CategoryExpenseVo> categoryExpenseList = new ArrayList<>();
        BigDecimal totalExpense = BigDecimal.ZERO;
        
        // 先计算总额
        if (stats != null && !stats.isEmpty()) {
            for (Map<String, Object> stat : stats) {
                BigDecimal amount = (BigDecimal) stat.get("amount");
                totalExpense = totalExpense.add(amount);
            }
            
            // 构建返回数据并计算百分比
            for (Map<String, Object> stat : stats) {
                FinancialAdviceVo.CategoryExpenseVo categoryExpenseVo = new FinancialAdviceVo.CategoryExpenseVo();
                categoryExpenseVo.setCategoryId((Long) stat.get("categoryId"));
                categoryExpenseVo.setCategoryName((String) stat.get("categoryName"));
                BigDecimal amount = (BigDecimal) stat.get("amount");
                categoryExpenseVo.setAmount(amount);
                
                // 计算百分比
                BigDecimal percentage = totalExpense.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                        : amount.multiply(new BigDecimal("100")).divide(totalExpense, 2, RoundingMode.HALF_UP);
                categoryExpenseVo.setPercentage(percentage.toString() + "%");
                
                categoryExpenseList.add(categoryExpenseVo);
            }
        }
        
        return categoryExpenseList;
    }
    
    /**
     * 获取指定时间区间的每日收支趋势
     */
    @Override
    public List<FinancialAdviceVo.DailyTrendVo> getDailyTrendByTimeRange(Long userId, Date startDate, Date endDate) {
        List<Map<String, Object>> trendData = billMapper.getDailyTrendByTimeRange(userId, startDate, endDate);
        
        List<FinancialAdviceVo.DailyTrendVo> dailyTrendList = new ArrayList<>();
        
        if (trendData != null && !trendData.isEmpty()) {
            for (Map<String, Object> trend : trendData) {
                FinancialAdviceVo.DailyTrendVo trendVo = new FinancialAdviceVo.DailyTrendVo();
                Object dateObj = trend.get("date");
                trendVo.setDate(dateObj != null ? dateObj.toString() : "");
                trendVo.setIncome((BigDecimal) trend.get("income"));
                trendVo.setExpense((BigDecimal) trend.get("expense"));
                dailyTrendList.add(trendVo);
            }
        }
        
        return dailyTrendList;
    }
    
    @Override
    public FinancialAdviceVo getFinancialAdvice(Long userId) {
        // 委托给 AIService 处理财务分析报告
        return aiService.getFinancialAdvice(userId);
    }
}

package com.it.greenfinance.controller;

import com.it.greenfinance.pojo.vo.FinancialAdviceVo;
import com.it.greenfinance.service.AIService;
import com.it.greenfinance.service.ReportService;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 报表控制器
 * 提供财务统计报表相关接口
 */
@Slf4j
@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;
    private final UserContextUtil userContextUtil;
    
    public ReportController(ReportService reportService, UserContextUtil userContextUtil) {
        this.reportService = reportService;
        this.userContextUtil = userContextUtil;
    }
    
    /**
     * 获取财务分析报告（仅包含 AI 建议和预计支出）
     *
     * @return 财务建议 VO
     */
    @GetMapping("/advice")
    public Result getFinancialAdvice() {
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        try {
            FinancialAdviceVo advice = reportService.getFinancialAdvice(userId);
            return Result.ok(advice);
        } catch (Exception e) {
            log.error("获取财务建议失败", e);
            return Result.error(500, "获取建议失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取分类支出占比数据（饼图）
     * 支持时间区间选择
     * 
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param endDate 结束日期（格式：yyyy-MM-dd）
     * @return 分类支出列表
     */
    @GetMapping("/category-expense")
    public Result getCategoryExpense(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        if (startDate == null || endDate == null) {
            return Result.error(400, "开始日期和结束日期不能为空");
        }
        
        if (startDate.after(endDate)) {
            return Result.error(400, "开始日期不能晚于结束日期");
        }
        
        try {
            var categoryList = reportService.getCategoryExpenseByTimeRange(userId, startDate, endDate);
            return Result.ok(categoryList);
        } catch (Exception e) {
            log.error("获取分类支出失败", e);
            return Result.error(500, "获取分类支出失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取收支趋势数据（折线图）
     * 支持时间区间选择
     * 
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param endDate 结束日期（格式：yyyy-MM-dd）
     * @return 每日收支趋势列表
     */
    @GetMapping("/daily-trend")
    public Result getDailyTrend(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        
        if (startDate == null || endDate == null) {
            return Result.error(400, "开始日期和结束日期不能为空");
        }
        
        if (startDate.after(endDate)) {
            return Result.error(400, "开始日期不能晚于结束日期");
        }
        
        try {
            var trendList = reportService.getDailyTrendByTimeRange(userId, startDate, endDate);
            return Result.ok(trendList);
        } catch (Exception e) {
            log.error("获取收支趋势失败", e);
            return Result.error(500, "获取收支趋势失败：" + e.getMessage());
        }
    }
}

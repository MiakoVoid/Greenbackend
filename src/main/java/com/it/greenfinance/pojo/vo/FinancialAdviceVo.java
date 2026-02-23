package com.it.greenfinance.pojo.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class FinancialAdviceVo {
    /**
     * Local System Advice (Based on statistics)
     */
    private String localAdvice;

    /**
     * AI Generated Advice (Based on Qwen model)
     */
    private String aiAdvice;

    /**
     * Monthly Total Expense
     */
    private String totalExpense;

    /**
     * Top Spending Category
     */
    private String topCategory;

    /**
     * 饼图数据：分类支出占比
     */
    private List<CategoryExpenseVo> categoryExpenseList;

    /**
     * 折线图数据：近 7 天收支趋势
     */
    private List<DailyTrendVo> dailyTrendList;

    /**
     * 列表数据：预计支出提醒
     */
    private List<ExpectedExpenseRemindVo> expectedExpenseList;

    @Data
    public static class CategoryExpenseVo {
        private Long categoryId;
        private String categoryName;
        private BigDecimal amount;
        private String percentage;
    }

    @Data
    public static class DailyTrendVo {
        private String date;
        private BigDecimal income;
        private BigDecimal expense;
    }

    @Data
    public static class ExpectedExpenseRemindVo {
        private Long id;
        private String remark;
        private BigDecimal amount;
        private String dueDate;
        private Integer status;
    }
}

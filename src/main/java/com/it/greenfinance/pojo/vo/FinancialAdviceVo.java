package com.it.greenfinance.pojo.vo;

import lombok.Data;
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
}

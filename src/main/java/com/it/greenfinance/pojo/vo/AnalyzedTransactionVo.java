package com.it.greenfinance.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AnalyzedTransactionVo {
    /**
     * Original text segment
     */
    private String originalText;

    /**
     * Extracted remark
     */
    private String remark;

    /**
     * Extracted merchant name
     */
    private String merchant;

    /**
     * Extracted amount
     */
    private BigDecimal amount;

    /**
     * Transaction time
     */
    private LocalDateTime transactionTime;

    /**
     * Type: 1-Expense, 2-Income
     */
    private Integer type;

    /**
     * Suggested Category ID (if found)
     */
    private Long categoryId;

    /**
     * Suggested Category Name
     */
    private String categoryName;

    /**
     * Suggested SubCategory ID
     */
    private Long subCategoryId;

    /**
     * Suggested SubCategory Name
     */
    private String subCategoryName;

    /**
     * Whether this is a future expected expense
     */
    private Boolean isExpected;

    /**
     * Confidence level or source of classification (KEYWORD, AI, FALLBACK)
     */
    private String source;

    /**
     * Order Number extracted from text
     */
    private String orderNumber;
}

package com.it.greenfinance.service;

import com.it.greenfinance.pojo.vo.AnalyzedTransactionVo;
import com.it.greenfinance.pojo.vo.FinancialAdviceVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface AIService {

    /**
     * 1. Text Accounting Interface
     * Process natural language text, extract transaction information, AND create transactions.
     *
     * @param userId Current user ID
     * @param text Natural language input (e.g., "Lunch 20, bought coffee 15")
     * @param billTime Optional explicit bill time (YYYY-MM-DD HH:mm:ss)
     * @return List of created transaction objects (BillVo or ExpectedExpenseVo)
     */
    @Transactional(rollbackFor = Exception.class)
    List<Object> processText(Long userId, String text, String billTime);

    /**
     * 2. OCR Accounting Interface (Batch)
     * Process a list of text segments recognized from images AND create transactions.
     *
     * @param userId Current user ID
     * @param ocrTexts List of text segments from OCR
     * @return List of created transaction objects (BillVo or ExpectedExpenseVo)
     */
    @Transactional(rollbackFor = Exception.class)
    List<Object> processOcrTexts(Long userId, List<String> ocrTexts);

    /**
     * 3. Financial Advice Interface
     * Generate financial advice based on user's recent spending.
     * Includes both local statistical advice and AI-generated suggestions.
     *
     * @param userId The user ID to analyze
     * @return Financial Advice VO
     */
    FinancialAdviceVo getFinancialAdvice(Long userId);
    

}

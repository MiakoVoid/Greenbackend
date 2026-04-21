package com.it.greenfinance.service;

import com.it.greenfinance.pojo.vo.FinancialAdviceVo;
import com.it.greenfinance.pojo.vo.OcrBillParseResultVo;
import org.springframework.transaction.annotation.Transactional;

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
     * 2. OCR 批量记账接口
     * 处理 OCR 识别出的文本列表，解析为交易信息
     *
     * @param userId 用户 ID
     * @param ocrTexts OCR 文本列表
     * @param autoCreate 是否自动创建账单，true-自动创建，false-仅返回解析结果
     * @return OCR账单解析结构化结果
     */
    @Transactional(rollbackFor = Exception.class)
    OcrBillParseResultVo processOcrTexts(Long userId, List<String> ocrTexts, boolean autoCreate);

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

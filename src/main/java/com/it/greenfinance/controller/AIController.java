package com.it.greenfinance.controller;

import com.it.greenfinance.pojo.vo.FinancialAdviceVo;
import com.it.greenfinance.pojo.vo.OcrBillParseResultVo;
import com.it.greenfinance.service.AIService;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI模块控制器
 * 提供文本记账、OCR识别和财务建议接口
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AIController {

    private final AIService aiService;

    private final UserContextUtil userContextUtil;
    
    public AIController(AIService aiService, UserContextUtil userContextUtil) {
        this.aiService = aiService;
        this.userContextUtil = userContextUtil;
    }
    
    /**
     * 文本记账接口
     * 解析自然语言文本，返回识别出的交易信息
     * 
     * @param body 包含text字段的请求体
     * @return 解析后的交易记录列表
     */
    @PostMapping("/text")
    public Result processText(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        String billTime = body.get("billtime");
        if (text == null || text.trim().isEmpty()) {
            return Result.error(400, "输入文本不能为空");
        }
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        List<Object> result = aiService.processText(userId, text, billTime);
        System.out.println( result);
        return Result.ok(result);
    }

    /**
     * OCR 批量记账接口
     * 接收 OCR 识别出的文本列表，解析为交易信息
     * 
     * @param ocrTexts OCR 识别出的文本列表
     * @return OCR账单解析结构化结果
     */
    @PostMapping("/ocr")
    public Result processOcrTexts(@RequestBody List<String> ocrTexts) {
    
        if (ocrTexts == null || ocrTexts.isEmpty()) {
            return Result.error(400, "OCR 文本列表不能为空");
        }
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        // 不自动创建账单，仅返回解析结果供前端确认
        OcrBillParseResultVo result = aiService.processOcrTexts(userId, ocrTexts, false);
        System.out.println( result);
        return Result.ok(result);
    }

    /**
     * 财务建议接口
     * 获取当前用户的月度支出统计及AI财务建议
     * 
     * @return 财务建议VO
     */
    @GetMapping("/advice")
    public Result getFinancialAdvice() {
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        FinancialAdviceVo advice = aiService.getFinancialAdvice(userId);
        return Result.ok(advice);
    }
}

package com.it.greenfinance.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.it.greenfinance.mapper.BillMapper;
import com.it.greenfinance.mapper.CategoryKeywordMapper;
import com.it.greenfinance.mapper.CategoryMapper;
import com.it.greenfinance.mapper.SubCategoryMapper;
import com.it.greenfinance.pojo.Bill;
import com.it.greenfinance.pojo.Category;
import com.it.greenfinance.pojo.CategoryKeyword;
import com.it.greenfinance.pojo.SubCategory;
import com.it.greenfinance.pojo.bo.BillBo;
import com.it.greenfinance.pojo.bo.ExpectedExpenseBo;
import com.it.greenfinance.pojo.vo.AnalyzedTransactionVo;
import com.it.greenfinance.pojo.vo.BillVo;
import com.it.greenfinance.pojo.vo.FinancialAdviceVo;
import com.it.greenfinance.service.AIService;
import com.it.greenfinance.service.BillService;
import com.it.greenfinance.service.ExpectedExpenseService;
import com.it.utils.QwenUtil;
import com.it.utils.TimeProcessor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI服务实现类
 * 提供文本解析、OCR处理和财务建议等AI相关功能
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    /**
     * 分类关键词Mapper，用于数据库操作
     */
    private final CategoryKeywordMapper categoryKeywordMapper;

    /**
     * 分类Mapper，用于主分类数据库操作
     */
    private final CategoryMapper categoryMapper;

    /**
     * 子分类Mapper，用于子分类数据库操作
     */
    private final SubCategoryMapper subCategoryMapper;
    
    /**
     * 账单Mapper，用于账单相关数据库操作
     */
    private final BillMapper billMapper;

    /**
     * Qwen工具类，用于调用通义千问AI模型
     */
    private final QwenUtil qwenUtil;
    
    private final BillService billService;
    private final ExpectedExpenseService expectedExpenseService;

    /**
     * 金额匹配模式，用于从文本中提取金额
     */
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(\\.\\d{1,2})?)");
    
    /**
     * 显式时间参数匹配模式: billtime=YYYY-MM-DD HH:mm:ss
     */
    private static final Pattern BILLTIME_PATTERN = Pattern.compile("billtime[:= ]?(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})", Pattern.CASE_INSENSITIVE);

    public AIServiceImpl(CategoryKeywordMapper categoryKeywordMapper, CategoryMapper categoryMapper, SubCategoryMapper subCategoryMapper, BillMapper billMapper, QwenUtil qwenUtil, BillService billService, ExpectedExpenseService expectedExpenseService) {
        this.categoryKeywordMapper = categoryKeywordMapper;
        this.categoryMapper = categoryMapper;
        this.subCategoryMapper = subCategoryMapper;
        this.billMapper = billMapper;
        this.qwenUtil = qwenUtil;
        this.billService = billService;
        this.expectedExpenseService = expectedExpenseService;
    }
    
    /**
     * 处理自然语言文本，将其解析为交易记录列表，并自动创建交易
     * 
     * @param userId 用户ID
     * @param text 用户输入的自然语言文本
     * @return 创建后的交易对象列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Object> processText(Long userId, String text, String billTime) {
        long startTime = System.currentTimeMillis();
        if (!StringUtils.hasText(text)) {
            return new ArrayList<>();
        }

        LocalDateTime forcedTime = null;

        // 1. 优先处理传入的显式 billTime 参数
        if (StringUtils.hasText(billTime)) {
            try {
                forcedTime = LocalDateTime.parse(billTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                if (forcedTime.isAfter(LocalDateTime.now())) {
                    throw new IllegalArgumentException("Billtime cannot be in the future: " + billTime);
                }
                log.info("Using explicit billtime from request body: {}", forcedTime);
            } catch (DateTimeParseException e) {
                log.error("Invalid billtime format in body: {}", billTime);
                throw new IllegalArgumentException("Invalid billtime format. Expected: YYYY-MM-DD HH:mm:ss");
            }
        }
        // 2. 如果body中没有，尝试从文本中提取显式 billtime 参数 (兼容旧模式)
        else if (text.toLowerCase().contains("billtime")) {
            Matcher matcher = BILLTIME_PATTERN.matcher(text);
            if (matcher.find()) {
                String timeStr = matcher.group(1);
                try {
                    forcedTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    if (forcedTime.isAfter(LocalDateTime.now())) {
                        throw new IllegalArgumentException("Billtime cannot be in the future: " + timeStr);
                    }
                    // 移除 billtime 参数，避免干扰后续分词
                    text = matcher.replaceAll("");
                    log.info("Extracted explicit billtime from text: {}", forcedTime);
                } catch (DateTimeParseException e) {
                    log.error("Invalid billtime format in text: {}", timeStr);
                    throw new IllegalArgumentException("Invalid billtime format. Expected: YYYY-MM-DD HH:mm:ss");
                }
            } else {
                 // billtime keyword present but regex failed to match (format invalid)
                 log.error("Invalid billtime format found in text: {}", text);
                 throw new IllegalArgumentException("Invalid billtime format. Expected: YYYY-MM-DD HH:mm:ss");
            }
        }

        // 预加载分类数据，避免在循环中重复查询
        List<Category> categories = categoryMapper.selectList(null);
        List<SubCategory> subCategories = subCategoryMapper.selectList(null);

        // 按标点符号分割文本为多个片段
        String[] segmentArray = text.split("[,，.。;；\\n]");
        List<String> segments = Arrays.asList(segmentArray);
        
        // 预加载关键词
        Map<String, CategoryKeyword> keywordMap = preloadKeywords(segments);

        // 并行处理每个文本片段
        LocalDateTime finalForcedTime = forcedTime;
        List<AnalyzedTransactionVo> analyzedTransactions = segments.parallelStream()
                .filter(segment -> StringUtils.hasText(segment.trim()))
                .map(segment -> processSegment(segment.trim(), categories, subCategories, finalForcedTime, keywordMap))
                .collect(Collectors.toList());
        
        // 自动创建交易
        List<Object> result = batchCreateTransactions(userId, analyzedTransactions);
        
        long endTime = System.currentTimeMillis();
        log.info("Processed text for user {} with {} segments in {} ms", userId, segments.size(), (endTime - startTime));
        return result;
    }

    /**
     * 预加载关键词
     */
    private Map<String, CategoryKeyword> preloadKeywords(List<String> segments) {
        if (segments == null || segments.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // 收集所有分词
        Set<String> allTokens = segments.stream()
                .filter(StringUtils::hasText)
                .parallel()
                .flatMap(seg -> HanLP.segment(seg).stream().map(t -> t.word))
                .collect(Collectors.toSet());

        if (allTokens.isEmpty()) {
            return Collections.emptyMap();
        }

        List<CategoryKeyword> keywords = new ArrayList<>();
        List<String> tokenList = new ArrayList<>(allTokens);
        int batchSize = 1000;
        
        // 分批查询数据库
        for (int i = 0; i < tokenList.size(); i += batchSize) {
            List<String> batch = tokenList.subList(i, Math.min(i + batchSize, tokenList.size()));
            QueryWrapper<CategoryKeyword> query = new QueryWrapper<>();
            query.in("keyword", batch);
            keywords.addAll(categoryKeywordMapper.selectList(query));
        }

        // 构建映射：关键词 -> CategoryKeyword对象 (若有重复，取权重高的)
        return keywords.stream().collect(Collectors.toMap(
                CategoryKeyword::getKeyword,
                k -> k,
                (k1, k2) -> k1.getWeight() > k2.getWeight() ? k1 : k2
        ));
    }


    /**
     * 处理 OCR 识别后的文本列表，并自动创建交易
     * 
     * @param userId 用户 ID
     * @param ocrTexts OCR 识别出的文本行列表
     * @return 创建后的交易对象列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Object> processOcrTexts(Long userId, List<String> ocrTexts) {
        if (ocrTexts == null || ocrTexts.isEmpty()) {
            return new ArrayList<>();
        }
    
        // 1. OCR 文本预处理和聚合
        List<String> processedTexts = preprocessAndAggregateOcrTexts(ocrTexts);
        if (processedTexts.isEmpty()) {
            log.warn("OCR 文本预处理后无有效内容");
            return new ArrayList<>();
        }
    
        // 2. 预加载分类数据（带缓存优化）
        List<Category> categories = categoryMapper.selectList(null);
        List<SubCategory> subCategories = subCategoryMapper.selectList(null);
    
        // 3. 预加载关键词
        Map<String, CategoryKeyword> keywordMap = preloadKeywords(processedTexts);
    
        // 4. 并行处理每段 OCR 文本
        List<AnalyzedTransactionVo> analyzedTransactions = processedTexts.parallelStream()
                .filter(text -> StringUtils.hasText(text.trim()))
                .map(text -> {
                    try {
                        return processSegment(text.trim(), categories, subCategories, null, keywordMap);
                    } catch (Exception e) {
                        log.error("处理 OCR 文本片段失败：{}", text, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
        // 5. 自动创建交易
        return batchCreateTransactions(userId, analyzedTransactions);
    }

    /**
     * 生成财务建议
     * 
     * @param userId 用户ID
     * @return 包含本地统计和AI建议的财务建议VO
     */
    @Override
    public FinancialAdviceVo getFinancialAdvice(Long userId) {
        FinancialAdviceVo vo = new FinancialAdviceVo();
        LocalDate now = LocalDate.now();
        
        // 1. 获取当月分类支出统计数据
        List<Map<String, Object>> stats = billMapper.getCategoryExpenseByMonth(userId, now.getYear(), now.getMonthValue());
        
        BigDecimal totalExpense = BigDecimal.ZERO;
        String topCategory = "None";
        BigDecimal maxAmount = BigDecimal.ZERO;
        
        // 构建支出摘要
        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append(String.format("月份: %d-%02d. ", now.getYear(), now.getMonthValue()));

        // 统计各类别支出并找出最高支出类别，同时构建饼图数据
        List<FinancialAdviceVo.CategoryExpenseVo> categoryExpenseList = new ArrayList<>();
        if (stats != null && !stats.isEmpty()) {
            for (Map<String, Object> stat : stats) {
                String catName = (String) stat.get("categoryName");
                BigDecimal amount = (BigDecimal) stat.get("amount");
                        
                totalExpense = totalExpense.add(amount);
                        
                if (amount.compareTo(maxAmount) > 0) {
                    maxAmount = amount;
                    topCategory = catName;
                }
                summaryBuilder.append(String.format("%s: %.2f. ", catName, amount));
                        
                // 构建饼图数据
                FinancialAdviceVo.CategoryExpenseVo categoryExpenseVo = new FinancialAdviceVo.CategoryExpenseVo();
                categoryExpenseVo.setCategoryId((Long) stat.get("categoryId"));
                categoryExpenseVo.setCategoryName(catName);
                categoryExpenseVo.setAmount(amount);
                categoryExpenseList.add(categoryExpenseVo);
            }
                    
            // 计算百分比
            for (FinancialAdviceVo.CategoryExpenseVo item : categoryExpenseList) {
                BigDecimal percentage = totalExpense.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO 
                    : item.getAmount().multiply(new BigDecimal("100")).divide(totalExpense, 2, BigDecimal.ROUND_HALF_UP);
                item.setPercentage(percentage.toString() + "%");
            }
        }
        summaryBuilder.append(String.format("总计：%.2f.", totalExpense));
                
        vo.setTotalExpense(totalExpense.toString());
        vo.setTopCategory(topCategory);
        vo.setCategoryExpenseList(categoryExpenseList);
                
        // 生成本地建议
        if (totalExpense.compareTo(BigDecimal.ZERO) == 0) {
            vo.setLocalAdvice("本月暂无支出记录，继续保持记账习惯！");
        } else {
            vo.setLocalAdvice(String.format("您本月共支出%.2f 元，其中%s类别支出最高（%.2f 元）。",
                    totalExpense, topCategory, maxAmount));
        }
        
        // 2. 获取近 7 天收支趋势数据
        List<FinancialAdviceVo.DailyTrendVo> dailyTrendList = new ArrayList<>();
        List<Map<String, Object>> trendData = billMapper.getDailyTrend(userId, 6); // 包括今天共 7 天
                
        for (Map<String, Object> trend : trendData) {
            FinancialAdviceVo.DailyTrendVo trendVo = new FinancialAdviceVo.DailyTrendVo();
            Object dateObj = trend.get("date");
            trendVo.setDate(dateObj != null ? dateObj.toString() : "");
            trendVo.setIncome((BigDecimal) trend.get("income"));
            trendVo.setExpense((BigDecimal) trend.get("expense"));
            dailyTrendList.add(trendVo);
        }
        vo.setDailyTrendList(dailyTrendList);
                
        // 3. 获取预计支出提醒
        List<FinancialAdviceVo.ExpectedExpenseRemindVo> expectedExpenseList = new ArrayList<>();
        com.it.greenfinance.pojo.bo.ExpectedExpenseBo queryBo = new com.it.greenfinance.pojo.bo.ExpectedExpenseBo();
        queryBo.setStatus(1); // 只查询待支付的预计支出
        List<com.it.greenfinance.pojo.vo.ExpectedExpenseVo> expectedExpenses = expectedExpenseService.list(userId, queryBo);
                
        if (expectedExpenses != null && !expectedExpenses.isEmpty()) {
            for (com.it.greenfinance.pojo.vo.ExpectedExpenseVo expense : expectedExpenses) {
                FinancialAdviceVo.ExpectedExpenseRemindVo remindVo = new FinancialAdviceVo.ExpectedExpenseRemindVo();
                remindVo.setId(expense.getId());
                remindVo.setRemark(expense.getRemark());
                remindVo.setAmount(expense.getAmount());
                remindVo.setDueDate(expense.getDueDate() != null ? expense.getDueDate().toString() : "");
                remindVo.setStatus(expense.getStatus());
                expectedExpenseList.add(remindVo);
            }
        }
        vo.setExpectedExpenseList(expectedExpenseList);
        
        // 4. 获取 AI 建议
        String aiAdvice = qwenUtil.getFinancialAdvice(summaryBuilder.toString());
        vo.setAiAdvice(aiAdvice);
        
        return vo;
    }

    /**
     * OCR 文本预处理和聚合
     * 功能：
     * 1. 去除噪声（乱码、特殊符号、无意义字符）
     * 2. 合并相关行（同一账单的多行信息）
     * 3. 去重（重复识别的文本）
     * 4. 提取关键信息块
     * 
     * @param ocrTexts 原始 OCR 文本列表
     * @return 预处理后的文本列表
     */
    private List<String> preprocessAndAggregateOcrTexts(List<String> ocrTexts) {
        // 1. 过滤和清洗每行文本
        List<String> cleanedLines = ocrTexts.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(this::cleanOcrLine)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
            
        if (cleanedLines.isEmpty()) {
            return new ArrayList<>();
        }
            
        // 2. 检测并合并同一账单的多行信息
        List<String> aggregatedTexts = aggregateBillLines(cleanedLines);
            
        // 3. 去重（移除相似度极高的文本）
        return removeDuplicates(aggregatedTexts);
    }
        
    /**
     * 清洗单行 OCR 文本
     * 去除乱码、特殊符号、无意义字符
     * 
     * @param line 原始文本行
     * @return 清洗后的文本
     */
    private String cleanOcrLine(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
            
        // 1. 去除常见 OCR 噪声字符
        String cleaned = line.replaceAll("[\\x00-\\x1F\\x7F]", ""); // 控制字符
        cleaned = cleaned.replaceAll("[\u3000\u200B\uFEFF]+", ""); // 零宽字符和全角空格
            
        // 2. 修正常见 OCR 错误
        cleaned = cleaned.replaceAll("[0oO]", "0"); // 数字 0 的混淆（在金额中）
        cleaned = cleaned.replaceAll("[lIi]|", "1"); // 数字 1 的混淆（在金额中）
            
        // 3. 合并多余空格
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
            
        // 4. 去除过长重复字符（如 "啊啊啊" → "啊"）
        cleaned = cleaned.replaceAll("(.)\\1{2,}", "$1");
            
        return cleaned;
    }
        
    /**
     * 合并同一账单的多行信息
     * 根据关键词（商户名、金额、时间）将相关的行合并
     * 
     * @param lines 清洗后的文本行
     * @return 合并后的文本块列表
     */
    private List<String> aggregateBillLines(List<String> lines) {
        List<String> result = new ArrayList<>();
        StringBuilder currentBlock = new StringBuilder();
            
        for (String line : lines) {
            // 检测是否为新账单的开始
            boolean isNewBill = isBillStart(line);
                
            if (isNewBill && currentBlock.length() > 0) {
                // 保存当前的账单块
                result.add(currentBlock.toString().trim());
                currentBlock = new StringBuilder();
            }
                
            // 添加到当前块
            if (currentBlock.length() > 0) {
                currentBlock.append(" ");
            }
            currentBlock.append(line);
        }
            
        // 添加最后一个块
        if (currentBlock.length() > 0) {
            result.add(currentBlock.toString().trim());
        }
            
        return result;
    }
        
    /**
     * 判断是否为账单开始行
     * 通常包含商户名、支付平台等关键信息
     * 
     * @param line 文本行
     * @return 是否为账单开始
     */
    private boolean isBillStart(String line) {
        // 匹配常见支付平台标识
        String[] billStartPatterns = {
            ".*(微信 | 支付宝|银联|云闪付).*(收款 | 付款|支付|交易).*",
            ".*(喜茶 | 肯德基 | 麦当劳 | 星巴克|瑞幸|古茗|蜜雪冰城).*",
            ".*(超市 | 便利店 | 商场 | 餐厅|酒店|电影).*",
            ".*金额.*[0-9].*",
            ".*收款方.*",
            ".*商户.*"
        };
            
        for (String pattern : billStartPatterns) {
            if (line.matches(pattern)) {
                return true;
            }
        }
            
        return false;
    }
        
    /**
     * 去除重复或高度相似的文本
     * 
     * @param texts 文本列表
     * @return 去重后的文本列表
     */
    private List<String> removeDuplicates(List<String> texts) {
        List<String> result = new ArrayList<>();
            
        for (String text : texts) {
            boolean isDuplicate = false;
                
            // 检查是否与已有文本高度相似
            for (String existing : result) {
                if (isSimilar(text, existing)) {
                    isDuplicate = true;
                    break;
                }
            }
                
            if (!isDuplicate) {
                result.add(text);
            }
        }
            
        return result;
    }
        
    /**
     * 判断两个文本是否相似
     * 使用简单的编辑距离算法
     * 
     * @param text1 文本 1
     * @param text2 文本 2
     * @return 是否相似
     */
    private boolean isSimilar(String text1, String text2) {
        if (text1.equals(text2)) {
            return true;
        }
            
        // 如果长度差异过大，直接返回 false
        int lengthDiff = Math.abs(text1.length() - text2.length());
        if (lengthDiff > 5) {
            return false;
        }
            
        // 计算编辑距离
        int distance = calculateLevenshteinDistance(text1, text2);
            
        // 相似度阈值：编辑距离小于较短文本长度的 30%
        double threshold = Math.min(text1.length(), text2.length()) * 0.3;
        return distance <= threshold;
    }
        
    /**
     * 计算两个字符串的编辑距离（Levenshtein Distance）
     * 
     * @param s1 字符串 1
     * @param s2 字符串 2
     * @return 编辑距离
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
            
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
            
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1,      // 删除
                        dp[i][j - 1] + 1),     // 插入
                        dp[i - 1][j - 1] + cost); // 替换
            }
        }
            
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * 处理单个文本片段，提取交易信息
     * 
     * @param segment 文本片段
     * @param categories 主分类列表
     * @param subCategories 子分类列表
     * @param forcedTime 强制指定的时间（若非空，则直接使用）
     * @param keywordMap 预加载的关键词映射
     * @return 解析后的交易记录 VO
     */
    private AnalyzedTransactionVo processSegment(String segment, List<Category> categories, List<SubCategory> subCategories, LocalDateTime forcedTime, Map<String, CategoryKeyword> keywordMap) {
        AnalyzedTransactionVo tx = new AnalyzedTransactionVo();
        tx.setOriginalText(segment);
        tx.setType(1); // 默认为支出
        tx.setIsExpected(false);
        tx.setSource("RAW");

        // 提取金额信息
        extractAmount(segment, tx);
        // 提取时间信息
        extractTime(segment, tx, forcedTime);
        // 提取描述和意图
        extractDescriptionAndIntent(segment, tx);
        // 提取类型和订单号
        extractTypeAndOrderNumber(segment, tx);
        // 分类交易
        classifyTransaction(tx, categories, subCategories, keywordMap);

        return tx;
    }

    /**
     * 提取类型（收入/支出）和订单号
     *
     * @param text 原始文本
     * @param tx 交易记录VO
     */
    private void extractTypeAndOrderNumber(String text, AnalyzedTransactionVo tx) {
        // 提取订单号
        // 匹配规则：订单号/单号/No./Order No. 后接8位以上字母数字
        Pattern orderPattern = Pattern.compile("(订单号|单号|No\\.|Order No\\.)[:：]?\\s*([A-Za-z0-9]{8,})", Pattern.CASE_INSENSITIVE);
        Matcher orderMatcher = orderPattern.matcher(text);
        if (orderMatcher.find()) {
            tx.setOrderNumber(orderMatcher.group(2));
        }

        // 判断类型 (收入/支出)
        // 关键词：退款、收入、工资、转账、入账、进账
        if (text.matches(".*(退款|收入|工资|转账|入账|进账).*")) {
            tx.setType(2); // 收入
        }
        // 如果前面没有被标记为预期，这里再次检查
        if (!Boolean.TRUE.equals(tx.getIsExpected()) && text.matches(".*(预计|计划|打算|将来|下个月|下周|明天|后天).*")) {
            tx.setIsExpected(true);
        }
    }

    /**
     * 批量创建交易（内部方法，被processText和processOcrTexts调用）
     */
    private List<Object> batchCreateTransactions(Long userId, List<AnalyzedTransactionVo> transactions) {
        List<Object> createdObjects = new ArrayList<>();
        
        for (AnalyzedTransactionVo tx : transactions) {
            try {
                if (tx.getType() != null && tx.getType() == 2) { // Income
                    processIncomeTransaction(userId, tx, createdObjects);
                } else { // Expense (Default or Type 1)
                    processExpenseTransaction(userId, tx, createdObjects);
                }
            } catch (Exception e) {
                log.error("Error processing transaction: {}", tx, e);
                // 记录错误日志或抛出异常，这里选择继续处理下一个
            }
        }
        return createdObjects;
    }

    private void processIncomeTransaction(Long userId, AnalyzedTransactionVo tx, List<Object> createdObjects) {
        // 基础校验：金额必须有效
        if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Skipping income transaction due to invalid amount: {}", tx);
            return;
        }

        // 1. 检查是否存在匹配账单
        Bill existingBill = null;
        
        // 方式A: 通过订单号查询
        if (StringUtils.hasText(tx.getOrderNumber())) {
            existingBill = billMapper.selectOne(new QueryWrapper<Bill>()
                    .eq("user_id", userId)
                    .eq("order_number", tx.getOrderNumber())
                    .last("LIMIT 1"));
        }
        
        // 方式B: 若订单号未匹配，尝试通过 商品名(描述) + 价格 组合查询
        if (existingBill == null && tx.getAmount() != null && StringUtils.hasText(tx.getDescription())) {
             // 模糊匹配备注或商户名
             List<Bill> potentialBills = billMapper.selectList(new QueryWrapper<Bill>()
                    .eq("user_id", userId)
                    .eq("original_amount", tx.getAmount()) 
                    .and(wrapper -> wrapper.like("remark", tx.getDescription()).or().like("merchant", tx.getDescription()))
                    .orderByDesc("bill_time")); // 取最近的一笔
             
             if (!potentialBills.isEmpty()) {
                 existingBill = potentialBills.get(0);
             }
        }

        if (existingBill != null) {
            // 若存在匹配账单
            // 2. 更新原账单描述字段（追加新描述内容）
            String newRemark = existingBill.getRemark();
            if (StringUtils.hasText(tx.getDescription()) && (newRemark == null || !newRemark.contains(tx.getDescription()))) {
                newRemark = (newRemark == null ? "" : newRemark + " | ") + "退款/收入: " + tx.getDescription();
                existingBill.setRemark(newRemark);
            }
            
            // 3. 调整退款金额字段（根据新输入值重新计算）
            // 假设这是针对原支出账单的退款
            if (existingBill.getType() == 1) {
                BigDecimal newRefund = getBigDecimal(tx, existingBill);
                
                existingBill.setRefundAmount(newRefund);
                 // 自动计算最终金额: original - refund
                 existingBill.setAmount(existingBill.getOriginalAmount().subtract(newRefund));
                 existingBill.setUpdateTime(new Date());
                 
                 billMapper.updateById(existingBill);
                 // 返回更新后的完整对象
                 BillVo result = billService.getBillDetail(existingBill.getId());
                 createdObjects.add(result);
                 log.info("Updated Bill (Refund): id={}, amount={}, newRefund={}", existingBill.getId(), existingBill.getAmount(), newRefund);
            } else {
                // 如果原账单已经是收入，通常不处理退款逻辑，仅更新备注
                billMapper.updateById(existingBill);
                BillVo result = billService.getBillDetail(existingBill.getId());
                createdObjects.add(result);
                log.info("Updated Bill (Income Remark): id={}, remark={}", existingBill.getId(), existingBill.getRemark());
            }
        } else {
            // 创建新账单前的校验
            if (tx.getCategoryId() == null) {
                log.warn("Skipping income bill creation due to missing category: {}", tx);
                return;
            }
            
            // 4. 若无匹配账单，创建完整收入账单记录
            BillBo billBo = getBillBo(tx, 2);
            // 调用标准收入账单创建接口
            BillVo result = billService.createBill(billBo);
            createdObjects.add(result);
            log.info("Created Bill (Income): id={}, amount={}, orderNo={}", result.getId(), result.getOriginalAmount(), result.getOrderNumber());
        }
    }
    
    private BigDecimal getBigDecimal(AnalyzedTransactionVo tx, Bill existingBill) {
        BigDecimal currentRefund = existingBill.getRefundAmount() == null ? BigDecimal.ZERO : existingBill.getRefundAmount();
        BigDecimal refundAmt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
        BigDecimal newRefund = currentRefund.add(refundAmt);
        
        // 确保退款金额不超过原金额
        if (newRefund.compareTo(existingBill.getOriginalAmount()) > 0) {
            newRefund = existingBill.getOriginalAmount();
        }
        return newRefund;
    }
    
    @NotNull
    private BillBo getBillBo(AnalyzedTransactionVo tx, int type) {
        BillBo billBo = new BillBo();
        billBo.setOriginalAmount(tx.getAmount());
        billBo.setType(type); // 收入
        billBo.setRemark(tx.getDescription());
        billBo.setCategoryId(tx.getCategoryId());
        billBo.setSubCategoryId(tx.getSubCategoryId());
        billBo.setOrderNumber(tx.getOrderNumber());
        Date billTime = new Date();
        if (tx.getTransactionTime() != null) {
            billTime = Date.from(tx.getTransactionTime().atZone(ZoneId.systemDefault()).toInstant());
        }
        billBo.setBillTime(billTime);
        return billBo;
    }
    
    private void processExpenseTransaction(Long userId, AnalyzedTransactionVo tx, List<Object> createdObjects) {
        // 基础校验
        if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Skipping expense transaction due to invalid amount: {}", tx);
            return;
        }
        if (tx.getCategoryId() == null) {
            log.warn("Skipping expense transaction due to missing category: {}", tx);
            return;
        }

        if (Boolean.TRUE.equals(tx.getIsExpected())) {
            // 预计支出处理
            ExpectedExpenseBo bo = new ExpectedExpenseBo();
            bo.setUserId(userId);
            bo.setAmount(tx.getAmount());
            bo.setCategoryId(tx.getCategoryId());
            bo.setSubCategoryId(tx.getSubCategoryId());
            bo.setRemark(tx.getDescription());
            
            Date dueDate = new Date();
            if (tx.getTransactionTime() != null) {
                dueDate = Date.from(tx.getTransactionTime().atZone(ZoneId.systemDefault()).toInstant());
            }
            bo.setDueDate(dueDate);
            bo.setStatus(1); // 1-待支付
            
            // 调用预计支出专用创建接口
            com.it.greenfinance.pojo.vo.ExpectedExpenseVo result = expectedExpenseService.create(bo);
            createdObjects.add(result);
            log.info("Created Expected Expense: id={}, amount={}, dueDate={}", result.getId(), result.getAmount(), result.getDueDate());
        } else {
            // 支出账单处理
            BillBo billBo = getBillBo(tx, 1);
            
            // 调用标准支出账单创建接口
            BillVo result = billService.createBill(billBo);
            createdObjects.add(result);
            log.info("Created Bill (Expense): id={}, amount={}, orderNo={}", result.getId(), result.getOriginalAmount(), result.getOrderNumber());
        }
    }

    /**
     * 从文本中提取金额
     * 
     * @param text 原始文本
     * @param tx 交易记录VO
     */
    private void extractAmount(String text, AnalyzedTransactionVo tx) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        String lastMatch = null;
        // 查找最后一个匹配的金额（通常是总金额）
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        if (lastMatch != null) {
            tx.setAmount(new BigDecimal(lastMatch));
        }
    }

    /**
     * 从文本中提取时间信息
     * 
     * @param text 原始文本
     * @param tx 交易记录VO
     * @param forcedTime 强制指定的时间（若非空，则直接使用）
     */
    private void extractTime(String text, AnalyzedTransactionVo tx, LocalDateTime forcedTime) {
        if (forcedTime != null) {
            tx.setTransactionTime(forcedTime);
            return;
        }

        // 使用 TimeProcessor 进行自然语言时间解析
        LocalDateTime parsedTime = TimeProcessor.parseNaturalLanguage(text);

        // 如果仍未解析出时间，记录日志并使用默认时间NOW
        if (parsedTime == null) {
            // 记录无法识别的时间描述
            log.warn("无法识别时间描述：{}", text);
            // 默认设为当前时间
            parsedTime = LocalDateTime.now();
        }

        tx.setTransactionTime(parsedTime);
    }

    /**
     * 提取描述信息和判断是否为预期交易
     *
     * @param text 原始文本
     * @param tx 交易记录VO
     */
    private void extractDescriptionAndIntent(String text, AnalyzedTransactionVo tx) {
        // 使用HanLP进行中文分词
        List<Term> terms = HanLP.segment(text);
        StringBuilder desc = new StringBuilder();
        boolean isExpected = false;
        if (tx.getTransactionTime()!=null){
            if (tx.getTransactionTime().isAfter(LocalDateTime.now())){
                isExpected = true;
            }
        }
        // 遍历分词结果，提取描述并判断是否为预期交易
        for (Term term : terms) {
            String word = term.word;
            // 判断是否包含预期交易关键词 - 优化后的正则表达式
            if (word.matches(".*(打算|预计|计划|预备|准备|想要|想买|想喝|想吃|要去|将要|即将|明天|后天|下周|下月|明年|今晚|稍后|一会儿|待会|过后|之后).*")) {
                isExpected = true;
            }
            // 过滤掉纯数字和货币单位
            if (word.matches("\\d+(\\.\\d+)?") ||
                    word.matches("[rRｗＲｒ円￥$＄元块kKｋＫwWｗＷ万元千元]+")) {
                continue;
            }
            desc.append(word).append(" ");
        }
        tx.setDescription(desc.toString().trim());
        tx.setIsExpected(isExpected);
    }
    
    
    /**
     * 对交易进行分类
     * 
     * @param tx 交易记录VO
     * @param categories 所有主分类
     * @param subCategories 所有子分类
     * @param keywordMap 预加载的关键词映射
     */
    private void classifyTransaction(AnalyzedTransactionVo tx, List<Category> categories, List<SubCategory> subCategories, Map<String, CategoryKeyword> keywordMap) {
        String description = tx.getDescription();
        if (!StringUtils.hasText(description)) {
            return;
        }

        // 1. 基于预加载的关键词映射进行匹配
        List<Term> terms = HanLP.segment(description);
        List<String> tokens = terms.stream().map(t -> t.word).collect(Collectors.toList());
        
        if (!tokens.isEmpty()) {
            // 在 Map 中查找匹配的关键词
            List<CategoryKeyword> matches = tokens.stream()
                .map(keywordMap::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(CategoryKeyword::getWeight).reversed())
                .collect(Collectors.toList());
                    
            // 如果找到匹配关键词，使用该关键词对应的分类
            if (!matches.isEmpty()) {
                CategoryKeyword match = matches.get(0);
                fillCategoryInfo(tx, match, categories, subCategories);
                tx.setSource("KEYWORD_DB");
                log.debug("关键词匹配：{} -> {}({})", description, tx.getCategoryName(), tx.getSource());
                return;
            }
        }
        
        // 构建分类名称列表
        List<String> categoryNames = new ArrayList<>();
        categories.forEach(c -> categoryNames.add(c.getName()));
        subCategories.forEach(sc -> categoryNames.add(sc.getName()));
        
        // 2. 优先匹配子分类（更精确）
        Optional<String> subCategoryMatch = subCategories.stream()
                .map(SubCategory::getName)
                .filter(name -> description.contains(name))
                .findFirst();
        
        if (subCategoryMatch.isPresent()) {
            String matchedName = subCategoryMatch.get();
            Optional<SubCategory> subMatch = subCategories.stream()
                    .filter(sc -> sc.getName().equals(matchedName)).findFirst();
            if (subMatch.isPresent()) {
                tx.setSubCategoryId(subMatch.get().getId());
                tx.setSubCategoryName(subMatch.get().getName());
                tx.setCategoryId(subMatch.get().getCategoryId());
                categories.stream().filter(c -> c.getId().equals(subMatch.get().getCategoryId()))
                        .findFirst().ifPresent(c -> tx.setCategoryName(c.getName()));
                tx.setSource("DIRECT_MATCH_SUB");
                log.debug("直接匹配子分类：{} -> {}({})", description, tx.getSubCategoryName(), tx.getSource());
                return;
            }
        }
        
        // 3. 其次匹配主分类
        Optional<String> categoryMatch = categories.stream()
                .map(Category::getName)
                .filter(name -> !"其他".equals(name) && description.contains(name))
                .findFirst();
        
        if (categoryMatch.isPresent()) {
            String matchedName = categoryMatch.get();
            Optional<Category> catMatch = categories.stream()
                    .filter(c -> c.getName().equals(matchedName)).findFirst();
            if (catMatch.isPresent()) {
                tx.setCategoryId(catMatch.get().getId());
                tx.setCategoryName(catMatch.get().getName());
                tx.setSource("DIRECT_MATCH");
                log.debug("直接匹配主分类：{} -> {}({})", description, tx.getCategoryName(), tx.getSource());
                return;
            }
        }
        // 4. 使用 AI 智能分类
        try {
            
            // 调用 AI 进行分类并提取时间
            JSONObject aiResultObj = qwenUtil.classifyWithTime(description, categoryNames);
            
            if (aiResultObj != null) {
                String aiCategory = aiResultObj.getString("category");
                String aiDate = aiResultObj.getString("date");

                // 如果 AI 提取到了日期，且之前未解析出日期，则使用 AI 提取的日期
                if (StringUtils.hasText(aiDate) && tx.getTransactionTime() == null) {
                    try {
                        LocalDate date = LocalDate.parse(aiDate);
                        tx.setTransactionTime(date.atStartOfDay());
                        log.debug("AI 提取时间：{} -> {}", description, aiDate);
                    } catch (Exception e) {
                        log.warn("Failed to parse date from AI result: {}", aiDate);
                    }
                }

                if (StringUtils.hasText(aiCategory)) {
                    // 优先查找匹配的子分类
                    Optional<SubCategory> subMatch = subCategories.stream()
                            .filter(sc -> sc.getName().equals(aiCategory)).findFirst();
                    if (subMatch.isPresent()) {
                        tx.setSubCategoryId(subMatch.get().getId());
                        tx.setSubCategoryName(subMatch.get().getName());
                        tx.setCategoryId(subMatch.get().getCategoryId());
                        categories.stream().filter(c -> c.getId().equals(subMatch.get().getCategoryId()))
                                .findFirst().ifPresent(c -> tx.setCategoryName(c.getName()));
                        
                        // 保存新学到的关键词
                        saveNewKeyword(description, subMatch.get().getId(), subMatch.get().getCategoryId());
                        tx.setSource("AI_QWEN_SUB");
                        log.info("AI 分类（子分类）：{} -> {}({})", description, tx.getSubCategoryName(), tx.getSource());
                        return;
                    }
                    
                    // 查找匹配的主分类
                    Optional<Category> catMatch = categories.stream()
                            .filter(c -> c.getName().equals(aiCategory)).findFirst();
                    if (catMatch.isPresent()) {
                        tx.setCategoryId(catMatch.get().getId());
                        tx.setCategoryName(catMatch.get().getName());
                        
                        // 保存新学到的关键词
                        saveNewKeyword(description, null, catMatch.get().getId());
                        tx.setSource("AI_QWEN");
                        log.info("AI 分类（主分类）：{} -> {}({})", description, tx.getCategoryName(), tx.getSource());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.error("AI 分类失败：{}", description, e);
        }

        // 5. 回退到默认分类"其他"
        tx.setCategoryName("其他");
        tx.setSource("FALLBACK");
        // 尝试查找"其他"分类 ID
        Optional<Category> otherCat = categories.stream()
                .filter(c -> "其他".equals(c.getName())).findFirst();
        otherCat.ifPresent(category -> tx.setCategoryId(category.getId()));
        log.warn("回退分类：{} -> 其他 (FALLBACK)", description);
    }

    /**
     * 填充分类信息
     * 
     * @param tx 交易记录VO
     * @param keyword 分类关键词
     * @param categories 所有主分类
     * @param subCategories 所有子分类
     */
    private void fillCategoryInfo(AnalyzedTransactionVo tx, CategoryKeyword keyword, List<Category> categories, List<SubCategory> subCategories) {
        tx.setCategoryId(keyword.getCategoryId());
        tx.setSubCategoryId(keyword.getSubCategoryId());
        
        // 填充主分类名称
        if (keyword.getCategoryId() != null) {
            categories.stream().filter(c -> c.getId().equals(keyword.getCategoryId()))
                    .findFirst().ifPresent(c -> tx.setCategoryName(c.getName()));
        }
        // 填充子分类名称
        if (keyword.getSubCategoryId() != null) {
            subCategories.stream().filter(sc -> sc.getId().equals(keyword.getSubCategoryId()))
                    .findFirst().ifPresent(sc -> tx.setSubCategoryName(sc.getName()));
        }
    }

    /**
     * 保存新学到的关键词
     * 
     * @param keyword 关键词
     * @param subId 子分类ID
     * @param catId 主分类ID
     */
    private void saveNewKeyword(String keyword, Long subId, Long catId) {
        // 异步保存，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 检查关键词是否已存在
                QueryWrapper<CategoryKeyword> check = new QueryWrapper<>();
                check.eq("keyword", keyword);
                if (categoryKeywordMapper.selectCount(check) == 0) {
                    // 创建新的关键词记录
                    CategoryKeyword newKw = new CategoryKeyword();
                    newKw.setKeyword(keyword);
                    newKw.setCategoryId(catId);
                    newKw.setSubCategoryId(subId);
                    newKw.setType(subId != null ? 2 : 1); // 1为主分类，2为子分类
                    newKw.setMatchValue(keyword);
                    newKw.setWeight(10); // 默认权重
                    newKw.setUserId(0L); // 系统用户
                    categoryKeywordMapper.insert(newKw);
                    log.info("学习到新关键词: {} -> 分类ID: {}", keyword, catId);
                }
            } catch (Exception e) {
                log.error("保存学习到的关键词失败", e);
            }
        });
    }

}

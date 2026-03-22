package com.it.greenfinance.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.it.greenfinance.mapper.*;
import com.it.greenfinance.pojo.Bill;
import com.it.greenfinance.pojo.Category;
import com.it.greenfinance.pojo.CategoryKeyword;
import com.it.greenfinance.pojo.SubCategory;
import com.it.greenfinance.pojo.bo.BillBo;
import com.it.greenfinance.pojo.bo.ExpectedExpenseBo;
import com.it.greenfinance.pojo.vo.AnalyzedTransactionVo;
import com.it.greenfinance.pojo.vo.BillVo;
import com.it.greenfinance.pojo.vo.FinancialAdviceVo;
import com.it.greenfinance.pojo.vo.OcrBillParseResultVo;
import com.it.greenfinance.service.AIService;
import com.it.greenfinance.service.BillService;
import com.it.greenfinance.service.ExpectedExpenseService;
import com.it.utils.QwenUtil;
import com.it.utils.TimeProcessor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
      * 系统配置Mapper，用于系统配置数据库操作
      */
    private final SystemConfigMapper systemConfigMapper;
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
     * 支持小数位记账，如 20.5, 20.55, 20.555 (后续会四舍五入)
     */
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)");
    
    /**
     * 显式时间参数匹配模式: billtime=YYYY-MM-DD HH:mm:ss
     */
    private static final Pattern BILLTIME_PATTERN = Pattern.compile("billtime[:= ]?(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})", Pattern.CASE_INSENSITIVE);
    
    private static final DateTimeFormatter DATE_TIME_SEC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AIServiceImpl(CategoryKeywordMapper categoryKeywordMapper, SystemConfigMapper systemConfigMapper, CategoryMapper categoryMapper, SubCategoryMapper subCategoryMapper, BillMapper billMapper, QwenUtil qwenUtil, BillService billService, ExpectedExpenseService expectedExpenseService) {
        this.categoryKeywordMapper = categoryKeywordMapper;
        this.systemConfigMapper = systemConfigMapper;
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
        if (!StringUtils.hasText(text)) {
            return new ArrayList<>();
        }

        LocalDateTime forcedTime = parseForcedTime(text, billTime);
        // 如果是从文本中提取的，需要移除 billtime 部分
        String cleanText = (billTime == null && text.toLowerCase().contains("billtime")) 
                ? BILLTIME_PATTERN.matcher(text).replaceAll("") : text;

        // 预加载分类数据
        List<Category> categories = categoryMapper.selectList(new QueryWrapper<Category>()
                .isNull("user_id").or().eq("user_id", userId));
        List<SubCategory> subCategories = subCategoryMapper.selectList(new QueryWrapper<SubCategory>()
                .isNull("user_id").or().eq("user_id", userId));

        // 按标点符号分割文本
        String[] segmentArray = cleanText.split("[,，。;；\\n]|(?<!\\d)\\.|\\.(?!\\d)");
        List<String> segments = Arrays.stream(segmentArray)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        
        // 预加载关键词
        Map<String, CategoryKeyword> keywordMap = preloadKeywords(segments);

        // 并行处理每个文本片段
        List<AnalyzedTransactionVo> analyzedTransactions = segments.parallelStream()
                .map(segment -> processSegment(segment, categories, subCategories, forcedTime, keywordMap))
                .collect(Collectors.toList());
        
        // 根据系统设置决定是否自动创建交易
        String checkValue = systemConfigMapper.getConfigValue("check", userId);
        int check = (StringUtils.hasText(checkValue)) ? Integer.parseInt(checkValue) : 0;
        
        if (check == 0) {
            List<Object> result = batchCreateTransactions(userId, analyzedTransactions);
            log.info("用户 {}：已自动创建 {} 笔交易", userId, result.size());
            return result;
        } else {
            log.info("用户 {}：check=1，返回 {} 笔解析结果", userId, analyzedTransactions.size());
            return new ArrayList<>(analyzedTransactions);
        }
    }

    private LocalDateTime parseForcedTime(String text, String billTime) {
        String timeStr = billTime;
        if (!StringUtils.hasText(timeStr)) {
            Matcher matcher = BILLTIME_PATTERN.matcher(text);
            if (matcher.find()) {
                timeStr = matcher.group(1);
            }
        }

        if (StringUtils.hasText(timeStr)) {
            try {
                LocalDateTime forcedTime = LocalDateTime.parse(timeStr, DATE_TIME_SEC);
                if (forcedTime.isAfter(LocalDateTime.now())) {
                    throw new IllegalArgumentException("Billtime cannot be in the future: " + timeStr);
                }
                return forcedTime;
            } catch (DateTimeParseException e) {
                log.error("Invalid billtime format: {}", timeStr);
                throw new IllegalArgumentException("Invalid billtime format. Expected: YYYY-MM-DD HH:mm:ss");
            }
        }
        return null;
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
     * OCR 批量记账接口
     * 
     * @param userId 用户 ID
     * @param ocrTexts OCR 识别出的文本列表
     * @param autoCreate 是否自动创建账单，true-自动创建，false-仅返回解析结果
     * @return OCR账单解析结构化结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OcrBillParseResultVo processOcrTexts(Long userId, List<String> ocrTexts, boolean autoCreate) {
        if (ocrTexts == null || ocrTexts.isEmpty()) {
            return new OcrBillParseResultVo();
        }
        String result = qwenUtil.parseOcrTexts(userId, ocrTexts);
        OcrBillParseResultVo vo = new OcrBillParseResultVo();
        if (result != null) {
            List<BillVo> bills = JSON.parseArray(result, BillVo.class);
            if (autoCreate) {
                List<BillVo> createdBills = new ArrayList<>();
                for (BillVo billVo : bills) {
                    BillBo billBo = new BillBo();
                    BeanUtils.copyProperties(billVo, billBo);
                    billBo.setUserId(userId);
                    // AI 返回的是 amount，BillBo 需要 originalAmount
                    if (billBo.getOriginalAmount() == null) {
                        billBo.setOriginalAmount(billVo.getAmount());
                    }
                    createdBills.add(billService.createBill(billBo));
                }
                vo.setBills(createdBills);
            } else {
                for (BillVo bill : bills) {
                    bill.setUserId(userId);
                    bill.setOriginalAmount(bill.getAmount());
                }
                vo.setBills(bills);
            }
        }
        return vo;
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
                    : item.getAmount().multiply(new BigDecimal("100")).divide(totalExpense, 2, RoundingMode.HALF_UP);
                item.setPercentage(percentage + "%");
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
                remindVo.setCategoryId(expense.getCategoryId());
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
        String amountStr = extractAmount(segment, tx);
        
        // 优化：从文本中移除识别出的金额部分，避免干扰 Natty 时间解析 (如 "9.9" 导致解析错误)
        String timeParsingText = segment;
        if (amountStr != null) {
            timeParsingText = segment.replace(amountStr, "").trim();
            if (timeParsingText.isEmpty()) {
                timeParsingText = segment;
            }
        }
        
        // 提取时间信息
        extractTime(timeParsingText, tx, forcedTime);
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
        if (transactions == null || transactions.isEmpty()) {
            return createdObjects;
        }

        // 1. 预加载可能存在的账单，减少循环中的数据库查询次数
        Map<String, Bill> orderNumberMap = prefetchBillsByOrderNumbers(userId, transactions);
        // 对于按金额和备注匹配的情况，由于需要模糊匹配且组合较多，维持原有按需查询或采用更复杂的缓存
        
        for (AnalyzedTransactionVo tx : transactions) {
            try {
                if (tx.getType() != null && tx.getType() == 2) { // Income
                    processIncomeTransaction(userId, tx, createdObjects, orderNumberMap);
                } else { // Expense (Default or Type 1)
                    processExpenseTransaction(userId, tx, createdObjects);
                }
            } catch (Exception e) {
                log.error("Error processing transaction: {}", tx, e);
            }
        }
        return createdObjects;
    }

    private Map<String, Bill> prefetchBillsByOrderNumbers(Long userId, List<AnalyzedTransactionVo> transactions) {
        Set<String> orderNumbers = transactions.stream()
                .map(AnalyzedTransactionVo::getOrderNumber)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        
        if (orderNumbers.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Bill> existingBills = billMapper.selectList(new QueryWrapper<Bill>()
                .eq("user_id", userId)
                .in("order_number", orderNumbers));
        
        return existingBills.stream().collect(Collectors.toMap(
                Bill::getOrderNumber,
                b -> b,
                (b1, b2) -> b1.getBillTime().after(b2.getBillTime()) ? b1 : b2 // 存在重复订单号时取最新的
        ));
    }

    private void processIncomeTransaction(Long userId, AnalyzedTransactionVo tx, List<Object> createdObjects, Map<String, Bill> orderNumberMap) {
        if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // 优先从预加载的 Map 中获取
        Bill existingBill = null;
        if (StringUtils.hasText(tx.getOrderNumber())) {
            existingBill = orderNumberMap.get(tx.getOrderNumber());
        }
        
        // 如果没有预加载到，再按金额和备注查询（这种情况较少，且涉及模糊匹配）
        if (existingBill == null) {
            existingBill = findExistingBillByAmount(userId, tx);
        }

        if (existingBill != null) {
            updateExistingBill(existingBill, tx);
            createdObjects.add(billService.getBillDetail(existingBill.getId()));
        } else {
            if (tx.getCategoryId() == null) return;
            createdObjects.add(billService.createBill(convertToBillBo(tx, 2)));
        }
    }

    private Bill findExistingBillByAmount(Long userId, AnalyzedTransactionVo tx) {
        if (tx.getAmount() != null && (StringUtils.hasText(tx.getRemark()) || StringUtils.hasText(tx.getMerchant()))) {
             String searchKey = StringUtils.hasText(tx.getRemark()) ? tx.getRemark() : tx.getMerchant();
             return billMapper.selectOne(new QueryWrapper<Bill>()
                    .eq("user_id", userId)
                    .eq("original_amount", tx.getAmount()) 
                    .and(wrapper -> wrapper.like("remark", searchKey).or().like("merchant", searchKey))
                    .orderByDesc("bill_time")
                    .last("LIMIT 1"));
        }
        return null;
    }

    private void updateExistingBill(Bill existingBill, AnalyzedTransactionVo tx) {
        String newRemark = existingBill.getRemark();
        if (StringUtils.hasText(tx.getRemark()) && (newRemark == null || !newRemark.contains(tx.getRemark()))) {
            newRemark = (newRemark == null ? "" : newRemark + " | ") + "退款/收入: " + tx.getRemark();
            existingBill.setRemark(newRemark);
        }
        
        if (!StringUtils.hasText(existingBill.getMerchant()) && StringUtils.hasText(tx.getMerchant())) {
            existingBill.setMerchant(tx.getMerchant());
        }
        
        if (existingBill.getType() == 1) {
            BigDecimal currentRefund = existingBill.getRefundAmount() == null ? BigDecimal.ZERO : existingBill.getRefundAmount();
            BigDecimal refundAmt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            BigDecimal newRefund = currentRefund.add(refundAmt).min(existingBill.getOriginalAmount());
            
            existingBill.setRefundAmount(newRefund);
            existingBill.setAmount(existingBill.getOriginalAmount().subtract(newRefund));
        }
        existingBill.setUpdateTime(new Date());
        billMapper.updateById(existingBill);
    }
    
    @NotNull
    private BillBo convertToBillBo(AnalyzedTransactionVo tx, int type) {
        LocalDateTime time = tx.getTransactionTime() != null ? tx.getTransactionTime() : LocalDateTime.now();
        return new BillBo()
                .setOriginalAmount(tx.getAmount())
                .setType(type)
                .setRemark(tx.getRemark())
                .setMerchant(tx.getMerchant())
                .setCategoryId(tx.getCategoryId())
                .setSubCategoryId(tx.getSubCategoryId())
                .setOrderNumber(tx.getOrderNumber())
                .setBillTime(Date.from(time.atZone(ZoneId.systemDefault()).toInstant()));
    }
    
    private void processExpenseTransaction(Long userId, AnalyzedTransactionVo tx, List<Object> createdObjects) {
        if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) <= 0 || tx.getCategoryId() == null) {
            return;
        }

        if (Boolean.TRUE.equals(tx.getIsExpected())) {
            String remark = tx.getRemark();
            if (StringUtils.hasText(tx.getMerchant())) {
                remark = (StringUtils.hasText(remark) ? tx.getMerchant() + " | " + remark : tx.getMerchant());
            }
            
            LocalDateTime time = tx.getTransactionTime() != null ? tx.getTransactionTime() : LocalDateTime.now();
            
            ExpectedExpenseBo bo = new ExpectedExpenseBo()
                    .setUserId(userId)
                    .setAmount(tx.getAmount())
                    .setCategoryId(tx.getCategoryId())
                    .setSubCategoryId(tx.getSubCategoryId())
                    .setRemark(remark)
                    .setDueDate(Date.from(time.atZone(ZoneId.systemDefault()).toInstant()))
                    .setStatus(1);
            
            createdObjects.add(expectedExpenseService.create(bo));
        } else {
            createdObjects.add(billService.createBill(convertToBillBo(tx, 1)));
        }
    }

    /**
     * 从文本中提取金额
     * 
     * @param text 原始文本
     * @param tx 交易记录VO
     * @return 提取出的金额原始字符串（用于清理文本）
     */
    private String extractAmount(String text, AnalyzedTransactionVo tx) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        String lastMatch = null;
        // 查找最后一个匹配的金额（通常是总金额）
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        if (lastMatch != null) {
            BigDecimal amount = new BigDecimal(lastMatch);
            // 保持2位小数，第3位四舍五入
            tx.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        }
        return lastMatch;
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
     * 提取描述信息、商户信息和判断是否为预期交易
     * 优化：利用 HanLP 的词性分析区分商户（如“瑞幸” - nz/ni）和备注（如“午饭” - n）
     *
     * @param text 原始文本
     * @param tx 交易记录VO
     */
    private void extractDescriptionAndIntent(String text, AnalyzedTransactionVo tx) {
        // 使用HanLP进行分词及词性标注
        List<Term> terms = HanLP.segment(text);
        StringBuilder remarkBuilder = new StringBuilder();
        StringBuilder merchant = new StringBuilder();
        boolean isExpected = false;

        if (tx.getTransactionTime() != null) {
            if (tx.getTransactionTime().isAfter(LocalDateTime.now())) {
                isExpected = true;
            }
        }

        // 遍历分词结果
        for (Term term : terms) {
            String word = term.word;
            String nature = term.nature.toString(); // 词性

            // 1. 判断是否包含预期交易关键词
            if (word.matches(".*(打算|预计|计划|预备|准备|想要|想买|想喝|想吃|要去|将要|即将|明天|后天|下周|下月|明年|今晚|稍后|一会儿|待会|过后|之后).*")) {
                isExpected = true;
            }

            // 2. 过滤掉纯数字和货币单位
            if (word.matches("\\d+(\\.\\d+)?") ||
                    word.matches("[rRｗＲｒ円￥$＄元块kKｋＫwWｗＷ万元千元]+")) {
                continue;
            }

            // 3. 根据词性区分商户和备注
            // ni-机构名, ns-地名, nz-其他专名(品牌名常被识别为此类), nt-机构团体
            if (nature.startsWith("ni") || nature.startsWith("ns") || nature.startsWith("nz") || nature.startsWith("nt")) {
                merchant.append(word);
            } else {
                // 普通名词(n)、动词(v)、标点(w)等放入备注
                remarkBuilder.append(word);
            }
        }

        tx.setRemark(remarkBuilder.toString().trim());
        tx.setMerchant(merchant.toString().trim());
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
        String remark = tx.getRemark();
        String merchant = tx.getMerchant();
        
        // 组合搜索文本用于分类识别
        String searchText = (StringUtils.hasText(remark) ? remark : "") + 
                           (StringUtils.hasText(merchant) ? " " + merchant : "");
        searchText = searchText.trim();
        
        if (!StringUtils.hasText(searchText)) {
            return;
        }

        // 1. 优先使用本地关键词映射进行匹配（这是最精确的匹配方式）
        List<Term> terms = HanLP.segment(searchText);
        List<String> tokens = terms.stream().map(t -> t.word).toList();
        
        if (!tokens.isEmpty()) {
            List<CategoryKeyword> matches = tokens.stream()
                .map(keywordMap::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(CategoryKeyword::getWeight).reversed())
                .toList();
                    
            if (!matches.isEmpty()) {
                CategoryKeyword match = matches.get(0);
                fillCategoryInfo(tx, match, categories, subCategories);
                tx.setSource("KEYWORD_DB");
                // 本地识别成功后，简单优化商户/备注：如果匹配的关键词在备注中，且商户为空，则将其移动到商户
                refineLocalExtraction(tx, match.getKeyword());
                return;
            }
        }
        
        // 2. 其次匹配子分类名称
        String finalSearchText = searchText;
        Optional<SubCategory> subMatch = subCategories.stream()
                .filter(sc -> finalSearchText.contains(sc.getName()))
                .findFirst();
        
        if (subMatch.isPresent()) {
            tx.setSubCategoryId(subMatch.get().getId());
            tx.setSubCategoryName(subMatch.get().getName());
            tx.setCategoryId(subMatch.get().getCategoryId());
            categories.stream().filter(c -> c.getId().equals(subMatch.get().getCategoryId()))
                    .findFirst().ifPresent(c -> tx.setCategoryName(c.getName()));
            tx.setSource("DIRECT_MATCH_SUB");
            refineLocalExtraction(tx, subMatch.get().getName());
            return;
        }
        
        // 3. 再次匹配主分类名称
        Optional<Category> catMatch = categories.stream()
                .filter(c -> !"其他".equals(c.getName()) && finalSearchText.contains(c.getName()))
                .findFirst();
        
        if (catMatch.isPresent()) {
            tx.setCategoryId(catMatch.get().getId());
            tx.setCategoryName(catMatch.get().getName());
            tx.setSource("DIRECT_MATCH");
            refineLocalExtraction(tx, catMatch.get().getName());
            return;
        }

        // 4. 本地匹配失败后，使用 AI 智能分类和商户/备注分析
        try {
            // 构建分类名称列表
            List<String> categoryNames = new ArrayList<>();
            categories.forEach(c -> categoryNames.add(c.getName()));
            subCategories.forEach(sc -> categoryNames.add(sc.getName()));

            JSONObject aiResultObj = qwenUtil.classifyWithTime(searchText, categoryNames);
            if (aiResultObj != null) {
                String aiCategory = aiResultObj.getString("category");
                String aiDate = aiResultObj.getString("date");
                String aiMerchant = aiResultObj.getString("merchant");
                String aiRemark = aiResultObj.getString("remark");

                // AI 对商户和备注的识别非常准确，用于修正规则解析的结果
                if (StringUtils.hasText(aiMerchant)) {
                    tx.setMerchant(aiMerchant);
                }
                if (StringUtils.hasText(aiRemark)) {
                    tx.setRemark(aiRemark);
                } else if (StringUtils.hasText(aiMerchant) && tx.getRemark().equals(aiMerchant)) {
                    // 如果识别出的商户正是之前的备注，且 AI 认为没有额外备注，则清空备注
                    tx.setRemark("");
                }

                // 如果 AI 提取到了日期，且之前未解析出日期，则使用 AI 提取的日期
                if (StringUtils.hasText(aiDate) && tx.getTransactionTime() == null) {
                    try {
                        LocalDate date = LocalDate.parse(aiDate);
                        tx.setTransactionTime(date.atStartOfDay());
                    } catch (Exception ignored) {}
                }

                if (StringUtils.hasText(aiCategory)) {
                     // 查找匹配的子分类或主分类（AI 返回的可能是名称）
                     Optional<SubCategory> aiSubMatch = subCategories.stream()
                             .filter(sc -> sc.getName().equals(aiCategory)).findFirst();
                     if (aiSubMatch.isPresent()) {
                         tx.setSubCategoryId(aiSubMatch.get().getId());
                         tx.setSubCategoryName(aiSubMatch.get().getName());
                         tx.setCategoryId(aiSubMatch.get().getCategoryId());
                         categories.stream().filter(c -> c.getId().equals(aiSubMatch.get().getCategoryId()))
                                 .findFirst().ifPresent(c -> tx.setCategoryName(c.getName()));
                         saveNewKeyword(tx.getRemark(), aiSubMatch.get().getId(), aiSubMatch.get().getCategoryId());
                         tx.setSource("AI_QWEN_SUB");
                         return;
                     }
                     
                     Optional<Category> aiCatMatch = categories.stream()
                             .filter(c -> c.getName().equals(aiCategory)).findFirst();
                     if (aiCatMatch.isPresent()) {
                         tx.setCategoryId(aiCatMatch.get().getId());
                         tx.setCategoryName(aiCatMatch.get().getName());
                         saveNewKeyword(tx.getRemark(), null, aiCatMatch.get().getId());
                         tx.setSource("AI_QWEN");
                         return;
                     }
                }
            }
        } catch (Exception e) {
            log.error("AI 预分析失败", e);
        }

        // 5. 回退到默认分类"其他"
        tx.setCategoryName("其他");
        tx.setSource("FALLBACK");
        Optional<Category> otherCat = categories.stream()
                .filter(c -> "其他".equals(c.getName())).findFirst();
        otherCat.ifPresent(category -> tx.setCategoryId(category.getId()));
    }

    /**
     * 本地识别成功后优化商户/备注
     * 如果匹配到的关键词在备注中，且商户为空，则将关键词移动到商户，并从备注中清除
     */
    private void refineLocalExtraction(AnalyzedTransactionVo tx, String matchedWord) {
        if (!StringUtils.hasText(matchedWord)) return;
        
        String remark = tx.getRemark();
        String merchant = tx.getMerchant();
        
        // 如果备注中包含关键词，且商户为空，则考虑优化
        if (StringUtils.hasText(remark) && remark.contains(matchedWord) && !StringUtils.hasText(merchant)) {
            tx.setMerchant(matchedWord);
            // 从备注中移除关键词
            tx.setRemark(remark.replace(matchedWord, "").trim());
        }
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

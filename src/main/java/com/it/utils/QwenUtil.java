package com.it.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.it.config.AIConfig;
import com.it.greenfinance.pojo.SubCategory;
import com.it.greenfinance.pojo.vo.CategoryVo;
import com.it.greenfinance.service.CategoryService;
import com.it.greenfinance.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Qwen AI 模型工具类
 * 提供与阿里云通义千问大语言模型的交互功能
 * 主要包括文本分类、时间提取和财务建议生成等功能
 * 
 * @author Lingma
 * @version 2.0
 */
@Slf4j
@Component
public class QwenUtil {

    /**
     * AI 配置类，包含 API 地址、密钥等配置信息
     */
    @Autowired
    private AIConfig aiConfig;

    /**
     * 系统配置服务，用于从数据库获取配置
     */
    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 分类服务，用于获取用户分类信息
     */
    @Autowired
    private CategoryService categoryService;

    /**
     * OkHttp 客户端，用于发送 HTTP 请求到 AI 模型 API
     */
    private final OkHttpClient client;

    /**
     * 默认 API 基础地址
     */
//    private static final String DEFAULT_BASE_URL = "https://ms-ens-2c9319b7-9b0e.api-inference.modelscope.cn/v1";
    private static final String DEFAULT_BASE_URL = "https://api-inference.modelscope.cn/v1";
    /**
     * 默认模型 ID（Qwen/Qwen3-4B）
     */
    private static final String DEFAULT_MODEL_ID = "Qwen/Qwen3-4B";
    
    /**
     * Chat Completions API 路径
     */
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    
    /**
     * Markdown 代码块标记正则表达式
     */
    private static final Pattern MARKDOWN_CODE_BLOCK_PATTERN = Pattern.compile("^```(json)?|```$");
    
    /**
     * 解析 Markdown JSON 代码块的正则表达式
     */
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("(?s)```(?:json|JSON)?\\s*(.*?)\\s*```");

    /**
     * 构造函数，初始化 OkHttpClient 并设置超时时间
     * 使用单例模式确保客户端复用
     */
    public QwenUtil() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)  // 连接超时时间 30 秒
                .writeTimeout(60, TimeUnit.SECONDS)    // 写入超时时间 60 秒
                .readTimeout(90, TimeUnit.SECONDS)     // 读取超时时间 90 秒（AI 响应可能较慢）
                .retryOnConnectionFailure(true)        // 启用连接失败重试
                .build();
    }
    
    /**
     * 文本分类并提取时间方法
     * 将输入的文本分类到指定的类别列表中的某一类，并尝试提取时间
     *
     * @param text 待分类的文本描述
     * @param categories 可选的类别列表
     * @return 包含分类结果和时间的JSON对象，例如 {"category": "餐饮", "date": "2023-10-01"}
     */
    public JSONObject classifyWithTime(String text, List<String> categories) {
        // 检查AI功能是否开启
        if (isAiDisabled()) return null;

        // 将类别列表转换为字符串
        String categoriesStr = String.join(", ", categories);
        
        // 构建系统提示词（优化：更严谨的指令和示例）
        String systemPrompt = String.format("""
                你是一个精准的账单分析助手。请分析用户的交易描述并提取结构化信息。
                
                【任务目标】
                1. 分类：从下列类别中选择一个最合适的：[%s]。
                   重要规则：如果列表中包含具体的子分类（如"早餐"、"午餐"、"晚餐"、"房租"等），必须优先选择子分类，而不是宽泛的主分类（如"餐饮"、"居家"）。
                   只有当没有匹配的子分类时，才选择主分类。
                2. 日期：提取日期并转为 YYYY-MM-DD。若无明确日期，基于当前系统日期推断。
                3. 商户：识别交易对方（品牌/店名）。
                4. 备注：识别交易具体内容（如：午餐、打车费）。
                
                【约束条件】
                - 必须返回纯 JSON 格式，严禁包含 Markdown 标记、代码块或解释性文字。
                - 如果无法确定某字段，请返回 null。
                
                【输出示例】
                {"category": "午餐", "date": "2024-03-22", "merchant": "麦当劳", "remark": "巨无霸套餐"}
                """, categoriesStr);
        // 构建用户提示词
        String userPrompt = String.format("交易描述：\"%s\"", text);

        // 调用Qwen API并返回结果（提取任务使用低温度）
        String result = callQwenApi(systemPrompt, userPrompt, 0.1);

        if (result != null) {
            try {
                return JSON.parseObject(result);
            } catch (Exception e) {
                log.error("Error parsing classification result with time: {}", result, e);
            }
        }
        return null;
    }
    
    
    /**
     * 获取财务建议
     * 
     * @param spendingSummary 支出摘要
     * @return AI 生成的财务建议
     */
    public String getFinancialAdvice(String spendingSummary) {
        if (isAiDisabled()) {
            log.debug("AI 功能未开启，返回默认提示");
            return "AI Module is disabled.";
        }
        
        if (!StringUtils.hasText(spendingSummary)) {
            log.warn("getFinancialAdvice 输入参数为空");
            return "支出摘要不能为空";
        }

        // 构建系统提示词
        String systemPrompt = "你是一个贴心的理财小伙伴，要求通过支出摘要建立简单的用户画像。请以好朋友的口吻给出温暖、实际的建议。用轻松友好的语调给出 3 条简短建议，帮助用户更好地管理财务。避免称呼，请用亲切自然的语言，避免说教，每条建议控制在 20 字以内。";
        // 构建用户提示词
        String userPrompt = String.format("根据以下月度支出情况给出建议。摘要：%s", spendingSummary);
        
        // 调用 Qwen API 并返回结果（建议任务使用略高温度）
        return callQwenApi(systemPrompt, userPrompt, 0.7);
    }
    
    /**
     * 解析 OCR 文本，提取账单信息
     * 
     * @param userId 用户 ID
     * @param ocrTexts OCR 识别出的文本列表
     * @return JSON 格式的账单信息列表，包含：amount(金额), type(类型：1-支出/2-收入), 
     *         categoryId(主分类 ID), categoryName(主分类名称), subCategoryId(子分类 ID), 
     *         subCategoryName(子分类名称), categoryIcon(图标), billTime(账单时间), 
     *         remark(备注), merchant(商户), isExpected(是否预计支出)
     */
    public String parseOcrTexts(Long userId, List<String> ocrTexts) {
        if (isAiDisabled()) {
            log.warn("AI 功能未开启，无法解析 OCR 文本");
            return null;
        }
        
        if (ocrTexts == null || ocrTexts.isEmpty()) {
            log.warn("parseOcrTexts 输入参数为空");
            return null;
        }
        
        // 获取所有分类信息（主分类及子分类）
        List<CategoryVo> categoryVos = categoryService.getUserCategoriesWithSubCategories(userId, null);
        
        // 将分类列表转换为 AI 可理解的格式字符串
        StringBuilder categoriesBuilder = getCategoriesBuilder(categoryVos);
        
        // 构建系统提示词（优化：更精简和明确的指令）
        String systemPrompt = "你是一个专业 OCR 账单解析专家。请从 OCR 文本中提取每笔交易的信息。\n" +
                "【分类规则】\n" +
                "1. 必须从提供的分类列表中选择最合适的 ID。\n" +
                "2. 优先选择带 * 的子分类，找不到合适的再选带 - 的主分类。\n" +
                "【提取规则】\n" +
                "1. 金额（amount）：提取消费金额，统一为正数数字。\n" +
                "2. 类型（type）：1-支出，2-收入。\n" +
                "3. 时间（billTime）：格式 YYYY-MM-DD HH:mm:ss。**若 OCR 文本中没有明确的时间信息，必须使用当前系统时间作为默认值**。\n" +
                "4. 预计支出（isExpected）：若含有'计划'、'准备'等词，设为 true。\n" +
                "【返回要求】\n" +
                "- 必须返回纯 JSON 数组，严禁任何解释文本。\n" +
                "\n" +
                categoriesBuilder + "\n" +
                "【JSON 示例】\n" +
                "[{\"amount\": 30.0, \"type\": 1, \"categoryId\": 1, \"categoryName\": \"餐饮\", \"subCategoryId\": 10, \"subCategoryName\": \"晚餐\", \"categoryIcon\": \"res:food\", \"merchant\": \"肯德基\", \"billTime\": \"2024-03-22 18:30:00\", \"remark\": \"吮指原味鸡\", \"isExpected\": false}]";
        
        // 构建用户提示词
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("请解析以下 OCR 文本，提取账单信息：\n");
        for (int i = 0; i < ocrTexts.size(); i++) {
            userPromptBuilder.append(i + 1).append(". ").append(ocrTexts.get(i)).append("\n");
        }
        
        // 调用 Qwen API 并返回结果（解析任务使用低温度）
        String result = callQwenApi(systemPrompt, userPromptBuilder.toString(), 0.1);
        
        if (result != null) {
            try {
                // 验证返回的 JSON 是否有效
                JSON.parseArray(result);
                return result;
            } catch (Exception e) {
                log.error("解析 OCR 文本失败: {}", result, e);
            }
        }
        
        return null;
    }
    
    @NotNull
    private StringBuilder getCategoriesBuilder(List<CategoryVo> categoryVos) {
        StringBuilder categoriesBuilder = new StringBuilder();
        categoriesBuilder.append("可用分类列表（层级结构）：\n");
        for (CategoryVo cat : categoryVos) {
            String catType = (cat.getType() == 1 ? "支出" : "收入");
            categoriesBuilder.append(String.format("- [%s] %s (ID: %d, Icon: %s)\n", catType, cat.getName(), cat.getId(), cat.getCategoryIcon()));
            if (cat.getSubCategories() != null && !cat.getSubCategories().isEmpty()) {
                for (SubCategory sub : cat.getSubCategories()) {
                    String subIcon = sub.getCategoryIcon() != null ? sub.getCategoryIcon() : cat.getCategoryIcon();
                    categoriesBuilder.append(String.format("  * %s (ID: %d, ParentID: %d, Icon: %s)\n", sub.getName(), sub.getId(), cat.getId(), subIcon));
                }
            }
        }
        return categoriesBuilder;
    }
    
    
    /**
     * 检查 AI 功能是否未启用（禁用状态）
     *
     * @return 如果 AI 功能未开启或配置为禁用返回 true，否则返回 false
     */
    private boolean isAiDisabled() {
        Boolean enabled = aiConfig.getModelEnabled();
        return enabled == null || !enabled;
    }

    /**
     * 调用 Qwen API 的核心方法（重构版）
     * 构造请求参数并发送 HTTP POST 请求到 Qwen API
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @param temperature 温度参数（0.0-2.0），用于控制结果的确定性或创造性
     * @return AI 模型的响应结果；发生错误时返回 null
     */
    private String callQwenApi(String systemPrompt, String userPrompt, double temperature) {
        // 在 System Prompt 中注入当前时间信息，增强 AI 的时间感知能力（使用中国大陆时区）
        String timeAwareSystemPrompt = systemPrompt + String.format(" 当前系统日期是：%s。", java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")));
        
        // 获取 API Key（优先数据库，回退配置文件）
        String apiKey = getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            log.error("AI API Key 缺失，请配置");
            return null;
        }

        // 构造消息数组
        JSONArray messages = new JSONArray();
        
        // System Message
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", timeAwareSystemPrompt);
        messages.add(systemMessage);
        
        // User Message
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        // 构造请求体
        JSONObject requestBody = buildRequestBody(messages, temperature);

        // 确定 API URL
        String apiUrl = buildChatCompletionsUrl(aiConfig.getModelApiUrl());

        // 重试逻辑
        int maxRetries = 3;
        int retryDelayMs = 1000;

        for (int i = 0; i <= maxRetries; i++) {
            try {
                Request request = createHttpRequest(apiUrl, apiKey, requestBody);
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return handleResponse(response);
                    }
                    
                    int code = response.code();
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.warn("AI API 调用失败 (尝试 {}/{}): code={}, body={}", i + 1, maxRetries + 1, code, errorBody);
                    
                    // 只有特定错误码才重试（如 429 频率限制，或 5xx 服务器错误）
                    if (code != 429 && code < 500) {
                        break; 
                    }
                }
            } catch (IOException e) {
                log.error("调用 AI API 异常 (尝试 {}/{}): {}", i + 1, maxRetries + 1, e.getMessage());
            }

            if (i < maxRetries) {
                try {
                    Thread.sleep(retryDelayMs * (long) Math.pow(2, i)); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 获取 API Key（优先数据库，回退配置文件）
     * 
     * @return API Key
     */
    private String getApiKey() {
        String apiKey = null;
        try {
            // 1. 尝试从数据库获取
            apiKey = systemConfigService.getConfigValue("ai.model.api-key");
        } catch (Exception e) {
            log.warn("从数据库获取 API Key 失败，回退到配置文件", e);
        }
        
        // 2. 如果数据库中没有，回退到配置文件
        if (!StringUtils.hasText(apiKey)) {
            apiKey = aiConfig.getModelApiKey();
        }
        
        return apiKey;
    }
    
    /**
     * 构建请求体
     * 
     * @param messages 消息数组
     * @param temperature 温度
     * @return 请求体 JSON 对象
     */
    private JSONObject buildRequestBody(JSONArray messages, double temperature) {
        JSONObject requestBody = new JSONObject();
        String modelId = StringUtils.hasText(aiConfig.getModelId()) ? aiConfig.getModelId().trim() : DEFAULT_MODEL_ID;
        requestBody.put("model", modelId);
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        boolean streamEnabled = Boolean.TRUE.equals(aiConfig.getStreamEnabled());
        requestBody.put("stream", streamEnabled);
        return requestBody;
    }
    
    /**
     * 创建 HTTP 请求
     * 
     * @param apiUrl API 地址
     * @param apiKey API Key
     * @param requestBody 请求体
     * @return Request 对象
     */
    private Request createHttpRequest(String apiUrl, String apiKey, JSONObject requestBody) {
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestBody.toJSONString());
        
        return new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
    }
    
    /**
     * 处理 HTTP 响应
     * 
     * @param response HTTP 响应
     * @return 响应内容
     * @throws IOException IO 异常
     */
    private String handleResponse(Response response) throws IOException {
        if (response.body() == null) {
            return null;
        }

        // 判断是否需要处理流式响应
        if (Boolean.TRUE.equals(aiConfig.getStreamEnabled())) {
            return parseStreamResponseToContent(response.body());
        }

        // 处理普通响应
        String responseStr = response.body().string();
        JSONObject jsonResponse = JSON.parseObject(responseStr);
        JSONArray choices = jsonResponse.getJSONArray("choices");
        
        if (choices != null && !choices.isEmpty()) {
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            if (message != null) {
                String content = message.getString("content");
                return cleanResponse(content);
            }
        }
        
        return null;
    }

    private String buildChatCompletionsUrl(String configuredUrl) {
        String baseUrl = DEFAULT_BASE_URL;
        if (StringUtils.hasText(configuredUrl) && configuredUrl.trim().startsWith("http")) {
            baseUrl = configuredUrl.trim();
        }
        baseUrl = baseUrl.replace("`", "").trim();
        if (baseUrl.contains(CHAT_COMPLETIONS_PATH)) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + CHAT_COMPLETIONS_PATH;
    }

    private String parseStreamResponseToContent(ResponseBody responseBody) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = responseBody.source().readUtf8Line()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith(":")) {
                continue;
            }
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            String data = trimmed.substring(5).trim();
            if ("[DONE]".equals(data)) {
                break;
            }
            JSONObject chunkJson;
            try {
                chunkJson = JSON.parseObject(data);
            } catch (Exception ignored) {
                continue;
            }
            JSONArray choices = chunkJson.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                continue;
            }
            JSONObject choice0 = choices.getJSONObject(0);
            JSONObject delta = choice0.getJSONObject("delta");
            if (delta == null) {
                continue;
            }
            String part = delta.getString("content");
            if (part != null) {
                sb.append(part);
            }
        }
        return cleanResponse(sb.toString());
    }

    /**
     * 清理 AI 模型响应内容
     * 去除首尾空格、多余引号和 Markdown 代码块标记
     * 
     * @param content 原始响应内容
     * @return 清理后的响应内容
     */
    private String cleanResponse(String content) {
        if (content == null) {
            return null;
        }
            
        // 去除首尾空格
        content = content.trim();
            
        // 1. 处理 Markdown 代码块标记 (例如 ```json ... ```)
        // 匹配 ```json, ```JSON, ``` 或单独的 ```
        if (content.contains("```")) {
            // 尝试匹配中间的内容
            java.util.regex.Matcher matcher = JSON_BLOCK_PATTERN.matcher(content);
            if (matcher.find()) {
                content = matcher.group(1).trim();
            } else {
                // 如果没有成对出现，尝试直接移除标记
                content = MARKDOWN_CODE_BLOCK_PATTERN.matcher(content).replaceAll("").trim();
            }
        }
            
        // 2. 去除首尾的引号 (有些模型会给结果加引号)
        content = content.replaceAll("^\"|\"$", "").trim();
        return content;
    }
}

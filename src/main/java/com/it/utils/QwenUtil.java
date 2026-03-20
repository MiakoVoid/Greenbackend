package com.it.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.it.config.AIConfig;
import com.it.greenfinance.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.Arrays;
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
     * OkHttp 客户端，用于发送 HTTP 请求到 AI 模型 API
     */
    private final OkHttpClient client;

    /**
     * 默认 API 基础地址
     */
    private static final String DEFAULT_BASE_URL = "https://api-inference.modelscope.cn/v1/";
    
    /**
     * 默认模型 ID（Qwen2.5-7B-Instruct）
     */
//    private static final String DEFAULT_MODEL_ID = "Qwen/Qwen2.5-7B-Instruct";
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
     * 文本分类方法（兼容旧接口，内部调用 classifyWithTime）
     * 将输入的文本分类到指定的类别列表中的某一类
     *
     * @param text 待分类的文本描述
     * @param categories 可选的类别列表
     * @return 分类结果，即匹配的类别名称；如果 AI 功能未启用或发生错误则返回 null
     */
    public String classify(String text, List<String> categories) {
        if (!StringUtils.hasText(text) || categories == null || categories.isEmpty()) {
            log.warn("classify 输入参数无效：text={}, categories={}", text, categories);
            return null;
        }
            
        JSONObject result = classifyWithTime(text, categories);
        if (result != null) {
            return result.getString("category");
        }
        return null;
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
        // 检查AI功能是否启用
        if (!isAiEnabled()) return null;

        // 将类别列表转换为字符串
        String categoriesStr = String.join(", ", categories);
        
        // 构建系统提示词
        String systemPrompt = String.format("你是一个智能助手。请分析用户的描述。\n" +
                "1. 将描述准确分类到下列类别之一：[%s]。\n" +
                "2. 提取描述中的日期/时间信息，并根据当前时间转换为具体的日期格式（YYYY-MM-DD）。\n" +
                "请严格按照以下JSON格式返回结果，不要输出其他任何内容：\n" +
                "{\"category\": \"类别名称\", \"date\": \"YYYY-MM-DD\"}。如果未提及时间，date字段请返回null。", categoriesStr);
        // 构建用户提示词
        String userPrompt = String.format("描述：\"%s\"", text);

        // 调用Qwen API并返回结果
        String result = callQwenApi(systemPrompt, userPrompt);
        
        if (result != null) {
            try {
                // 清理可能存在的 Markdown 代码块标记
                if (result.startsWith("```json")) {
                    result = result.substring(7);
                    if (result.endsWith("```")) {
                        result = result.substring(0, result.length() - 3);
                    }
                }
                result = result.trim();
                return JSON.parseObject(result);
            } catch (Exception e) {
                log.error("Error parsing classification result with time", e);
            }
        }
        return null;
    }

    /**
     * 批量文本分类方法
     * 将输入的多个文本分别分类到指定的类别列表中的某一类
     *
     * @param texts 待分类的文本描述列表
     * @param categories 可选的类别列表
     * @return 分类结果列表，每个元素是对应的分类结果；如果AI功能未启用或发生错误则返回null
     */
    public List<String> batchClassify(List<String> texts, List<String> categories) {
        // 检查AI功能是否启用
        if (!isAiEnabled()) return null;
        
        // 将类别列表转换为字符串
        String categoriesStr = String.join(", ", categories);
        
        // 构建系统提示词
        String systemPrompt = String.format("你是一个智能分类助手。请将以下每条描述分别准确分类到下列类别之一：[%s]。\n" +
                "请严格按照以下JSON格式返回结果，不要输出其他任何内容：\n" +
                "[{\"index\": 0, \"category\": \"分类1\"}, {\"index\": 1, \"category\": \"分类2\"}]", categoriesStr);
        
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("需要分类的描述如下：\n");
        for (int i = 0; i < texts.size(); i++) {
            userPromptBuilder.append(i).append(". \"").append(texts.get(i)).append("\"\n");
        }
        
        // 调用Qwen API并返回结果
        String result = callQwenApi(systemPrompt, userPromptBuilder.toString());
        
        if (result != null) {
            try {
                // 解析返回的JSON数组
                // 有时候模型可能返回 ```json ... ```，需要清理
                if (result.startsWith("```json")) {
                    result = result.substring(7);
                    if (result.endsWith("```")) {
                        result = result.substring(0, result.length() - 3);
                    }
                }
                result = result.trim();
                
                JSONArray jsonArray = JSON.parseArray(result);
                String[] results = new String[texts.size()];
                
                // 按照索引填充结果
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    int index = obj.getIntValue("index");
                    String category = obj.getString("category");
                    if (index >= 0 && index < results.length) {
                        results[index] = category;
                    }
                }
                
                return Arrays.asList(results);
            } catch (Exception e) {
                log.error("Error parsing batch classification result", e);
            }
        }
        
        return null;
    }
    
    
    /**
     * 获取财务建议
     * 根据支出摘要生成个性化的财务建议
     *
     * @param spendingSummary 支出摘要信息
     * @return 财务建议文本；如果 AI 功能未启用则返回提示信息
     */
    public String getFinancialAdvice(String spendingSummary) {
        if (!isAiEnabled()) {
            log.debug("AI 功能未启用，返回默认提示");
            return "AI Module is disabled.";
        }
        
        if (!StringUtils.hasText(spendingSummary)) {
            log.warn("getFinancialAdvice 输入参数为空");
            return "支出摘要不能为空";
        }

        // 构建系统提示词
        String systemPrompt = "你是一个贴心的理财小伙伴，请以朋友的口吻给出温暖的建议。用轻松友好的语调给出 3 条简短建议，帮助用户更好地管理财务。请用亲切自然的语言，避免说教，每条建议控制在 20 字以内。";
        // 构建用户提示词
        String userPrompt = String.format("根据以下月度支出情况给出建议。摘要：%s", spendingSummary);
        
        // 调用 Qwen API 并返回结果
        return callQwenApi(systemPrompt, userPrompt);
    }

    /**
     * 检查 AI 功能是否启用
     *
     * @return 如果 AI 功能启用返回 true，否则返回 false
     */
    private boolean isAiEnabled() {
        Boolean enabled = aiConfig.getModelEnabled();
        return enabled != null && enabled;
    }

    /**
     * 调用 Qwen API 的核心方法（重构版）
     * 构造请求参数并发送 HTTP POST 请求到 Qwen API
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return AI 模型的响应结果；发生错误时返回 null
     */
    private String callQwenApi(String systemPrompt, String userPrompt) {
        // 在 System Prompt 中注入当前时间信息，增强 AI 的时间感知能力
        String timeAwareSystemPrompt = systemPrompt + String.format(" 当前系统日期是：%s。", java.time.LocalDate.now());
        
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
        JSONObject requestBody = buildRequestBody(messages);

        // 确定 API URL
        String apiUrl = buildChatCompletionsUrl(aiConfig.getModelApiUrl());

        // 创建 HTTP 请求
        Request request = createHttpRequest(apiUrl, apiKey, requestBody);

        Response response = null;
        try {
            response = client.newCall(request).execute();
            return handleResponse(response);
        } catch (IOException e) {
            log.error("调用 AI API 异常", e);
            return null;
        } finally {
            if (response != null) {
                response.close();
            }
        }
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
     * @return 请求体 JSON 对象
     */
    private JSONObject buildRequestBody(JSONArray messages) {
        JSONObject requestBody = new JSONObject();
        String modelId = StringUtils.hasText(aiConfig.getModelId()) ? aiConfig.getModelId().trim() : DEFAULT_MODEL_ID;
        requestBody.put("model", modelId);
        requestBody.put("messages", messages);
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
        // 检查响应是否成功
        if (!response.isSuccessful()) {
            String errorBody = "";
            if (response.body() != null) {
                errorBody = response.body().string();
            }
            log.error("AI API 调用失败：code={}, body={}", response.code(), errorBody);
            return null;
        }

        // 判断是否需要处理流式响应
        if (Boolean.TRUE.equals(aiConfig.getStreamEnabled())) {
            return parseStreamResponseToContent(Objects.requireNonNull(response.body()));
        }

        // 处理普通响应
        String responseStr = Objects.requireNonNull(response.body()).string();
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
            
        // 去除 Markdown 代码块标记
        content = MARKDOWN_CODE_BLOCK_PATTERN.matcher(content).replaceAll("");
        content = content.trim();
            
        // 去除首尾的引号
        content = content.replaceAll("^\"|\"$", "");
            
        return content;
    }
}

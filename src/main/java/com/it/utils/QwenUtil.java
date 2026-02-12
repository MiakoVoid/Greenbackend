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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Qwen AI模型工具类
 * 提供与阿里云通义千问大语言模型的交互功能
 * 主要包括文本分类和财务建议生成两大功能
 */
@Slf4j
@Component
public class QwenUtil {

    /**
     * AI配置类，包含API地址、密钥等配置信息
     */
    @Autowired
    private AIConfig aiConfig;

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * OkHttp客户端，用于发送HTTP请求到AI模型API
     */
    private final OkHttpClient client;

    private static final String DEFAULT_BASE_URL = "https://api-inference.modelscope.cn/v1/";
    private static final String MODEL_ID = "Qwen/Qwen2.5-7B-Instruct";

    /**
     * 构造函数，初始化OkHttpClient并设置超时时间
     */
    public QwenUtil() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)  // 连接超时时间30秒
                .writeTimeout(60, TimeUnit.SECONDS)    // 写入超时时间60秒
                .readTimeout(60, TimeUnit.SECONDS)     // 读取超时时间60秒
                .build();
    }

    /**
     * 文本分类方法（兼容旧接口，内部调用classifyWithTime）
     * 将输入的文本分类到指定的类别列表中的某一类
     *
     * @param text 待分类的文本描述
     * @param categories 可选的类别列表
     * @return 分类结果，即匹配的类别名称；如果AI功能未启用或发生错误则返回null
     */
    public String classify(String text, List<String> categories) {
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
     * @return 财务建议文本；如果AI功能未启用则返回提示信息
     */
    public String getFinancialAdvice(String spendingSummary) {
        // 检查AI功能是否启用
        if (!isAiEnabled()) return "AI Module is disabled.";

        // 构建系统提示词
        String systemPrompt = "你是一个贴心的理财小伙伴，请以朋友的口吻给出温暖的建议。用轻松友好的语调给出3条简短建议，帮助用户更好地管理财务。请用亲切自然的语言，避免说教，每条建议控制在20字以内。";
        // 构建用户提示词
        String userPrompt = String.format("根据以下月度支出情况给出建议。摘要：%s", spendingSummary);
        
        // 调用Qwen API并返回结果
        return callQwenApi(systemPrompt, userPrompt);
    }

    /**
     * 检查AI功能是否启用
     *
     * @return 如果AI功能启用返回true，否则返回false
     */
    private boolean isAiEnabled() {
        return aiConfig.getModelEnabled() != null && aiConfig.getModelEnabled();
    }

    /**
     * 调用Qwen API的核心方法
     * 构造请求参数并发送HTTP POST请求到Qwen API
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return AI模型的响应结果；发生错误时返回null
     */
    private String callQwenApi(String systemPrompt, String userPrompt) {
        // 在System Prompt中注入当前时间信息，增强AI的时间感知能力
        String timeAwareSystemPrompt = systemPrompt + String.format(" 当前系统日期是：%s。", java.time.LocalDate.now());
        
        // 获取API Key
        String apiKey = null;
        try {
            // 1. 尝试从数据库获取
            apiKey = systemConfigService.getConfigValue("ai.model.api-key");
        } catch (Exception e) {
            log.warn("Failed to get API key from database, falling back to config file", e);
        }
        
        // 2. 如果数据库中没有，回退到配置文件
        if (!StringUtils.hasText(apiKey)) {
            apiKey = aiConfig.getModelApiKey();
        }
        
        if (!StringUtils.hasText(apiKey)) {
            log.error("AI API Key is missing");
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
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL_ID);  // 使用完整的ModelScope模型ID
        requestBody.put("messages", messages);
        requestBody.put("stream", false);    // 显式设置stream=false

        // 确定API URL
        String apiUrl = DEFAULT_BASE_URL;
        if (StringUtils.hasText(aiConfig.getModelApiUrl()) && aiConfig.getModelApiUrl().startsWith("http")) {
            apiUrl = aiConfig.getModelApiUrl();
        }

        // 创建HTTP请求体
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestBody.toJSONString());
        
        // 构造HTTP请求
        Request request = new Request.Builder()
                .url(apiUrl)                                     // 使用确定后的API地址
                .addHeader("Authorization", "Bearer " + apiKey)  // 认证头
                .addHeader("Content-Type", "application/json")   // 内容类型
                .post(body)                                      // POST方法
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 检查响应是否成功
            if (!response.isSuccessful()) {
                log.error("AI API call failed: code={}, body={}", response.code(), response.body() != null ? response.body().string() : "");
                return null;
            }

            // 解析响应内容
            String responseStr = response.body().string();
            JSONObject jsonResponse = JSON.parseObject(responseStr);
            
            // OpenAI API 格式响应
            JSONArray choices = jsonResponse.getJSONArray("choices");
            
            // 提取AI回复内容
            if (choices != null && !choices.isEmpty()) {
                String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
                return cleanResponse(content);  // 清理响应内容并返回
            }
        } catch (IOException e) {
            log.error("Error calling AI API", e);
        }
        return null;
    }

    /**
     * 清理AI模型响应内容
     * 去除首尾空格和多余的引号
     * 
     * @param content 原始响应内容
     * @return 清理后的响应内容
     */
    private String cleanResponse(String content) {
        if (content == null) return null;
        return content.trim().replaceAll("^\"|\"$", "");  // 去除首尾空格和引号
    }
}
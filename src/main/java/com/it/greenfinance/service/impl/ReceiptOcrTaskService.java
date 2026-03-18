package com.it.greenfinance.service.impl;

import com.it.config.OcrProperties;
import com.it.greenfinance.pojo.vo.AnalyzedTransactionVo;
import com.it.greenfinance.pojo.vo.ReceiptOcrImageResultVo;
import com.it.greenfinance.pojo.vo.ReceiptOcrTaskVo;
import com.it.greenfinance.service.AIService;
import com.it.utils.ImagePreprocessor;
import com.it.utils.QwenUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReceiptOcrTaskService {

    private final OcrProperties ocrProperties;
    private final QiniuStorageService qiniuStorageService;
    private final QwenUtil qwenUtil;
    private final AIService aiService;

    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(Math.max(4, Math.min(8, Runtime.getRuntime().availableProcessors() * 2)));
    private final ExecutorService ocrExecutor = Executors.newFixedThreadPool(Math.max(4, Math.min(16, Runtime.getRuntime().availableProcessors() * 4)));
    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();

    public ReceiptOcrTaskService(OcrProperties ocrProperties, QiniuStorageService qiniuStorageService, QwenUtil qwenUtil, AIService aiService) {
        this.ocrProperties = ocrProperties;
        this.qiniuStorageService = qiniuStorageService;
        this.qwenUtil = qwenUtil;
        this.aiService = aiService;
    }

    public String createTask(Long userId, List<MultipartFile> files) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("请选择图片");
        }
        if (!Boolean.TRUE.equals(ocrProperties.getEnabled())) {
            throw new IllegalStateException("OCR功能未启用");
        }
        if (files.size() > 10) {
            throw new IllegalArgumentException("一次最多上传10张图片");
        }

        String taskId = UUID.randomUUID().toString().replace("-", "");
        TaskState state = new TaskState();
        state.setTaskId(taskId);
        state.setUserId(userId);
        state.setStatus("PENDING");
        state.setCreateTime(LocalDateTime.now());
        state.setTotal(files.size());
        state.setEmitters(Collections.synchronizedList(new ArrayList<>()));

        List<ImageInput> inputs = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            validateImage(file);
            try {
                ImageInput input = new ImageInput();
                input.setIndex(i);
                input.setOriginalFilename(file.getOriginalFilename());
                input.setContentType(file.getContentType());
                input.setBytes(file.getBytes());
                inputs.add(input);
            } catch (IOException e) {
                throw new IllegalArgumentException("读取图片失败: " + (file.getOriginalFilename() == null ? "" : file.getOriginalFilename()));
            }
        }
        state.setInputs(inputs);
        state.setImages(inputs.stream().map(in -> {
            ReceiptOcrImageResultVo vo = new ReceiptOcrImageResultVo();
            vo.setIndex(in.getIndex());
            vo.setOriginalFilename(in.getOriginalFilename());
            vo.setStatus("PENDING");
            return vo;
        }).collect(Collectors.toList()));

        tasks.put(taskId, state);
        CompletableFuture.runAsync(() -> runTask(state), taskExecutor);
        return taskId;
    }

    public ReceiptOcrTaskVo getTask(Long userId, String taskId) {
        TaskState state = tasks.get(taskId);
        if (state == null) {
            return null;
        }
        if (userId == null || !userId.equals(state.getUserId())) {
            throw new IllegalArgumentException("无权访问任务");
        }
        return state.toVo();
    }

    public SseEmitter subscribe(Long userId, String taskId) {
        TaskState state = tasks.get(taskId);
        if (state == null) {
            return null;
        }
        if (userId == null || !userId.equals(state.getUserId())) {
            throw new IllegalArgumentException("无权访问任务");
        }
        SseEmitter emitter = new SseEmitter(0L);
        state.getEmitters().add(emitter);
        emitter.onCompletion(() -> state.getEmitters().remove(emitter));
        emitter.onTimeout(() -> state.getEmitters().remove(emitter));
        push(state, "snapshot");
        return emitter;
    }

    private void runTask(TaskState state) {
        state.setStatus("RUNNING");
        push(state, "start");

        List<CompletableFuture<Void>> futures = state.getInputs().stream()
                .map(input -> CompletableFuture.runAsync(() -> processSingleImage(state, input), taskExecutor)
                        .whenComplete((v, ex) -> push(state, "progress")))
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        state.setFinishTime(LocalDateTime.now());
        if (state.getFailed().get() > 0 && state.getCompleted().get() > 0) {
            state.setStatus("PARTIAL");
        } else if (state.getFailed().get() > 0) {
            state.setStatus("FAILED");
        } else {
            state.setStatus("SUCCEEDED");
        }
        state.getProgress().set(100);
        push(state, "finish");
        completeEmitters(state);
    }

    private void processSingleImage(TaskState state, ImageInput input) {
        ReceiptOcrImageResultVo imageResult = state.getImages().get(input.getIndex());
        imageResult.setStatus("UPLOADING");
        push(state, "image");

        QiniuStorageService.UploadResult uploaded;
        try {
            uploaded = qiniuStorageService.upload(input.getBytes(), input.getOriginalFilename(), input.getContentType());
        } catch (Exception e) {
            imageResult.setStatus("FAILED");
            imageResult.setErrorMessage(e.getMessage());
            state.getFailed().incrementAndGet();
            updateProgress(state);
            return;
        }

        imageResult.setImageKey(uploaded.getKey());
        imageResult.setImageUrl(uploaded.getUrl());
        imageResult.setStatus("OCRING");
        push(state, "image");

        List<String> ocrLines = ocrWithRetry(uploaded.getUrl(), input);
        if (ocrLines == null) {
            imageResult.setStatus("FAILED");
            imageResult.setErrorMessage("OCR识别失败");
            state.getFailed().incrementAndGet();
            updateProgress(state);
            return;
        }
        imageResult.setOcrTexts(ocrLines);

        imageResult.setStatus("PARSING");
        push(state, "image");
        List<AnalyzedTransactionVo> drafts;
        try {
            drafts = aiService.analyzeOcrTexts(state.getUserId(), ocrLines);
        } catch (Exception e) {
            imageResult.setStatus("FAILED");
            imageResult.setErrorMessage("解析失败: " + e.getMessage());
            state.getFailed().incrementAndGet();
            updateProgress(state);
            return;
        }

        imageResult.setDrafts(drafts);
        imageResult.setStatus("SUCCEEDED");
        state.getCompleted().incrementAndGet();
        updateProgress(state);
    }

    private List<String> ocrWithRetry(String imageUrl, ImageInput input) {
        int maxRetries = ocrProperties.getMaxRetries() == null ? 0 : ocrProperties.getMaxRetries();
        int attempts = Math.max(1, 1 + maxRetries);

        String currentUrl = imageUrl;
        for (int i = 0; i < attempts; i++) {
            if (i > 0) {
                byte[] preprocessed = ImagePreprocessor.preprocessForOcr(input.getBytes(), input.getOriginalFilename());
                try {
                    QiniuStorageService.UploadResult uploaded = qiniuStorageService.upload(preprocessed, input.getOriginalFilename(), input.getContentType());
                    currentUrl = uploaded.getUrl();
                } catch (Exception e) {
                    return null;
                }
            }

            List<String> lines = timedOcr(currentUrl);
            if (lines != null && !lines.isEmpty()) return lines;
        }
        return null;
    }

    private List<String> timedOcr(String imageUrl) {
        int timeoutMs = ocrProperties.getRequestTimeoutMs() == null ? 1500 : ocrProperties.getRequestTimeoutMs();
        CompletableFuture<List<String>> f = CompletableFuture.supplyAsync(() -> qwenUtil.ocrLinesFromImageUrl(imageUrl), ocrExecutor);
        try {
            List<String> lines = f.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (lines == null || lines.isEmpty()) {
                return null;
            }
            return lines;
        } catch (Exception e) {
            f.cancel(true);
            return null;
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片");
        }
        if (file.getSize() > 6 * 1024 * 1024) {
            throw new IllegalArgumentException("图片大小不能超过6MB");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new IllegalArgumentException("无法识别图片类型");
        }
        String ct = contentType.toLowerCase();
        if (!ct.contains("jpeg") && !ct.contains("jpg") && !ct.contains("png")) {
            throw new IllegalArgumentException("只支持JPEG/PNG格式");
        }
    }

    private void updateProgress(TaskState state) {
        int done = state.getCompleted().get() + state.getFailed().get();
        int total = state.getTotal() == null || state.getTotal() == 0 ? 1 : state.getTotal();
        int p = Math.min(99, (int) Math.floor(done * 100.0 / total));
        state.getProgress().set(p);
    }

    private void push(TaskState state, String event) {
        ReceiptOcrTaskVo payload = state.toVo();
        List<SseEmitter> emitters = new ArrayList<>(state.getEmitters());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(event).data(payload));
            } catch (Exception e) {
                state.getEmitters().remove(emitter);
            }
        }
    }

    private void completeEmitters(TaskState state) {
        List<SseEmitter> emitters = new ArrayList<>(state.getEmitters());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
        state.getEmitters().clear();
    }

    @Data
    private static class ImageInput {
        private Integer index;
        private String originalFilename;
        private String contentType;
        private byte[] bytes;
    }

    @Data
    private static class TaskState {
        private String taskId;
        private Long userId;
        private String status;
        private Integer total;
        private AtomicInteger completed = new AtomicInteger(0);
        private AtomicInteger failed = new AtomicInteger(0);
        private AtomicInteger progress = new AtomicInteger(0);
        private LocalDateTime createTime;
        private LocalDateTime finishTime;
        private List<ImageInput> inputs;
        private List<ReceiptOcrImageResultVo> images;
        private List<SseEmitter> emitters;

        private ReceiptOcrTaskVo toVo() {
            ReceiptOcrTaskVo vo = new ReceiptOcrTaskVo();
            vo.setTaskId(taskId);
            vo.setStatus(status);
            vo.setTotal(total);
            vo.setCompleted(completed.get());
            vo.setFailed(failed.get());
            vo.setProgress(progress.get());
            vo.setCreateTime(createTime);
            vo.setFinishTime(finishTime);
            vo.setImages(images);
            return vo;
        }
    }
}

package com.it.greenfinance.service.impl;

import com.it.config.QiniuProperties;
import com.it.greenfinance.service.SystemConfigService;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class QiniuStorageService {

    private final QiniuProperties qiniuProperties;
    private final SystemConfigService systemConfigService;
    private final UploadManager uploadManager;

    public QiniuStorageService(QiniuProperties qiniuProperties, SystemConfigService systemConfigService) {
        this.qiniuProperties = qiniuProperties;
        this.systemConfigService = systemConfigService;
        this.uploadManager = new UploadManager(new Configuration(Region.autoRegion()));
    }

    public UploadResult upload(byte[] data, String originalFilename, String contentType) {
        if (!Boolean.TRUE.equals(qiniuProperties.getEnabled())) {
            throw new IllegalStateException("七牛云未启用");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("文件内容为空");
        }

        String accessKey = getConfigFirstNonBlank("qiniu.access-key", qiniuProperties.getAccessKey());
        String secretKey = getConfigFirstNonBlank("qiniu.secret-key", qiniuProperties.getSecretKey());
        String bucket = getConfigFirstNonBlank("qiniu.bucket", qiniuProperties.getBucket());
        String domain = getConfigFirstNonBlank("qiniu.domain", qiniuProperties.getDomain());
        String prefix = StringUtils.hasText(qiniuProperties.getPrefix()) ? qiniuProperties.getPrefix() : "receipts/";

        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey) || !StringUtils.hasText(bucket) || !StringUtils.hasText(domain)) {
            throw new IllegalStateException("七牛云配置不完整");
        }

        String ext = inferExt(originalFilename, contentType);
        String datePath = LocalDate.now().toString().replace("-", "");
        String key = normalizePrefix(prefix) + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);
        try {
            uploadManager.put(data, key, upToken);
        } catch (Exception e) {
            throw new RuntimeException("上传七牛云失败: " + e.getMessage(), e);
        }

        String url = buildUrl(domain, key);
        UploadResult result = new UploadResult();
        result.setKey(key);
        result.setUrl(url);
        return result;
    }

    private String getConfigFirstNonBlank(String key, String fallback) {
        String v = null;
        try {
            v = systemConfigService.getConfigValue(key);
        } catch (Exception ignored) {
        }
        return StringUtils.hasText(v) ? v : fallback;
    }

    private String buildUrl(String domain, String key) {
        String d = domain.trim();
        if (!d.startsWith("http")) {
            d = "https://" + d;
        }
        if (!d.endsWith("/")) {
            d = d + "/";
        }
        return d + key;
    }

    private String normalizePrefix(String prefix) {
        String p = prefix.trim();
        if (p.isEmpty()) {
            return "";
        }
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (!p.endsWith("/")) {
            p = p + "/";
        }
        return p;
    }

    private String inferExt(String originalFilename, String contentType) {
        String lower = originalFilename == null ? "" : originalFilename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png") || (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("png"))) {
            return ".png";
        }
        if (lower.endsWith(".jpeg") || lower.endsWith(".jpg") || (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("jpeg"))) {
            return ".jpg";
        }
        return ".jpg";
    }

    @Data
    public static class UploadResult {
        private String key;
        private String url;
    }
}


package com.it.greenfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.greenfinance.mapper.SystemConfigMapper;
import com.it.greenfinance.pojo.SystemConfig;
import com.it.greenfinance.pojo.bo.SystemConfigBo;
import com.it.greenfinance.pojo.vo.SystemConfigVo;
import com.it.greenfinance.service.SystemConfigService;
import com.it.utils.UserContextUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.SymmetricCrypto;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 系统配置表 服务实现类
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    private final UserContextUtil userContextUtil;
    private final SystemConfigMapper systemConfigMapper;
    /**
     * 对敏感配置值进行加解密的对称加密器
     */
    private final SymmetricCrypto crypto;

    /**
     * 需要加密存储的配置键（隐私信息）
     */
    private static final Set<String> SENSITIVE_KEYS;

    static {
        Set<String> keys = new HashSet<>();
        // AI 模型相关密钥
        keys.add("ai.model.api-key");
        SENSITIVE_KEYS = Collections.unmodifiableSet(keys);
    }

    public SystemConfigServiceImpl(UserContextUtil userContextUtil,
                                   SystemConfigMapper systemConfigMapper,
                                   @Value("${crypto.secret}") String cryptoSecret) {
        this.userContextUtil = userContextUtil;
        this.systemConfigMapper = systemConfigMapper;
        // 使用 crypto.secret 的 MD5 作为 AES 密钥，长度为 16/24/32 字节
        byte[] key = DigestUtil.md5(cryptoSecret);
        this.crypto = SecureUtil.aes(key);
    }
    
    @Override
    public SystemConfigVo create(SystemConfigBo systemConfigBo) {
        validateSystemConfigForCreate(systemConfigBo);

        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再创建系统配置");
        }

        // 检查该配置项是否已经存在（根据 userId 和 configKey 判断）
        QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                    .eq("config_key", systemConfigBo.getConfigKey());
        
        SystemConfig existingConfig = getOne(queryWrapper);
        
        if (existingConfig != null) {
            // 配置项已存在，执行更新操作
            systemConfigBo.setId(existingConfig.getId());
            return update(systemConfigBo);
        } else {
            // 配置项不存在，执行创建操作
            SystemConfig systemConfig = new SystemConfig();
            BeanUtils.copyProperties(systemConfigBo, systemConfig);
            systemConfig.setUserId(userId);

            // 对敏感配置值进行加密存储
            if (isSensitiveKey(systemConfig.getConfigKey()) && systemConfig.getConfigValue() != null) {
                systemConfig.setConfigValue(encrypt(systemConfig.getConfigValue()));
            }

            save(systemConfig);
            
            return convertToVo(systemConfig);
        }
    }
    
    @Override
    public Page<SystemConfigVo> list(Integer page, Integer size, SystemConfigBo bo) {
        Long userId = userContextUtil.getCurrentUserId();
        List<SystemConfig> configs = systemConfigMapper.getUserConfigsDistinctByKey(userId);
        
        // 分页处理
        int total = configs.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, total);
        
        List<SystemConfig> pagedConfigs = new ArrayList<>();
        if (startIndex < total) {
            pagedConfigs = configs.subList(startIndex, endIndex);
        }
        
        Page<SystemConfigVo> systemConfigVoPage = new Page<>(page, size, total);
        systemConfigVoPage.setRecords(pagedConfigs.stream().map(this::convertToVo).collect(Collectors.toList()));
        
        return systemConfigVoPage;
    }
    
    @Override
    public SystemConfigVo getDetail(SystemConfigBo bo) {
        SystemConfig systemConfig = getOne(buildQueryWrapper(bo));
        
        if (systemConfig != null) {
            return convertToVo(systemConfig);
        }
        return null;
    }
    
    @Override
    public SystemConfigVo update(SystemConfigBo systemConfigBo) {
        if (systemConfigBo == null || systemConfigBo.getId() == null) {
            throw new IllegalArgumentException("系统配置ID不能为空");
        }
        validateSystemConfigForUpdate(systemConfigBo);

        SystemConfig systemConfig = new SystemConfig();
        BeanUtils.copyProperties(systemConfigBo, systemConfig);
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再更新系统配置");
        }
        systemConfig.setUserId(userId);

        // 确认记录归属当前用户
        SystemConfig existing = getById(systemConfig.getId());
        if (existing == null || existing.getUserId() == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("系统配置不存在或无权修改");
        }

        // 对敏感配置值进行加密存储
        if (isSensitiveKey(systemConfig.getConfigKey()) && systemConfig.getConfigValue() != null) {
            systemConfig.setConfigValue(encrypt(systemConfig.getConfigValue()));
        }

        updateById(systemConfig);
        return convertToVo(systemConfig);
    }
    
    @Override
    public boolean removeById(Long id) {
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再删除系统配置");
        }
        // 检查系统配置是否属于当前用户
        SystemConfig systemConfig = getById(id);
        if (systemConfig == null) {
            return false;
        }
        
        // 只能删除自己的系统配置
        if (!systemConfig.getUserId().equals(userId)) {
            return false;
        }
        
        // 删除系统配置
        return super.removeById(id);
    }
    
    @Override
    public String getConfigValue(String configKey) {
        Long userId = userContextUtil.getCurrentUserId();
        String value = systemConfigMapper.getConfigValue(configKey, userId);
        if (value == null) {
            return null;
        }
        // 从数据库取出时对敏感配置值解密，兼容历史明文数据
        if (isSensitiveKey(configKey)) {
            return decryptSafely(value);
        }
        return value;
    }

    /**
     * 判断配置键是否属于敏感信息，需要加密
     */
    private boolean isSensitiveKey(String configKey) {
        return configKey != null && SENSITIVE_KEYS.contains(configKey);
    }

    /**
     * 加密配置值
     */
    private String encrypt(String plainText) {
        return crypto.encryptHex(plainText);
    }

    /**
     * 解密配置值，如果解密失败则原样返回（兼容历史明文）
     */
    private String decryptSafely(String cipherOrPlain) {
        try {
            return crypto.decryptStr(cipherOrPlain);
        } catch (Exception e) {
            // 可能是旧数据（明文），直接返回
            return cipherOrPlain;
        }
    }

    private void validateSystemConfigForCreate(SystemConfigBo bo) {
        if (bo == null) {
            throw new IllegalArgumentException("系统配置参数不能为空");
        }
        if (bo.getConfigKey() == null || bo.getConfigKey().trim().isEmpty()) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        if (bo.getConfigValue() == null) {
            throw new IllegalArgumentException("配置值不能为空");
        }
    }

    private void validateSystemConfigForUpdate(SystemConfigBo bo) {
        if (bo.getConfigKey() != null && bo.getConfigKey().trim().isEmpty()) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        if (bo.getConfigValue() != null && bo.getConfigValue().trim().isEmpty()) {
            throw new IllegalArgumentException("配置值不能为空");
        }
    }
    
    /**
     * 将SystemConfig实体转换为SystemConfigVo
     * @param systemConfig SystemConfig实体
     * @return SystemConfigVo对象
     */
    private SystemConfigVo convertToVo(SystemConfig systemConfig) {
        SystemConfigVo systemConfigVo = new SystemConfigVo();
        BeanUtils.copyProperties(systemConfig, systemConfigVo);
        return systemConfigVo;
    }
    
    private QueryWrapper<SystemConfig> buildQueryWrapper(SystemConfigBo bo) {
        // 创建查询条件构造器实例
        QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
        
        // 如果ID不为空，则添加ID相等条件
        if (bo.getId() != null) {
            queryWrapper.eq("id", bo.getId());
        }
        
        // 设置用户ID相等条件
        if (bo.getUserId() != null) {
            queryWrapper.eq("user_id", bo.getUserId());
        }
        
        // 如果配置键不为空，则添加配置键相等条件
        if (bo.getConfigKey() != null && !bo.getConfigKey().isEmpty()) {
            queryWrapper.eq("config_key", bo.getConfigKey());
        }
        
        // 如果配置值不为空，则添加配置值相等条件
        if (bo.getConfigValue() != null && !bo.getConfigValue().isEmpty()) {
            queryWrapper.eq("config_value", bo.getConfigValue());
        }
        
        // 按创建时间倒序排列结果
        queryWrapper.orderByDesc("create_time");
        
        // 返回构建好的查询条件构造器
        return queryWrapper;
    }
}

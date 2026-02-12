package com.it.greenfinance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.it.greenfinance.pojo.SystemConfig;
import com.it.greenfinance.pojo.bo.SystemConfigBo;
import com.it.greenfinance.pojo.vo.SystemConfigVo;

/**
 * <p>
 * 系统配置表 服务类
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
public interface SystemConfigService extends IService<SystemConfig> {
    
    /**
     * 创建系统配置
     *
     * @param systemConfigBo 系统配置信息
     * @return 创建结果
     */
    SystemConfigVo create(SystemConfigBo systemConfigBo);
    
    /**
     * 获取系统配置列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param bo 查询条件
     * @return 系统配置列表
     */
    Page<SystemConfigVo> list(Integer page, Integer size, SystemConfigBo bo);
    
    /**
     * 获取系统配置详情
     *
     * @param bo 查询条件
     * @return 系统配置详情
     */
    SystemConfigVo getDetail(SystemConfigBo bo);
    
    /**
     * 更新系统配置
     *
     * @param systemConfigBo 更新的系统配置信息
     * @return 更新结果
     */
    SystemConfigVo update(SystemConfigBo systemConfigBo);
    
    /**
     * 删除系统配置
     *
     * @param id 系统配置ID
     * @return 是否删除成功
     */
    boolean removeById(Long id);
    
    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);
}
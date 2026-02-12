package com.it.greenfinance.mapper;

import com.it.greenfinance.pojo.SystemConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 系统配置表 Mapper 接口
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
    
    /**
     * 根据用户ID查询该用户设置的一个月的第一天的日期
     *
     * @param userId 用户ID，用于查询特定用户的配置信息
     * @return 返回LocalDate类型，表示该用户设置的一个月的第一天
     */
    @Select("select config_value from system_config where config_key = 'first_day_of_month' and (user_id =#{userId} or system_config.user_id is null)" +
            " order by user_id desc limit 1")
    String getBillMonthStartDay(Long userId);
    
    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @param userId 用户ID
     * @return 配置值
     */
    @Select("SELECT config_value FROM system_config WHERE config_key = #{configKey} AND (user_id = #{userId} OR user_id IS NULL) ORDER BY user_id DESC LIMIT 1")
    String getConfigValue(@Param("configKey") String configKey, @Param("userId") Long userId);
    
    /**
     * 获取用户配置，相同key只保留一份，优先用户自己的配置
     *
     * @param userId 用户ID
     * @return 配置列表
     */
    @Select("SELECT * FROM system_config s1 " +
            "WHERE s1.user_id = #{userId} " +
            "OR (s1.user_id IS NULL AND NOT EXISTS (" +
            "    SELECT 1 FROM system_config s2 WHERE s2.user_id = #{userId} AND s2.config_key = s1.config_key" +
            "))")
    List<SystemConfig> getUserConfigsDistinctByKey(@Param("userId") Long userId);
}
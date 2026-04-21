package com.it.greenfinance.pojo.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 系统配置表
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SystemConfigBo implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 配置ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 配置键（唯一）
     */
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    /**
     * 配置值
     */
    @NotBlank(message = "配置值不能为空")
    private String configValue;

    /**
     * 配置说明
     */
    @Size(max = 255, message = "配置说明长度过长")
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;


}

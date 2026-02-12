package com.it.greenfinance.pojo.bo;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 月度预算表
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class BudgetBo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 预算ID
     */
    private Long id;
    
    /**
     * 所属用户ID
     */
    private Long userId;
    
    /**
     * 月度预算总金额
     */
    @DecimalMin(value = "0.00", message = "预算金额必须大于等于0")
    private BigDecimal amount;
    
    /**
     * 年份（如2023）
     */
    private Integer year;
    
    /**
     * 月份（1-12）
     */
    private Integer month;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    
}

package com.it.greenfinance.pojo.bo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 预计支出表
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class ExpectedExpenseBo implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 预计支出ID
     */
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 预计金额
     */
    @NotNull(message = "预计金额不能为空")
    @DecimalMin(value = "0.00", message = "预计金额必须大于等于0")
    private BigDecimal amount;

    /**
     * 主分类ID
     */
    @NotNull(message = "主分类ID不能为空")
    private Long categoryId;

    /**
     * 子分类ID
     */
    private Long subCategoryId;

    /**
     * 备注（如"10月房租"）
     */
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    /**
     * 预计支付日期
     */
    @NotNull(message = "预计支付日期不能为空")
    private Date dueDate;

    /**
     * 状态：1-待支付，2-已支付，3-已取消
     */
    private Integer status;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    
    /**
     * 年
     */
    private Integer year;
    
    /**
     * 月
     */
    private Integer month;


}

package com.it.greenfinance.pojo;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
public class ExpectedExpense implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 预计支出ID
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 预计金额
     */
    private BigDecimal amount;

    /**
     * 主分类ID
     */
    private Long categoryId;

    /**
     * 子分类ID
     */
    private Long subCategoryId;

    /**
     * 备注（如"10月房租"）
     */
    private String remark;

    /**
     * 预计支付日期
     */
    private Date dueDate;

    /**
     * 状态：1-待支付，2-已支付，3-已取消
     */
    private Integer status;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


}

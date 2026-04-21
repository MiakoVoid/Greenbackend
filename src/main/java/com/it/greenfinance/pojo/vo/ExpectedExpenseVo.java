package com.it.greenfinance.pojo.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
public class ExpectedExpenseVo implements Serializable {

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
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 主分类名称
     */
    private String categoryName;
    
    /**
     * 子分类名称
     */
    private String subCategoryName;
    
    /**
     * 图标标识符
     */
    private String categoryIcon;


}

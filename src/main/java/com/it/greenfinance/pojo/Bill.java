package com.it.greenfinance.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 账单记录表
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Bill implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 账单ID
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 最终金额（自动计算：original_amount - refund_amount）
     */
    private BigDecimal amount;

    /**
     * 原始金额
     */
    private BigDecimal originalAmount;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 类型：1-支出，2-收入
     */
    private Integer type;

    /**
     * 主分类ID
     */
    private Long categoryId;

    /**
     * 子分类ID
     */
    private Long subCategoryId;

    /**
     * 商户名称（如"星巴克"）
     */
    private String merchant;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 收支发生时间
     */
    private Date billTime;

    /**
     * 支付方式（如"微信支付"）
     */
    private String paymentMethod;

    /**
     * 订单号
     */
    private String orderNumber;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 记录更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


}

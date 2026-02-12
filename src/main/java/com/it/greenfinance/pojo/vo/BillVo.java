package com.it.greenfinance.pojo.vo;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 账单记录表 VO类，用于向前端返回账单数据
 * </p>
 *
 * @author Lingma
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BillVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账单ID
     */
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
     * 主分类名称
     */
    private String categoryName;

    /**
     * 子分类ID
     */
    private Long subCategoryId;

    /**
     * 子分类名称
     */
    private String subCategoryName;

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
     * 记录创建时间
     */
    private Date createTime;

    /**
     * 记录更新时间
     */
    private Date updateTime;

}
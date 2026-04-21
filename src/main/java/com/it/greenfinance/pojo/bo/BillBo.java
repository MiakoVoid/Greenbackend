  package com.it.greenfinance.pojo.bo;
  
  import lombok.Data;
  import lombok.EqualsAndHashCode;
  import lombok.experimental.Accessors;
  import org.springframework.format.annotation.DateTimeFormat;
  
  import javax.validation.constraints.*;
  import java.io.Serial;
  import java.io.Serializable;
  import java.math.BigDecimal;
  import java.util.Date;

/**
 * <p>
 * 账单记录表 BO类，用于接收创建和更新账单的请求参数
 * </p>
 *
 * @author Lingma
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class BillBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 账单ID
     */
    private Long id;

    /**
     * 原始金额
     */
    @NotNull(message = "原始金额不能为空")
    @DecimalMin(value = "0.00", message = "原始金额必须大于等于0")
    private BigDecimal originalAmount;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 类型：1-支出，2-收入
     */
    @NotNull(message = "类型不能为空")
    @Min(value = 1, message = "类型不合法")
    @Max(value = 2, message = "类型不合法")
    private Integer type;

    /**
     * 主分类ID（需存在且归属当前用户）
     */
    @NotNull(message = "主分类ID不能为空")
    private Long categoryId;

    /**
     * 子分类ID，可为null（若传则需归属对应主分类）
     */
    private Long subCategoryId;

    /**
     * 商户名称（最长100字符）
     */
    @Size(max = 100, message = "商户名称长度不能超过100")
    private String merchant;

    /**
     * 备注（最多500字符）
     */
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    /**
     * 账单时间，格式yyyy-MM-dd HH:mm:ss
     */
    private Date billTime;

    /**
     * 支付方式（最长50字符）
     */
    private String paymentMethod;
    
    /**
     * 订单号
     */
    private String orderNumber;
    
    /**
     * 起止时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 创建时间
     * 前端传入，账单发生的时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    
    /**
     * 搜索关键字（用于模糊查询备注、商户名、金额）
     */
    private String keyword;
}

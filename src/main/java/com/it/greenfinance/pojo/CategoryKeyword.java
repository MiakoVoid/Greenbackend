package com.it.greenfinance.pojo;

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
 * 分类关键词表
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CategoryKeyword implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 关键词ID
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID（0=系统默认关键词，非0=用户自定义关键词）
     */
    private Long userId;

    /**
     * 关键词（如"肯德基""外卖""京东"，支持中文/英文/数字）
     */
    private String keyword;

    /**
     * 匹配值（关联的核心值：主分类名/子分类名/商户名，如"餐饮""快餐""肯德基"）
     */
    private String matchValue;

    /**
     * 匹配类型：1=主分类关键词，2=子分类关键词，3=商户名关键词
     */
    private Integer type;

    /**
     * 主分类ID（关联category表，type=1/2时必填）
     */
    private Long categoryId;

    /**
     * 子分类ID（关联sub_category表，type=2时必填，type=1/3时可为NULL）
     */
    private Long subCategoryId;

    /**
     * 匹配权重（1-100，越高优先级越高，解决关键词冲突）
     */
    private Integer weight;

    /**
     * 状态：1=启用，0=禁用（软删除，避免误删恢复麻烦）
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
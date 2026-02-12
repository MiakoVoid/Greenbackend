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
 * 子分类表
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SubCategory implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 子分类ID
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联主分类ID
     */
    private Long categoryId;

    /**
     * 用户ID（null表示系统默认分类）
     */
    private Long userId;

    /**
     * 子分类名称
     */
    private String name;

    /**
     * 图标标识（规则同主分类，为空时继承主分类图标）
     */
    private String iconIdentifier;

    /**
     * 排序序号（升序排列）
     */
    private Integer sortOrder;

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

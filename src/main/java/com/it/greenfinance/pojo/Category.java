package com.it.greenfinance.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 主分类表
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Category implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主分类ID
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（null表示系统默认分类）
     */
    private Long userId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类图标标识符
     */
    private String categoryIcon;

    /**
     * 类型：1-支出，2-收入
     */
    private Integer type;

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
    
    @TableField(exist = false)
    private List<SubCategory> subCategories;
    
    

}
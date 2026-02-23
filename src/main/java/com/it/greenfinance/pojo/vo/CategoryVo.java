package com.it.greenfinance.pojo.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.it.greenfinance.pojo.SubCategory;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 主分类表 VO类，用于向前端返回分类数据
 * </p>
 *
 * @author Lingma
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CategoryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主分类ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
     * 子分类列表
     */
    @TableField(exist = false)
    private List<SubCategory> subCategories;
}
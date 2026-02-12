package com.it.greenfinance.pojo.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 子分类表 VO类，用于向前端返回子分类数据
 * </p>
 *
 * @author Lingma
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SubCategoryVo implements Serializable {

    private static final long serialVersionUID = 1L;

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
}

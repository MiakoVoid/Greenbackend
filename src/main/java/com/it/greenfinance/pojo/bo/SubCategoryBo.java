package com.it.greenfinance.pojo.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * <p>
 * 子分类表 BO类，用于接收创建和更新子分类的请求参数
 * </p>
 *
 * @author Lingma
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SubCategoryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 子分类ID
     */
    private Long id;

    /**
     * 关联主分类ID
     */
    @NotNull(message = "主分类ID不能为空")
    private Long categoryId;

    /**
     * 子分类名称，同一主分类下唯一
     */
    @NotBlank(message = "子分类名称不能为空")
    private String name;

    /**
     * 子分类图标标识符，空则继承主分类
     */
    @Size(max = 255, message = "图标标识长度过长")
    private String categoryIcon;

    /**
     * 排序序号，默认0
     */
    @Min(value = 0, message = "排序序号不能为负数")
    private Integer sortOrder;
}

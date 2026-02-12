package com.it.greenfinance.pojo.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;
import java.io.Serializable;

/**
 * <p>
 * 主分类表 BO类，用于接收创建和更新分类的请求参数
 * </p>
 *
 * @author Lingma
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CategoryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主分类ID
     */
    private Long id;

    /**
     * 分类名称，唯一
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 图标标识（res:资源名/file:本地路径）
     */
    @Size(max = 255, message = "图标标识长度过长")
    private String iconIdentifier;

    /**
     * 类型：1=支出，2=收入
     */
    @NotNull(message = "分类类型不能为空")
    @Min(value = 1, message = "分类类型不合法")
    @Max(value = 2, message = "分类类型不合法")
    private Integer type;

    /**
     * 排序序号，默认0
     */
    @Min(value = 0, message = "排序序号不能为负数")
    private Integer sortOrder;
}

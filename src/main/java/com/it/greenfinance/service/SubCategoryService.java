package com.it.greenfinance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.it.greenfinance.pojo.SubCategory;
import com.it.greenfinance.pojo.bo.SubCategoryBo;
import com.it.greenfinance.pojo.vo.SubCategoryVo;

import java.util.List;

/**
 * <p>
 * 子分类表 服务类
 * </p>
 *
 * @author Lingma
 * @since 2025-10-25
 */
public interface SubCategoryService extends IService<SubCategory> {
    
    /**
     * 根据分类ID和用户ID获取子分类列表
     *
     * @param categoryId 分类ID
     * @param userId 用户ID
     * @return 子分类列表
     */
    List<SubCategoryVo> list(Long categoryId, Long userId);
    
    /**
     * 保存子分类
     *
     * @param subCategoryBo 子分类信息
     * @param userId
     * @return 保存后的子分类
     */
    SubCategoryVo saveSubCategory(SubCategoryBo subCategoryBo, Long userId);
    
    /**
     * 更新子分类
     *
     * @param subCategoryBo 子分类信息
     * @param userId 用户ID
     * @return 更新后的子分类
     */
    SubCategoryVo updateSubCategory(SubCategoryBo subCategoryBo, Long userId);
    
    /**
     * 根据ID删除子分类（只有用户自己的子分类才能删除）
     *
     * @param id 子分类ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean removeSubCategoryById(Long id, Long userId);
}
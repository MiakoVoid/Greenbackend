package com.it.greenfinance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.it.greenfinance.pojo.Category;
import com.it.greenfinance.pojo.bo.CategoryBo;
import com.it.greenfinance.pojo.vo.CategoryVo;

import java.util.List;

/**
 * <p>
 * 主分类表 服务类
 * </p>
 *
 * @author Lingma
 * @since 2025-10-25
 */
public interface CategoryService extends IService<Category> {
    
    /**
     * 获取用户分类及其子分类
     *
     * @param userId 用户ID
     * @param type 分类类型（可选）
     * @return 分类列表
     */
    List<CategoryVo> getUserCategoriesWithSubCategories(Long userId, Integer type);
    
    /**
     * 保存分类
     *
     * @param categoryBo 分类信息
     * @return 保存后的分类
     */
    CategoryVo saveCategory(CategoryBo categoryBo, Long userId);
    
    /**
     * 更新分类
     *
     * @param categoryBo 分类信息
     * @param userId 用户ID
     * @return 更新后的分类
     */
    CategoryVo updateCategory(CategoryBo categoryBo, Long userId);
    
    /**
     * 根据ID删除分类（只有用户自己的分类才能删除）
     *
     * @param id 分类ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean removeCategoryById(Long id, Long userId);
    
    List<String> buildIconLibrary(Integer type);
}
package com.it.greenfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.greenfinance.mapper.CategoryMapper;
import com.it.greenfinance.mapper.SubCategoryMapper;
import com.it.greenfinance.pojo.Category;
import com.it.greenfinance.pojo.SubCategory;
import com.it.greenfinance.pojo.bo.CategoryBo;
import com.it.greenfinance.pojo.vo.CategoryVo;
import com.it.greenfinance.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * 主分类表 服务实现类
 *
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    
    private final SubCategoryMapper subCategoryMapper;
    
    public CategoryServiceImpl(SubCategoryMapper subCategoryMapper) {
        this.subCategoryMapper = subCategoryMapper;
    }
    
    @Override
    public List<CategoryVo> getUserCategoriesWithSubCategories(Long userId, Integer type) {
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.nested(wrapper -> {
            wrapper.isNull("user_id");
            if (userId != null) {
                wrapper.or().eq("user_id", userId);
            }
        });
        
        if (type != null) {
            queryWrapper.eq("type", type);
        }
        
        queryWrapper.orderByAsc("sort_order");
        List<Category> categories = list(queryWrapper);
        
        // 查询每个分类的子分类
        List<CategoryVo> categoryVos = new ArrayList<>();
        for (Category category : categories) {
            CategoryVo categoryVo = new CategoryVo();
            BeanUtils.copyProperties(category, categoryVo);
            
            QueryWrapper<SubCategory> subQueryWrapper = new QueryWrapper<>();
            subQueryWrapper.eq("category_id", category.getId());
            subQueryWrapper.nested(wrapper -> {
                wrapper.isNull("user_id");
                if (userId != null) {
                    wrapper.or().eq("user_id", userId);
                }
            });
            subQueryWrapper.orderByAsc("sort_order");
            categoryVo.setSubCategories(subCategoryMapper.selectList(subQueryWrapper));
            
            categoryVos.add(categoryVo);
        }
        
        return categoryVos;
    }
    
    @Override
    public CategoryVo saveCategory(CategoryBo categoryBo, Long userId) {
        validateCategoryForCreate(categoryBo, userId);

        Category category = new Category();
        BeanUtils.copyProperties(categoryBo, category);
        category.setUserId(userId);
        save(category);
        CategoryVo categoryVo = new CategoryVo();
        BeanUtils.copyProperties(category, categoryVo);
        return categoryVo;
    }
    
    @Override
    public CategoryVo updateCategory(CategoryBo categoryBo, Long userId) {
        if (categoryBo == null || categoryBo.getId() == null) {
            throw new IllegalArgumentException("分类ID不能为空");
        }
        validateCategoryForUpdate(categoryBo);

        // 检查分类是否属于当前用户
        Category existingCategory = getById(categoryBo.getId());
        if (existingCategory != null &&
                (existingCategory.getUserId() == null || !existingCategory.getUserId().equals(userId))) {
            throw new RuntimeException("无权限更新该分类");
        }
        
        Category category = new Category();
        BeanUtils.copyProperties(categoryBo, category);
        updateById(category);
        
        CategoryVo categoryVo = new CategoryVo();
        BeanUtils.copyProperties(category, categoryVo);
        return categoryVo;
    }
    
    @Override
    public boolean removeCategoryById(Long id, Long userId) {
        if (id == null) {
            throw new IllegalArgumentException("分类ID不能为空");
        }
        // 检查分类是否属于当前用户
        Category category = getById(id);
        if (category == null) {
            return false;
        }
        
        // 系统默认分类（userId为null）不能删除
        if (category.getUserId() == null) {
            return false;
        }
        
        // 只能删除自己的分类
        if (!category.getUserId().equals(userId)) {
            return false;
        }
        
        // 删除分类及其关联的子分类
        removeById(id);
        QueryWrapper<SubCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category_id", id);
        subCategoryMapper.delete(queryWrapper);
        
        return true;
    }

    private void validateCategoryForCreate(CategoryBo bo, Long userId) {
        if (bo == null) {
            throw new IllegalArgumentException("分类参数不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再创建分类");
        }
        if (bo.getName() == null || bo.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (bo.getType() == null || (bo.getType() != 1 && bo.getType() != 2)) {
            throw new IllegalArgumentException("分类类型不合法");
        }
    }

    private void validateCategoryForUpdate(CategoryBo bo) {
        if (bo.getName() != null && bo.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (bo.getType() != null && (bo.getType() != 1 && bo.getType() != 2)) {
            throw new IllegalArgumentException("分类类型不合法");
        }
    }
}

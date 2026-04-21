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
        validate(categoryBo,userId);

        Category category = new Category();
        BeanUtils.copyProperties(categoryBo, category);
        category.setUserId(userId);
        save(category);
        
        CategoryVo categoryVo = new CategoryVo();
        BeanUtils.copyProperties(category, categoryVo);
        
        return categoryVo;
    }
    private void validate(CategoryBo bo, Long userId) {
        if (bo.getName() != null && bo.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        if (bo.getType() != null && (bo.getType() != 1 && bo.getType() != 2)) {
            throw new IllegalArgumentException("分类类型不合法");
        }
        
        // 如果更新了名称或类型，需要检查是否与用户的其他分类或系统默认分类重名
        if (bo.getId() != null && bo.getName() != null && bo.getType() != null) {
            // 检查是否与用户的其他分类重名
            QueryWrapper<Category> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.eq("user_id", userId);
            userQueryWrapper.eq("type", bo.getType());
            userQueryWrapper.eq("name", bo.getName().trim());
           if (bo.getId()!=null)userQueryWrapper.ne("id", bo.getId()); // 排除当前分类
            Long userCount = (long) count(userQueryWrapper);
            if (userCount > 0) {
                throw new IllegalArgumentException("该分类名称已存在，请勿重复添加");
            }
            
            // 检查是否与系统默认分类重名
            QueryWrapper<Category> systemQueryWrapper = new QueryWrapper<>();
            systemQueryWrapper.isNull("user_id");
            systemQueryWrapper.eq("type", bo.getType());
            systemQueryWrapper.eq("name", bo.getName().trim());
            Long systemCount = (long) count(systemQueryWrapper);
            if (systemCount > 0) {
                throw new IllegalArgumentException("该分类名称已与系统默认分类重复，请更换其他名称");
            }
        }
    }
    
    @Override
    public CategoryVo updateCategory(CategoryBo categoryBo, Long userId) {
        validate(categoryBo,userId);
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
    
    @Override
    public List<String> buildIconLibrary(Integer type) {
        // 查询主分类的图标
        QueryWrapper<Category> categoryQuery = new QueryWrapper<>();
        categoryQuery.isNull("user_id"); // 只查询系统默认分类
        if (type != null) {
            categoryQuery.eq("type", type);
        }
        categoryQuery.select("category_Icon");
        categoryQuery.orderByAsc("sort_order");
        List<Category> categories = list(categoryQuery);
        
        // 查询子分类的图标
        QueryWrapper<SubCategory> subCategoryQuery = new QueryWrapper<>();
        subCategoryQuery.isNull("user_id"); // 只查询系统默认子分类
        if (type != null) {
            // 需要先找到对应type的分类ID
            List<Long> categoryIds = categories.stream()
                    .map(Category::getId)
                    .collect(java.util.stream.Collectors.toList());
            if (!categoryIds.isEmpty()) {
                subCategoryQuery.in("category_id", categoryIds);
            } else {
                // 如果没有找到对应type的分类，返回空列表
                return new ArrayList<>();
            }
        }
        subCategoryQuery.select("category_Icon");
        subCategoryQuery.orderByAsc("sort_order");
        List<SubCategory> subCategories = subCategoryMapper.selectList(subCategoryQuery);
        
        // 合并主分类和子分类的图标，去重并保持顺序
        List<String> iconList = new ArrayList<>();
        for (Category category : categories) {
            if (category.getCategoryIcon() != null && !category.getCategoryIcon().trim().isEmpty()) {
                iconList.add(category.getCategoryIcon());
            }
        }
        for (SubCategory subCategory : subCategories) {
            if (subCategory.getCategoryIcon() != null && !subCategory.getCategoryIcon().trim().isEmpty()) {
                iconList.add(subCategory.getCategoryIcon());
            }
        }
        return iconList;
    }
}

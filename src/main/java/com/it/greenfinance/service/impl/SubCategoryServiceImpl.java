package com.it.greenfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.greenfinance.mapper.SubCategoryMapper;
import com.it.greenfinance.pojo.SubCategory;
import com.it.greenfinance.pojo.bo.SubCategoryBo;
import com.it.greenfinance.pojo.vo.SubCategoryVo;
import com.it.greenfinance.service.SubCategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * 子分类表 服务实现类
 *
 *
 * @author Lingma
 * @since 2025-10-25
 */
@Service
public class SubCategoryServiceImpl extends ServiceImpl<SubCategoryMapper, SubCategory> implements SubCategoryService {
    
    @Override
    public List<SubCategoryVo> list(Long categoryId, Long userId) {
        QueryWrapper<SubCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category_id", categoryId);
        queryWrapper.nested(wrapper -> {
            wrapper.isNull("user_id");
            if (userId != null) {
                wrapper.or().eq("user_id", userId);
            }
        });
        queryWrapper.orderByAsc("sort_order");
        
        List<SubCategory> subCategories = list(queryWrapper);
        List<SubCategoryVo> subCategoryVos = new ArrayList<>();
        
        for (SubCategory subCategory : subCategories) {
            SubCategoryVo subCategoryVo = new SubCategoryVo();
            BeanUtils.copyProperties(subCategory, subCategoryVo);
            subCategoryVos.add(subCategoryVo);
        }
        
        return subCategoryVos;
    }
    
    @Override
    public SubCategoryVo saveSubCategory(SubCategoryBo subCategoryBo, Long userId) {
        validateSubCategoryForCreate(subCategoryBo, userId);

        SubCategory subCategory = new SubCategory();
        BeanUtils.copyProperties(subCategoryBo, subCategory);
        subCategory.setUserId(userId);
        save(subCategory);
        SubCategoryVo subCategoryVo = new SubCategoryVo();
        BeanUtils.copyProperties(subCategory, subCategoryVo);
        return subCategoryVo;
    }
    
    @Override
    public SubCategoryVo updateSubCategory(SubCategoryBo subCategoryBo, Long userId) {
        if (subCategoryBo == null || subCategoryBo.getId() == null) {
            throw new IllegalArgumentException("子分类ID不能为空");
        }
        validateSubCategoryForUpdate(subCategoryBo);

        // 检查子分类是否属于当前用户
        SubCategory existingSubCategory = getById(subCategoryBo.getId());
        if (existingSubCategory != null &&
                (existingSubCategory.getUserId() == null || !existingSubCategory.getUserId().equals(userId))) {
            throw new RuntimeException("无权限更新该子分类");
        }
        
        SubCategory subCategory = new SubCategory();
        BeanUtils.copyProperties(subCategoryBo, subCategory);
        updateById(subCategory);
        
        SubCategoryVo subCategoryVo = new SubCategoryVo();
        BeanUtils.copyProperties(subCategory, subCategoryVo);
        return subCategoryVo;
    }
    
    @Override
    public boolean removeSubCategoryById(Long id, Long userId) {
        if (id == null) {
            throw new IllegalArgumentException("子分类ID不能为空");
        }
        // 检查子分类是否属于当前用户
        SubCategory subCategory = getById(id);
        if (subCategory == null) {
            return false;
        }
        
        // 系统默认子分类（userId为null）不能删除
        if (subCategory.getUserId() == null) {
            return false;
        }
        
        // 只能删除自己的子分类
        if (!subCategory.getUserId().equals(userId)) {
            return false;
        }
        
        // 删除子分类
        return super.removeById(id);
    }

    private void validateSubCategoryForCreate(SubCategoryBo bo, Long userId) {
        if (bo == null) {
            throw new IllegalArgumentException("子分类参数不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再创建子分类");
        }
        if (bo.getCategoryId() == null) {
            throw new IllegalArgumentException("主分类ID不能为空");
        }
        if (bo.getName() == null || bo.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("子分类名称不能为空");
        }
    }

    private void validateSubCategoryForUpdate(SubCategoryBo bo) {
        if (bo.getName() != null && bo.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("子分类名称不能为空");
        }
    }
}

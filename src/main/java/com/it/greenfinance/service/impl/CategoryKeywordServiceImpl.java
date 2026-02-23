package com.it.greenfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.greenfinance.mapper.CategoryKeywordMapper;
import com.it.greenfinance.pojo.CategoryKeyword;
import com.it.greenfinance.pojo.bo.CategoryKeywordBo;
import com.it.greenfinance.pojo.vo.CategoryKeywordVo;
import com.it.greenfinance.service.CategoryKeywordService;
import com.it.utils.UserContextUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 分类关键词表 服务实现类
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Service
public class CategoryKeywordServiceImpl extends ServiceImpl<CategoryKeywordMapper, CategoryKeyword> implements CategoryKeywordService {
    
    private final UserContextUtil userContextUtil;
    
    public CategoryKeywordServiceImpl(UserContextUtil userContextUtil) {
        this.userContextUtil = userContextUtil;
    }
    
    @Override
    public CategoryKeywordVo create(CategoryKeywordBo categoryKeywordBo) {
        validateCategoryKeywordForCreate(categoryKeywordBo);

        CategoryKeyword categoryKeyword = new CategoryKeyword();
        BeanUtils.copyProperties(categoryKeywordBo, categoryKeyword);
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再创建关键词");
        }
        categoryKeyword.setUserId(userId);
        
        // 设置默认值
        if (categoryKeyword.getWeight() == null) {
            categoryKeyword.setWeight(50); // 默认权重为50
        }
        if (categoryKeyword.getStatus() == null) {
            categoryKeyword.setStatus(1); // 默认启用状态
        }
        
        save(categoryKeyword);
        
        return convertToVo(categoryKeyword);
    }
    
    @Override
    public Page<CategoryKeywordVo> list(Integer page, Integer size, CategoryKeywordBo bo) {
        Page<CategoryKeyword> categoryKeywordPage = new Page<>(page, size);
        Page<CategoryKeyword> resultPage = page(categoryKeywordPage, buildQueryWrapper(bo));
        Page<CategoryKeywordVo> categoryKeywordVoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        categoryKeywordVoPage.setRecords(resultPage.getRecords().stream().map(this::convertToVo).collect(Collectors.toList()));
        
        return categoryKeywordVoPage;
    }
    
    @Override
    public CategoryKeywordVo getDetail(CategoryKeywordBo bo) {
        CategoryKeyword categoryKeyword = getOne(buildQueryWrapper(bo));
        
        if (categoryKeyword != null) {
            return convertToVo(categoryKeyword);
        }
        return null;
    }
    
    @Override
    public CategoryKeywordVo update(CategoryKeywordBo categoryKeywordBo) {
        if (categoryKeywordBo == null || categoryKeywordBo.getId() == null) {
            throw new IllegalArgumentException("关键词ID不能为空");
        }
        validateCategoryKeywordForUpdate(categoryKeywordBo);

        CategoryKeyword categoryKeyword = new CategoryKeyword();
        BeanUtils.copyProperties(categoryKeywordBo, categoryKeyword);
        Long userId = userContextUtil.getCurrentUserId();
        categoryKeyword.setUserId(userId);
        
        updateById(categoryKeyword);
        return convertToVo(categoryKeyword);
    }
    
    @Override
    public boolean removeById(Long id) {
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再删除关键词");
        }
        // 检查分类关键词是否属于当前用户
        CategoryKeyword categoryKeyword = getById(id);
        if (categoryKeyword == null) {
            return false;
        }
        
        // 只能操作自己的分类关键词（系统默认关键词除外）
        if (!categoryKeyword.getUserId().equals(userId) && !categoryKeyword.getUserId().equals(0L)) {
            return false;
        }
        
        // 软删除：将状态设置为禁用
        categoryKeyword.setStatus(0);
        return updateById(categoryKeyword);
    }
    
    @Override
    public List<CategoryKeywordVo> findByKeyword(String keyword) {
        Long userId = userContextUtil.getCurrentUserId();
        List<CategoryKeyword> categoryKeywords = baseMapper.findActiveByKeywordOrderByWeight(keyword, userId);
        return categoryKeywords.stream().map(this::convertToVo).collect(Collectors.toList());
    }
    
    @Override
    public CategoryKeywordVo findByExactKeyword(String keyword) {
        Long userId = userContextUtil.getCurrentUserId();
        CategoryKeyword categoryKeyword = baseMapper.findByExactKeyword(keyword, userId);
        // 只返回启用状态的关键词
        if (categoryKeyword != null && categoryKeyword.getStatus() == 1) {
            return convertToVo(categoryKeyword);
        }
        return null;
    }
    
    @Override
    public List<CategoryKeyword> findByKeyword(String keyword, Long userId) {
        return baseMapper.findActiveByKeywordOrderByWeight(keyword, userId);
    }
    
    @Override
    public List<String> findAllActiveMerchants() {
        return baseMapper.findAllActiveMerchants();
    }
    
    @Override
    public List<String> findAllActiveCategoryKeywords() {
        return baseMapper.findAllActiveCategoryKeywords();
    }
    
    /**
     * 将CategoryKeyword实体转换为CategoryKeywordVo
     * @param categoryKeyword CategoryKeyword实体
     * @return CategoryKeywordVo对象
     */
    private CategoryKeywordVo convertToVo(CategoryKeyword categoryKeyword) {
        CategoryKeywordVo categoryKeywordVo = new CategoryKeywordVo();
        BeanUtils.copyProperties(categoryKeyword, categoryKeywordVo);
        return categoryKeywordVo;
    }
    
    private QueryWrapper<CategoryKeyword> buildQueryWrapper(CategoryKeywordBo bo) {
        // 创建查询条件构造器实例
        QueryWrapper<CategoryKeyword> queryWrapper = new QueryWrapper<>();
        
        // 如果ID不为空，则添加ID相等条件
        if (bo.getId() != null) {
            queryWrapper.eq("id", bo.getId());
        }
        
        // 设置用户ID相等条件（包括系统默认关键词）
        if (bo.getUserId() != null) {
            queryWrapper.nested(wrapper -> wrapper.eq("user_id", bo.getUserId()));
        }
        
        // 如果主分类ID不为空，则添加主分类ID相等条件
        if (bo.getCategoryId() != null) {
            queryWrapper.eq("category_id", bo.getCategoryId());
        }
        
        // 如果子分类ID不为空，则添加子分类ID相等条件
        if (bo.getSubCategoryId() != null) {
            queryWrapper.eq("sub_category_id", bo.getSubCategoryId());
        }
        
        // 如果关键词不为空，则添加关键词相等条件
        if (bo.getKeyword() != null && !bo.getKeyword().isEmpty()) {
            queryWrapper.eq("keyword", bo.getKeyword());
        }
        
        // 如果匹配类型不为空，则添加匹配类型相等条件
        if (bo.getType() != null) {
            queryWrapper.eq("type", bo.getType());
        }
        
        // 如果状态不为空，则添加状态相等条件
        if (bo.getStatus() != null) {
            queryWrapper.eq("status", bo.getStatus());
        }
        
        // 按创建时间倒序排列结果
        queryWrapper.orderByDesc("create_time");
        
        // 返回构建好的查询条件构造器
        return queryWrapper;
    }

    private void validateCategoryKeywordForCreate(CategoryKeywordBo bo) {
        if (bo == null) {
            throw new IllegalArgumentException("分类关键词参数不能为空");
        }
        if (bo.getKeyword() == null || bo.getKeyword().trim().isEmpty()) {
            throw new IllegalArgumentException("关键词不能为空");
        }
        if (bo.getType() == null || bo.getType() < 1 || bo.getType() > 3) {
            throw new IllegalArgumentException("关键词类型不合法");
        }
        if (bo.getCategoryId() == null && (bo.getType() == 1 || bo.getType() == 2)) {
            throw new IllegalArgumentException("主分类ID不能为空");
        }
    }

    private void validateCategoryKeywordForUpdate(CategoryKeywordBo bo) {
        if (bo.getKeyword() != null && bo.getKeyword().trim().isEmpty()) {
            throw new IllegalArgumentException("关键词不能为空");
        }
        if (bo.getType() != null && (bo.getType() < 1 || bo.getType() > 3)) {
            throw new IllegalArgumentException("关键词类型不合法");
        }
    }
}

package com.it.greenfinance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.it.greenfinance.pojo.CategoryKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 分类关键词表 Mapper 接口
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Mapper
public interface CategoryKeywordMapper extends BaseMapper<CategoryKeyword> {
    
    /**
     * 根据关键词查找匹配的分类关键词列表
     * 
     * @param keyword 关键词
     * @param userId 用户ID
     * @return 匹配的分类关键词列表
     */
    List<CategoryKeyword> findByKeyword(@Param("keyword") String keyword, @Param("userId") Long userId);
    
    /**
     * 根据关键词精确查找匹配的分类关键词
     * 
     * @param keyword 关键词
     * @param userId 用户ID
     * @return 匹配的分类关键词
     */
    CategoryKeyword findByExactKeyword(@Param("keyword") String keyword, @Param("userId") Long userId);
    
    /**
     * 根据关键词查找启用状态的分类关键词列表（按权重排序）
     * 
     * @param keyword 关键词
     * @param userId 用户ID
     * @return 匹配的启用状态分类关键词列表
     */
    List<CategoryKeyword> findActiveByKeywordOrderByWeight(@Param("keyword") String keyword, @Param("userId") Long userId);
    
    /**
     * 获取所有启用的商户名关键词
     * 
     * @return 商户名关键词列表
     */
    List<String> findAllActiveMerchants();
    
    /**
     * 获取所有启用的分类关键词
     * 
     * @return 分类关键词列表
     */
    List<String> findAllActiveCategoryKeywords();
}
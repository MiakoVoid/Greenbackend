package com.it.greenfinance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.it.greenfinance.pojo.CategoryKeyword;
import com.it.greenfinance.pojo.bo.CategoryKeywordBo;
import com.it.greenfinance.pojo.vo.CategoryKeywordVo;

import java.util.List;

/**
 * <p>
 * 分类关键词表 服务类
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
public interface CategoryKeywordService extends IService<CategoryKeyword> {
    
    /**
     * 创建分类关键词
     *
     * @param categoryKeywordBo 分类关键词信息
     * @return 创建结果
     */
    CategoryKeywordVo create(CategoryKeywordBo categoryKeywordBo);
    
    /**
     * 获取分类关键词列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param bo 查询条件
     * @return 分类关键词列表
     */
    Page<CategoryKeywordVo> list(Integer page, Integer size, CategoryKeywordBo bo);
    
    /**
     * 获取分类关键词详情
     *
     * @param bo 查询条件
     * @return 分类关键词详情
     */
    CategoryKeywordVo getDetail(CategoryKeywordBo bo);
    
    /**
     * 更新分类关键词
     *
     * @param categoryKeywordBo 更新的分类关键词信息
     * @return 更新结果
     */
    CategoryKeywordVo update(CategoryKeywordBo categoryKeywordBo);
    
    /**
     * 删除分类关键词（软删除）
     *
     * @param id 分类关键词ID
     * @return 是否删除成功
     */
    boolean removeById(Long id);
    
    /**
     * 根据关键词查找匹配的分类（启用状态，按权重排序）
     * 
     * @param keyword 关键词
     * @return 匹配的分类关键词列表
     */
    List<CategoryKeywordVo> findByKeyword(String keyword);
    
    /**
     * 根据关键词精确查找匹配的分类（启用状态）
     * 
     * @param keyword 关键词
     * @return 匹配的分类关键词
     */
    CategoryKeywordVo findByExactKeyword(String keyword);
    
    /**
     * 根据关键词查找匹配的分类（指定用户，启用状态，按权重排序）
     * 
     * @param keyword 关键词
     * @param userId 用户ID
     * @return 匹配的分类关键词列表
     */
    List<CategoryKeyword> findByKeyword(String keyword, Long userId);
    
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
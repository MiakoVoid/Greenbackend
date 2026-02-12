package com.it.greenfinance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.it.greenfinance.pojo.Budget;
import com.it.greenfinance.pojo.bo.BudgetBo;
import com.it.greenfinance.pojo.vo.BudgetStatisticsVo;
import com.it.greenfinance.pojo.vo.BudgetVo;

/**
 * <p>
 * 月度预算表 服务类
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
public interface BudgetService extends IService<Budget> {
    
    /**
     * 创建预算
     *
     * @param budgetBo 预算信息
     * @return 创建后的预算VO
     */
    BudgetVo create(BudgetBo budgetBo);
    
    /**
     * 分页获取预算列表
     *
     * @param page 页码
     * @param size 每页条数
     * @return 预算分页数据
     */
    Page<BudgetVo> list(Integer page, Integer size, BudgetBo budgetBo);
    
    /**
     * 获取预算详情
     *
     * @param budgetBo 预算参数
     * @return 预算详情
     */
    BudgetVo getDetail(BudgetBo budgetBo);
    
    /**
     * 更新预算
     *
     * @param budgetBo 预算信息
     * @return 更新后的预算
     */
    BudgetVo update(BudgetBo budgetBo);
    
    /**
     * 根据ID删除预算
     *
     * @param id 预算ID
     * @return 是否删除成功
     */
    boolean removeById(Long id);
    
    
    /**
     * 获取预算统计信息
     * @param budgetBo 预算参数
     * @return 预算统计信息
     */
    BudgetStatisticsVo getStatistics(BudgetBo budgetBo);
}
package com.it.greenfinance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.it.greenfinance.pojo.ExpectedExpense;
import com.it.greenfinance.pojo.bo.ExpectedExpenseBo;
import com.it.greenfinance.pojo.vo.ExpectedExpenseVo;

import java.util.List;

/**
 * <p>
 * 预计支出表 服务类
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
public interface ExpectedExpenseService extends IService<ExpectedExpense> {
    
    ExpectedExpenseVo listByIds(Long id, Long userId);
    
    ExpectedExpenseVo create(ExpectedExpenseBo expectedExpenseBo);
    
    List<ExpectedExpenseVo> list(Long userId, ExpectedExpenseBo expectedExpenseBo);
    
    ExpectedExpenseVo update(Long userId, ExpectedExpenseBo expectedExpenseBo);
    
    boolean delete(Long id, Long userId);
    
    boolean confirm(Long id, Long userId);
}
package com.it.greenfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.greenfinance.mapper.ExpectedExpenseMapper;
import com.it.greenfinance.pojo.Category;
import com.it.greenfinance.pojo.ExpectedExpense;
import com.it.greenfinance.pojo.bo.BillBo;
import com.it.greenfinance.pojo.bo.ExpectedExpenseBo;
import com.it.greenfinance.pojo.vo.ExpectedExpenseVo;
import com.it.greenfinance.service.BillService;
import com.it.greenfinance.service.CategoryService;
import com.it.greenfinance.service.ExpectedExpenseService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 预计支出表 服务实现类
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Service
public class ExpectedExpenseServiceImpl extends ServiceImpl<ExpectedExpenseMapper, ExpectedExpense> implements ExpectedExpenseService {
    
    private final BillService billService;
    private final CategoryService categoryService;
    
    public ExpectedExpenseServiceImpl(BillService billService, CategoryService categoryService) {
        this.billService = billService;
        this.categoryService = categoryService;
    }
    
    @Override
    public ExpectedExpenseVo listByIds(Long id, Long userId) {
        ExpectedExpenseBo expectedExpenseBo = new ExpectedExpenseBo();
        expectedExpenseBo.setId(id);
        expectedExpenseBo.setUserId(userId);
        ExpectedExpense expectedExpense = this.getOne(buildQueryWrapper(expectedExpenseBo));
        if (expectedExpense == null) {
            return null;
        }
        ExpectedExpenseVo expectedExpenseVo = new ExpectedExpenseVo();
        BeanUtils.copyProperties(expectedExpense, expectedExpenseVo);
        
        // 设置分类图标
        setCategoryIcon(expectedExpenseVo, expectedExpense);
        
        return expectedExpenseVo;
    }
    /**
     * 构建查询条件
     *
     */
    private QueryWrapper<ExpectedExpense> buildQueryWrapper(ExpectedExpenseBo expectedExpenseBo) {
        QueryWrapper<ExpectedExpense> queryWrapper = new QueryWrapper<>();
        if (expectedExpenseBo.getId() != null)
            queryWrapper.eq("id", expectedExpenseBo.getId());
        if (expectedExpenseBo.getUserId() != null)
            queryWrapper.eq("user_id", expectedExpenseBo.getUserId());
            
        if (expectedExpenseBo.getCategoryId() != null)
            queryWrapper.eq("category_id", expectedExpenseBo.getCategoryId());
            
        if (expectedExpenseBo.getStatus() != null)
            queryWrapper.eq("status", expectedExpenseBo.getStatus());
        if (expectedExpenseBo.getCreateTime() != null)
            queryWrapper.eq("create_time", expectedExpenseBo.getCreateTime());
            
        if (expectedExpenseBo.getUpdateTime() != null)
            queryWrapper.eq("update_time", expectedExpenseBo.getUpdateTime());
            
        if (expectedExpenseBo.getSubCategoryId() != null)
            queryWrapper.eq("sub_category_id", expectedExpenseBo.getSubCategoryId());
            
        if (expectedExpenseBo.getStatus() != null)
            queryWrapper.eq("status", expectedExpenseBo.getStatus());
        queryWrapper.orderByDesc("create_time");
        return queryWrapper;
    }
    
    @Override
    public ExpectedExpenseVo create(ExpectedExpenseBo expectedExpenseBo) {
        validateExpectedExpenseForCreate(expectedExpenseBo);

        ExpectedExpense expectedExpense = new ExpectedExpense();
        BeanUtils.copyProperties(expectedExpenseBo, expectedExpense);
        this.save(expectedExpense);
        ExpectedExpenseVo expectedExpenseVo = new ExpectedExpenseVo();
        BeanUtils.copyProperties(expectedExpense, expectedExpenseVo);
        return expectedExpenseVo;
    }
    
 
    

    /**
     * 设置分类图标（优先子分类，其次主分类）
     * @param vo 预计支出 VO
     * @param expense 预计支出实体
     */
    private void setCategoryIcon(ExpectedExpenseVo vo, ExpectedExpense expense) {
        // 优先使用子分类图标
        if (expense.getSubCategoryId() != null) {
            Category subCategory = categoryService.getById(expense.getSubCategoryId());
            if (subCategory != null && subCategory.getCategoryIcon() != null) {
                vo.setCategoryIcon(subCategory.getCategoryIcon());
                return;
            }
        }
        
        // 其次使用主分类图标
        if (expense.getCategoryId() != null) {
            Category category = categoryService.getById(expense.getCategoryId());
            if (category != null && category.getCategoryIcon() != null) {
                vo.setCategoryIcon(category.getCategoryIcon());
            }
        }
    }

    /**
     * 列表
     */
    
    @Override
    public List<ExpectedExpenseVo> list(Long userId, ExpectedExpenseBo expectedExpenseBo) {
        expectedExpenseBo.setUserId(userId);
        QueryWrapper<ExpectedExpense> queryWrapper = buildQueryWrapper(expectedExpenseBo);
        List<ExpectedExpense> expectedExpenses = this.list(queryWrapper);
        if (expectedExpenses != null && !expectedExpenses.isEmpty()) {
            List<ExpectedExpenseVo> expectedExpenseVos = new ArrayList<>();
            for (ExpectedExpense expectedExpense : expectedExpenses) {
                ExpectedExpenseVo expectedExpenseVo = new ExpectedExpenseVo();
                BeanUtils.copyProperties(expectedExpense, expectedExpenseVo);
                
                // 设置分类图标
                setCategoryIcon(expectedExpenseVo, expectedExpense);
                expectedExpenseVos.add(expectedExpenseVo);
            }
            return expectedExpenseVos;
        }
        
        return null;
    }
    /**
     * 更新预计支出
     * @param userId 用户 ID
     * @param expectedExpenseBo 预计支出业务对象
     * @return 更新后的预计支出 VO
     */
    @Override
    public ExpectedExpenseVo update(Long userId, ExpectedExpenseBo expectedExpenseBo) {
        // 1. 参数校验
        if (expectedExpenseBo == null) {
            throw new IllegalArgumentException("预计支出参数不能为空");
        }
        if (expectedExpenseBo.getId() == null) {
            throw new IllegalArgumentException("预计支出 ID 不能为空");
        }
        // 2. 查询记录是否存在
        ExpectedExpense existingExpense = this.getById(expectedExpenseBo.getId());
        if (existingExpense == null) {
            return null;
        }
        
        // 3. 已确认的记录不允许修改
        if (existingExpense.getStatus() == 2) {
            throw new IllegalArgumentException("已确认的预计支出不允许修改");
        }
        
        // 4. 更新字段（只更新非空字段）
        if (expectedExpenseBo.getAmount() != null) {
            if (expectedExpenseBo.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("预计支出金额必须大于 0");
            }
            existingExpense.setAmount(expectedExpenseBo.getAmount());
        }
        if (expectedExpenseBo.getCategoryId() != null) {
            existingExpense.setCategoryId(expectedExpenseBo.getCategoryId());
        }
        if (expectedExpenseBo.getSubCategoryId() != null) {
            existingExpense.setSubCategoryId(expectedExpenseBo.getSubCategoryId());
        }
        if (expectedExpenseBo.getRemark() != null) {
            existingExpense.setRemark(expectedExpenseBo.getRemark());
        }
        if (expectedExpenseBo.getDueDate() != null) {
            existingExpense.setDueDate(expectedExpenseBo.getDueDate());
        }
        if (expectedExpenseBo.getStatus() != null) {
            existingExpense.setStatus(expectedExpenseBo.getStatus());
        }
        
        // 5. 执行更新
        this.updateById(existingExpense);
        
        // 6. 返回 VO
        ExpectedExpenseVo result = new ExpectedExpenseVo();
        BeanUtils.copyProperties(existingExpense, result);
        return result;
    }
    
    
    @Override
    public boolean delete(Long id, Long userId) {
        if (id == null || userId == null) {
            throw new IllegalArgumentException("参数非法，无法删除预计支出");
        }
        ExpectedExpenseBo bo = new ExpectedExpenseBo();
        bo.setId(id);
        bo.setUserId(userId);
        ExpectedExpense existing = this.getOne(buildQueryWrapper(bo));
        if (existing == null) {
            return false;
        }
        return this.removeById(id);
    }
    
    @Override
    public boolean confirm(Long id, Long userId) {
        // 根据ID和用户ID获取预计支出记录
        ExpectedExpenseBo queryBo = new ExpectedExpenseBo();
        queryBo.setId(id);
        queryBo.setUserId(userId);
        ExpectedExpense expectedExpense = this.getOne(buildQueryWrapper(queryBo));
        
        if (expectedExpense == null) {
            return false;
        }
        if (expectedExpense.getStatus() == 2)return true;
        // 创建新的实际支出账单
        BillBo billBo = new BillBo();
        billBo.setUserId(expectedExpense.getUserId());
        billBo.setOriginalAmount(expectedExpense.getAmount());
        billBo.setRefundAmount(BigDecimal.ZERO);
        billBo.setType(1); // 支出
        billBo.setPaymentMethod("预计支出");
        billBo.setCategoryId(expectedExpense.getCategoryId());
        if (expectedExpense.getSubCategoryId() != null){
            billBo.setSubCategoryId(expectedExpense.getSubCategoryId());
        }
       if (expectedExpense.getRemark() != null){
           billBo.setRemark(expectedExpense.getRemark());
       }
        billBo.setBillTime(expectedExpense.getDueDate());
        if (billService.createBill(billBo)== null) {
            return false;
        }
        // 更新预计支出状态为已支付（2）
        expectedExpense.setStatus(2);
        return this.updateById(expectedExpense);
    }

    private void validateExpectedExpenseForCreate(ExpectedExpenseBo bo) {
        if (bo == null) {
            throw new IllegalArgumentException("预计支出参数不能为空");
        }
        if (bo.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (bo.getAmount() == null || bo.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("预计支出金额必须大于0");
        }
        if (bo.getCategoryId() == null) {
            throw new IllegalArgumentException("分类ID不能为空");
        }
    }
}
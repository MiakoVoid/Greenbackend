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
            
        if (expectedExpenseBo.getAmount() != null)
            queryWrapper.eq("amount", expectedExpenseBo.getAmount());
        if (expectedExpenseBo.getRemark()!=null)
            queryWrapper.eq("remark", expectedExpenseBo.getRemark());
        if (expectedExpenseBo.getDueDate() != null)
            queryWrapper.eq("due_date", expectedExpenseBo.getDueDate());
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
 * 验证预期支出对象的有效性
 * @param expectedExpense 待验证的预期支出对象
 * @return 如果对象无效返回true，有效返回false
 */
    private boolean isInvalid(ExpectedExpense expectedExpense) {
            // 检查必要字段是否为空
            if (expectedExpense == null) {
                return true;
            }
            
            // 检查用户ID是否存在
            if (expectedExpense.getUserId() == null) {
                return true;
            }
            
            // 检查金额是否有效（大于0）
            if (expectedExpense.getAmount() == null || expectedExpense.getAmount().compareTo(BigDecimal.valueOf(0.0)) <= 0) {
                return true;
            }
            
            // 检查分类ID是否存在
        return expectedExpense.getCategoryId() == null;
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
                expectedExpenseVos.add(expectedExpenseVo);
            }
            return expectedExpenseVos;
        }
        
        return null;
    }
    /**
     * 更新
     */
    @Override
    public ExpectedExpenseVo update(Long userId, ExpectedExpenseBo expectedExpenseBo) {
        expectedExpenseBo.setUserId(userId);
        ExpectedExpense expectedExpense = this.getOne(buildQueryWrapper(expectedExpenseBo));
        if (expectedExpense != null) {
            BeanUtils.copyProperties(expectedExpenseBo, expectedExpense);
            if (isInvalid(expectedExpense)) {
                throw new IllegalArgumentException("预计支出信息不合法");
            }
            this.updateById(expectedExpense);
            ExpectedExpenseVo expectedExpenseVo = new ExpectedExpenseVo();
            BeanUtils.copyProperties(expectedExpense, expectedExpenseVo);
            return expectedExpenseVo;
        }
        return null;
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
        // 创建新的实际支出账单
        BillBo billBo = new BillBo();
        billBo.setUserId(expectedExpense.getUserId());
        billBo.setOriginalAmount(expectedExpense.getAmount());
        billBo.setRefundAmount(BigDecimal.ZERO);
        billBo.setType(1); // 支出
        Category category = categoryService.getById(expectedExpense.getCategoryId());
        billBo.setCategoryId(category.getId());
        billBo.setMerchant(category.getName());
        billBo.setSubCategoryId(expectedExpense.getSubCategoryId());
        billBo.setRemark(expectedExpense.getRemark());
        billBo.setBillTime(expectedExpense.getDueDate());
        if (billService.createBill(billBo)== null) {
            return false;
        }
        // 更新预计支出状态为已支付（2）
        expectedExpense.setStatus(2);
        if (isInvalid(expectedExpense)){
            return false;
        }
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
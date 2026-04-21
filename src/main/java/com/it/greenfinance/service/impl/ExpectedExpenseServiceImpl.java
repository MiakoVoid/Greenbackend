package com.it.greenfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.greenfinance.mapper.ExpectedExpenseMapper;
import com.it.greenfinance.pojo.Category;
import com.it.greenfinance.pojo.ExpectedExpense;
import com.it.greenfinance.pojo.SubCategory;
import com.it.greenfinance.pojo.bo.BillBo;
import com.it.greenfinance.pojo.bo.ExpectedExpenseBo;
import com.it.greenfinance.pojo.vo.ExpectedExpenseVo;
import com.it.greenfinance.service.BillService;
import com.it.greenfinance.service.CategoryService;
import com.it.greenfinance.service.ExpectedExpenseService;
import com.it.greenfinance.service.SubCategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
    private final SubCategoryService subCategoryService;
    
    public ExpectedExpenseServiceImpl(BillService billService, CategoryService categoryService, SubCategoryService subCategoryService) {
        this.billService = billService;
        this.categoryService = categoryService;
        this.subCategoryService = subCategoryService;
    }
    
    @Override
    public ExpectedExpenseVo listByIds(Long id, Long userId) {
        ExpectedExpenseBo expectedExpenseBo = new ExpectedExpenseBo().setId(id).setUserId(userId);
        ExpectedExpense expectedExpense = this.getOne(buildQueryWrapper(expectedExpenseBo));
        return expectedExpense != null ? convertToVo(expectedExpense) : null;
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper<ExpectedExpense> buildQueryWrapper(ExpectedExpenseBo bo) {
        QueryWrapper<ExpectedExpense> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(bo.getId() != null, "id", bo.getId())
                .eq(bo.getUserId() != null, "user_id", bo.getUserId())
                .eq(bo.getCategoryId() != null, "category_id", bo.getCategoryId())
                .eq(bo.getSubCategoryId() != null, "sub_category_id", bo.getSubCategoryId())
                .eq(bo.getStatus() != null, "status", bo.getStatus())
                .eq(bo.getUpdateTime() != null, "update_time", bo.getUpdateTime());

        // 匹配创建时间的年和月
        if (bo.getYear() != null && bo.getMonth() != null) {
            queryWrapper.apply("DATE_FORMAT(create_time, '%Y-%m') = CONCAT({0}, '-', LPAD({1}, 2, '0'))",
                    bo.getYear(), bo.getMonth());
        }

        queryWrapper.orderByDesc("create_time");
        return queryWrapper;
    }

    @Override
    public ExpectedExpenseVo create(ExpectedExpenseBo expectedExpenseBo) {
        validateExpectedExpenseForCreate(expectedExpenseBo);

        ExpectedExpense expectedExpense = new ExpectedExpense();
        BeanUtils.copyProperties(expectedExpenseBo, expectedExpense);
        this.save(expectedExpense);
        
        return convertToVo(expectedExpense);
    }

    /**
     * 将实体转换为 VO 并设置图标和分类名称
     */
    private ExpectedExpenseVo convertToVo(ExpectedExpense expense) {
        ExpectedExpenseVo vo = new ExpectedExpenseVo();
        BeanUtils.copyProperties(expense, vo);
        
        // 优先使用子分类图标和名称
        if (expense.getSubCategoryId() != null) {
            SubCategory subCategory = subCategoryService.getById(expense.getSubCategoryId());
            if (subCategory != null) {
                vo.setSubCategoryName(subCategory.getName());
                if (subCategory.getCategoryIcon() != null) {
                    vo.setCategoryIcon(subCategory.getCategoryIcon());
                    // 同时获取主分类名称
                    if (expense.getCategoryId() != null) {
                        Category category = categoryService.getById(expense.getCategoryId());
                        if (category != null) {
                            vo.setCategoryName(category.getName());
                        }
                    }
                    return vo;
                }
            }
        }
        
        // 其次使用主分类图标和名称
        if (expense.getCategoryId() != null) {
            Category category = categoryService.getById(expense.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
                if (category.getCategoryIcon() != null) {
                    vo.setCategoryIcon(category.getCategoryIcon());
                }
            }
        }
        return vo;
    }

    @Override
    public List<ExpectedExpenseVo> list(Long userId, ExpectedExpenseBo expectedExpenseBo) {
        expectedExpenseBo.setUserId(userId);
        List<ExpectedExpense> list = this.list(buildQueryWrapper(expectedExpenseBo));
        
        if (list == null || list.isEmpty()) {
            return null;
        }
        
        return list.stream().map(this::convertToVo).collect(Collectors.toList());
    }

    @Override
    public ExpectedExpenseVo update(Long userId, ExpectedExpenseBo bo) {
        if (bo == null || bo.getId() == null) {
            throw new IllegalArgumentException("预计支出 ID 不能为空");
        }

        ExpectedExpense existing = this.getById(bo.getId());
        if (existing == null) {
            return null;
        }
        
        if (existing.getStatus() == 2) {
            throw new IllegalArgumentException("已确认的预计支出不允许修改");
        }
        
        // 更新字段
        if (bo.getAmount() != null) {
            if (bo.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("预计支出金额必须大于 0");
            }
            existing.setAmount(bo.getAmount());
        }
        if (bo.getCategoryId() != null) existing.setCategoryId(bo.getCategoryId());
        if (bo.getSubCategoryId() != null) existing.setSubCategoryId(bo.getSubCategoryId());
        if (bo.getRemark() != null) existing.setRemark(bo.getRemark());
        if (bo.getDueDate() != null) existing.setDueDate(bo.getDueDate());
        if (bo.getStatus() != null) existing.setStatus(bo.getStatus());
        
        this.updateById(existing);
        return convertToVo(existing);
    }

    @Override
    public boolean delete(Long id, Long userId) {
        if (id == null || userId == null) {
            throw new IllegalArgumentException("参数非法，无法删除预计支出");
        }
        ExpectedExpense existing = this.getOne(new QueryWrapper<ExpectedExpense>()
                .eq("id", id).eq("user_id", userId));
        return existing != null && this.removeById(id);
    }

    @Override
    public boolean confirm(Long id, Long userId) {
        ExpectedExpense expense = this.getOne(new QueryWrapper<ExpectedExpense>()
                .eq("id", id).eq("user_id", userId));
        
        if (expense == null) {
            return false;
        }
        if (expense.getStatus() == 2) return true;

        // 创建实际账单
        BillBo billBo = new BillBo()
                .setUserId(expense.getUserId())
                .setOriginalAmount(expense.getAmount())
                .setRefundAmount(BigDecimal.ZERO)
                .setType(1)
                .setPaymentMethod("预计支出")
                .setCategoryId(expense.getCategoryId())
                .setSubCategoryId(expense.getSubCategoryId())
                .setRemark(expense.getRemark())
                .setBillTime(expense.getDueDate());

        if (billService.createBill(billBo) == null) {
            return false;
        }

        return this.updateById(new ExpectedExpense().setId(id).setStatus(2));
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
        if (bo.getDueDate() == null) {
            throw new IllegalArgumentException("预计支付日期不能为空");
        }
    }
}
package com.it.greenfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.injector.methods.SelectOne;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.greenfinance.mapper.BillMapper;
import com.it.greenfinance.mapper.BudgetMapper;
import com.it.greenfinance.mapper.ExpectedExpenseMapper;
import com.it.greenfinance.mapper.SystemConfigMapper;
import com.it.greenfinance.pojo.Budget;
import com.it.greenfinance.pojo.bo.BudgetBo;
import com.it.greenfinance.pojo.vo.BudgetStatisticsVo;
import com.it.greenfinance.pojo.vo.BudgetVo;
import com.it.greenfinance.service.BudgetService;
import com.it.utils.UserContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Date;

/**
 * <p>
 * 月度预算表 服务实现类
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Service
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, Budget> implements BudgetService {
    
    private final UserContextUtil userContextUtil;
    private final BillMapper billMapper;
    private final ExpectedExpenseMapper expectedExpenseMapper;
    private final SystemConfigMapper systemConfigMapper;
    
    public BudgetServiceImpl(UserContextUtil userContextUtil, BillMapper billMapper, ExpectedExpenseMapper expectedExpenseMapper, SystemConfigMapper systemConfigMapper) {
        this.userContextUtil = userContextUtil;
        this.billMapper = billMapper;
        this.expectedExpenseMapper = expectedExpenseMapper;
        this.systemConfigMapper = systemConfigMapper;
    }
    
    @Override
    public BudgetVo create(BudgetBo budgetBo) {
        Budget budget = new Budget();
        BeanUtils.copyProperties(budgetBo, budget);
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再设置预算");
        }
        budget.setUserId(userId);
        isValid( budget);
        save(budget);
        
        BudgetVo vo = convertToVo(budget);
        
        return vo;
    }
    
    /**
     * 获取预算列表
     *
     * @param page       页码
     * @param size       每页大小
     * @return 预算列表
     */
    @Override
    public Page<BudgetVo> list(Integer page, Integer size,BudgetBo bo) {
        Page<Budget> budgetPage = new Page<>(page, size);
        Page<Budget> resultPage = page(budgetPage, buildQueryWrapper(bo));
        Page<BudgetVo> budgetVoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        budgetVoPage.setRecords(resultPage.getRecords().stream().map(this::convertToVo).collect(java.util.stream.Collectors.toList()));
        
        return budgetVoPage;
    }
    
    @Override
    public BudgetVo getDetail(BudgetBo bo) {
        Budget budget = getOne(buildQueryWrapper(bo));
        
        if (budget != null) {
            return convertToVo(budget);
        }
        return null;
    }
    
    @Override
    public BudgetVo update(BudgetBo budgetBo) {
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再更新预算");
        }
        if (budgetBo == null)
            throw new IllegalArgumentException("参数错误");
       if (budgetBo.getId() == null){
           Budget entity = getOne(buildQueryWrapper(budgetBo));
           if (entity == null) {
               throw new IllegalArgumentException("预算不存在");
           }
           budgetBo.setId(entity.getId());
       }
        Budget budget = new Budget();
        BeanUtils.copyProperties(budgetBo, budget);
        // 确认记录归属当前用户
        Budget existing = getById(budget.getId());
        if (existing == null || existing.getUserId() == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("预算不存在或无权修改");
        }
        budget.setUserId(userId);
        isValid( budget);
        updateById(budget);
        
        BudgetVo vo = convertToVo(budget);
        
        return vo;
    }
    
    @Override
    public boolean removeById(Long id) {
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再删除预算");
        }
        // 检查预算是否属于当前用户
        Budget budget = getById(id);
        if (budget == null) {
            return false;
        }
        
        // 只能删除自己的预算
        if (!budget.getUserId().equals(userId)) {
            return false;
        }
        
        // 删除预算
        boolean removed = super.removeById(id);
        
        return removed;
    }
    
    
    @Override
    public BudgetStatisticsVo getStatistics(BudgetBo bo) {
        if(bo.getYear()== null|| bo.getMonth()== null)
            throw new IllegalArgumentException("参数错误");
        LocalDate currentDate = LocalDate.now();
        
        // 获取账单周期起始日和结束日
        int billMonthStartDay = Integer.parseInt(systemConfigMapper.getBillMonthStartDay(bo.getUserId()));
        LocalDate firstDayOfBillCycle = getFirstDayOfBillCycle(currentDate, billMonthStartDay);
        LocalDate lastDayOfBillCycle = getDayOfMonth(currentDate, billMonthStartDay);
        
        // 获取本月预算
        Budget budget = getOne(buildQueryWrapper(bo));
        
        BigDecimal totalBudget = BigDecimal.ZERO;
        if (budget != null) {
            totalBudget = budget.getAmount();
        }
        
        // 获取本月已使用的支出（按账单周期计算，不包括今天）
        Date today = java.sql.Date.valueOf(currentDate);
        BigDecimal used = billMapper.getBillCycleAmountByType(
            bo.getUserId(), 
            java.sql.Date.valueOf(firstDayOfBillCycle), 
            today, 
            1
        );
        
        // 获取今天的支出
        BigDecimal todayExpense = billMapper.getDayAmountByType(bo.getUserId(), today, 1);
        used = used.subtract(todayExpense); // 从已使用中排除今天的支出
        
        // 获取本月预计支出
        BigDecimal expected = expectedExpenseMapper.getMonthExpectedAmount(bo.getUserId(), today);
        
        // 计算剩余金额 = 总预算 - 已使用 - 本月预计支出
        BigDecimal remaining = totalBudget.subtract(used).subtract(expected);
        
        // 计算每日预算 = 剩余预算 / 剩余天数
        long remainingDaysLong = java.time.temporal.ChronoUnit.DAYS.between(currentDate, lastDayOfBillCycle) + 1;
        int remainingDays = (int) Math.max(0, remainingDaysLong); // 确保不会为负数
        BigDecimal daily = (remainingDays > 0) ? remaining.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        // 计算今日剩余金额 = 每日预算 - 今日支出
        BigDecimal todayRemaining = daily.subtract(todayExpense);
        
        BudgetStatisticsVo statistics = BudgetStatisticsVo.builder()
                .remaining(remaining)
                .daily(daily)
                .todayRemaining(todayRemaining)
                .expected(expected)
                .used(used)
                .build();
        
        return statistics;
    }

    /**
     * 创建预算前的合法性校验
     */
    private void isValid(Budget entity) {
        if (entity == null) {
            throw new IllegalArgumentException("预算参数不能为空");
        }
        if (entity.getAmount() == null || entity.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("预算金额必须大于0");
        }
        if (entity.getYear() == null || entity.getMonth() == null) {
            throw new IllegalArgumentException("预算年份和月份不能为空");
        }
        if (entity.getMonth() < 1 || entity.getMonth() > 12) {
            throw new IllegalArgumentException("预算月份不合法");
        }
        // 检查是否已存在同年同月的其他预算记录
        QueryWrapper<Budget> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq(entity.getUserId() != null, "user_id", entity.getUserId())
                .eq("year", entity.getYear())
                .eq("month",entity.getMonth());
        if (entity.getId()!=null)
            checkWrapper.ne("id", entity.getId());
        if (count(checkWrapper) > 0) {
            throw new IllegalArgumentException("该年月的预算已存在，不能重复创建");
        }
    }
    
    
    private LocalDate getDayOfMonth(LocalDate currentDate, int billMonthStartDay) {
        LocalDate firstDayOfMonth;
        try {
            if (currentDate.getDayOfMonth() >= billMonthStartDay) {
                // 当前日期大于等于起始日，账单月为当前月
                firstDayOfMonth = LocalDate.of(currentDate.getYear(), currentDate.getMonthValue(), billMonthStartDay);
            } else {
                // 当前日期小于起始日，账单月为上个月
                LocalDate prevMonth = currentDate.minusMonths(1);
                // 处理月末日期的边界情况
                int actualDay = Math.min(billMonthStartDay, prevMonth.lengthOfMonth());
                firstDayOfMonth = LocalDate.of(prevMonth.getYear(), prevMonth.getMonthValue(), actualDay);
            }
        } catch (Exception e) {
            // 如果日期计算出现异常，则使用默认日期
            firstDayOfMonth = LocalDate.of(currentDate.getYear(), currentDate.getMonthValue(), 1);
        }
        
        LocalDate lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1);
        return lastDayOfMonth;
    }
    
    /**
     * 获取账单周期的起始日期
     * @param currentDate 当前日期
     * @param billMonthStartDay 账单月起始日
     * @return 账单周期起始日期
     */
    private LocalDate getFirstDayOfBillCycle(LocalDate currentDate, int billMonthStartDay) {
        try {
            if (currentDate.getDayOfMonth() >= billMonthStartDay) {
                // 当前日期大于等于起始日，账单周期从本月起始日开始
                return LocalDate.of(currentDate.getYear(), currentDate.getMonthValue(), billMonthStartDay);
            } else {
                // 当前日期小于起始日，账单周期从上月起始日开始
                LocalDate prevMonth = currentDate.minusMonths(1);
                int actualDay = Math.min(billMonthStartDay, prevMonth.lengthOfMonth());
                return LocalDate.of(prevMonth.getYear(), prevMonth.getMonthValue(), actualDay);
            }
        } catch (Exception e) {
            // 如果日期计算出现异常，则使用默认日期
            return LocalDate.of(currentDate.getYear(), currentDate.getMonthValue(), 1);
        }
    }
    
    /**
     * 将Budget实体转换为BudgetVo
     * @param budget Budget实体
     * @return BudgetVo对象
     */
    private BudgetVo convertToVo(Budget budget) {
        BudgetVo budgetVo = new BudgetVo();
        BeanUtils.copyProperties(budget, budgetVo);
        return budgetVo;
    }
    
    

    private QueryWrapper<Budget> buildQueryWrapper(BudgetBo bo) {
        // 创建查询条件构造器实例
        QueryWrapper<Budget> queryWrapper = new QueryWrapper<>();
        // 设置用户ID相等条件
        queryWrapper.eq(bo.getUserId() != null, "user_id", bo.getUserId());
        // 如果年份不为空，则添加年份相等条件
        if (bo.getYear() != null) {
            queryWrapper.eq("year", bo.getYear());
        }
        //根据id获得详细预算
        if (bo.getId() != null)
            queryWrapper.eq("id", bo.getId());
        // 如果月份不为空，则添加月份相等条件
        if (bo.getMonth() != null) {
            queryWrapper.eq("month", bo.getMonth());
        }
        // 按年份和月份倒序排列结果
        queryWrapper.orderByDesc("year", "month");
        
        // 返回构建好查询条件构造器
        return queryWrapper;
    }
}

package com.it.greenfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.greenfinance.mapper.BillMapper;
import com.it.greenfinance.mapper.CategoryMapper;
import com.it.greenfinance.mapper.SubCategoryMapper;
import com.it.greenfinance.mapper.SystemConfigMapper;
import com.it.greenfinance.pojo.Bill;
import com.it.greenfinance.pojo.Category;
import com.it.greenfinance.pojo.SubCategory;
import com.it.greenfinance.pojo.bo.BillBo;
import com.it.greenfinance.pojo.bo.BudgetBo;
import com.it.greenfinance.pojo.vo.BillVo;
import com.it.greenfinance.pojo.vo.BudgetStatisticsVo;
import com.it.greenfinance.service.BillService;
import com.it.greenfinance.service.BudgetService;
import com.it.utils.UserContextUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 账单记录表 服务实现类
 * </p>
 *
 * @author Lingma
 * @since 2025-10-25
 */

@Service
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements BillService {
    
    private final CategoryMapper categoryMapper;
    private final SubCategoryMapper subCategoryMapper;
    private final UserContextUtil userContextUtil;
    private final BudgetService budgetService;
    private final SystemConfigMapper systemConfigMapper;
    
    public BillServiceImpl(CategoryMapper categoryMapper, SubCategoryMapper subCategoryMapper, UserContextUtil userContextUtil, BudgetService budgetService, SystemConfigMapper systemConfigMapper) {
        this.categoryMapper = categoryMapper;
        this.subCategoryMapper = subCategoryMapper;
        this.userContextUtil = userContextUtil;
        this.budgetService = budgetService;
        this.systemConfigMapper = systemConfigMapper;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillVo createBill(BillBo billBo) {
        if (billBo == null) {
            throw new IllegalArgumentException("账单参数不能为空");
        }
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再创建账单");
        }

        Bill bill = new Bill();
        BeanUtils.copyProperties(billBo, bill);
        bill.setUserId(userId);

        // 验证必需字段
        if (bill.getOriginalAmount() == null) {
            throw new IllegalArgumentException("原始金额不能为空");
        }
        if (bill.getType() == null) {
            throw new IllegalArgumentException("账单类型不能为空");
        }
        if (bill.getCategoryId() == null) {
            throw new IllegalArgumentException("分类ID不能为空");
        }
        
        // 计算并校验金额
        calculateAndValidateAmount(bill);
        
        if (bill.getBillTime() == null) {
            bill.setBillTime(new Date());
        }
        save(bill);
        
        return convertToVo(bill);
    }
    
    private void calculateAndValidateAmount(Bill bill) {
        if (bill.getRefundAmount() == null) {
            bill.setRefundAmount(BigDecimal.ZERO);
        }
        
        // 验证退款金额不能超过原始金额
        if (bill.getRefundAmount().compareTo(bill.getOriginalAmount()) > 0) {
            throw new IllegalArgumentException("退款金额不能超过原始金额");
        }
        
        bill.setAmount(bill.getOriginalAmount().subtract(bill.getRefundAmount()));
    }
    
    /**
     * 获取账单列表
     *
     * @param page       页码
     * @param size       每页大小
     * @param billBo     账单查询条件
     * @return 账单列表
     */
    @Override
    public Page<BillVo> getBillsByPage(Integer page, Integer size, BillBo billBo) {
        Long userId = userContextUtil.getCurrentUserId();
        Page<Bill> billPage = new Page<>(page, size);
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(userId != null, "user_id", userId);
        
        // 时间筛选
        if (billBo.getBillTime() != null) {
            queryWrapper.like("bill_time", billBo.getBillTime());
        }
        // 时间范围筛选
        if (billBo.getStartTime() != null) {
            queryWrapper.ge("bill_time", billBo.getStartTime());
        }
        if (billBo.getEndTime() != null) {
            queryWrapper.le("bill_time", billBo.getEndTime());
        }
        // 金额匹配
        if (billBo.getOriginalAmount() != null) {
            queryWrapper.like("original_amount", billBo.getOriginalAmount());
        }
        if (billBo.getRefundAmount() != null) {
            queryWrapper.like("refund_amount", billBo.getRefundAmount());
            if (billBo.getOriginalAmount() != null) {
                BigDecimal calculatedAmount = billBo.getOriginalAmount().subtract(billBo.getRefundAmount());
                queryWrapper.like("amount", calculatedAmount);
            }
        }
        // 分类筛选
        if (billBo.getCategoryId() != null) {
            queryWrapper.eq("category_id", billBo.getCategoryId());
        }
        
        // 类型筛选
        if (billBo.getType() != null) {
            queryWrapper.eq("type", billBo.getType());
        }
        
        // 商户筛选
        if (billBo.getMerchant() != null && !billBo.getMerchant().isEmpty()) {
            queryWrapper.like("merchant", billBo.getMerchant());
        }
        
        // 按时间倒序排列
        queryWrapper.orderByDesc("bill_time");
        
        Page<Bill> resultPage = page(billPage, queryWrapper);
            
        // 转换为 VO（复用转换方法）
        return convertToBillVoPage(resultPage);
    }
    
    @Override
    public BillVo getBillDetail(Long id) {
        Long userId = userContextUtil.getCurrentUserId();
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        queryWrapper.eq(userId != null, "user_id", userId);
        Bill bill = getOne(queryWrapper);
        
        if (bill != null) {
            return convertToVo(bill);
        }
        return null;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillVo updateBill(BillBo billBo) {
        if (billBo == null || billBo.getId() == null) {
            throw new IllegalArgumentException("账单ID不能为空");
        }
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再更新账单");
        }
        Bill existing = getById(billBo.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("账单不存在或无权修改");
        }
        
        // 校验分类从属关系
        validateCategoryRelation(billBo.getCategoryId(), billBo.getSubCategoryId());

        Bill bill = new Bill();
        BeanUtils.copyProperties(billBo, bill);
        bill.setUserId(userId);
        
        // 如果原始金额或退款金额有更新，则重新计算并校验
        if (bill.getRefundAmount() != null || bill.getOriginalAmount() != null) {
            if (bill.getRefundAmount() == null) {
                bill.setRefundAmount(existing.getRefundAmount());
            }
            if (bill.getOriginalAmount() == null) {
                bill.setOriginalAmount(existing.getOriginalAmount());
            }
            calculateAndValidateAmount(bill);
        }
        
        bill.setUpdateTime(new Date());
        updateById(bill);
        
        return convertToVo(bill);
    }

    private void validateCategoryRelation(Long categoryId, Long subCategoryId) {
        if (subCategoryId != null && categoryId != null) {
            SubCategory sc = subCategoryMapper.selectById(subCategoryId);
            if (sc == null || !sc.getCategoryId().equals(categoryId)) {
                throw new IllegalArgumentException("子分类不存在或不属于该主分类");
            }
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeBillById(Long id) {
        Long userId = userContextUtil.getCurrentUserId();
        // 检查账单是否属于当前用户
        Bill bill = getById(id);
        if (bill == null) {
            return false;
        }
        
        // 只能删除自己的账单
        if (!bill.getUserId().equals(userId)) {
            return false;
        }
        
        // 删除账单
        removeById(id);
        
        return true;
    }
    
/**
 * 获取账单统计信息的方法
 * 包括今日支出、本月收入、本月支出以及预算相关信息
 * @return 包含各类统计数据的 Map，键为统计类型，值为对应金额
 */
    @Override
    public Map<String, BigDecimal> getBillStatistics() {
    // 获取当前用户 ID
        Long userId = userContextUtil.getCurrentUserId();
    // 获取当前日期
        LocalDate today = LocalDate.now();
        
    // 创建用于存储统计结果的 Map
        Map<String, BigDecimal> statistics = new HashMap<>();
        
        // 获取今日支出金额并添加到统计结果中
        BigDecimal todayExpense = baseMapper.getDayAmountByType(userId, java.sql.Date.valueOf(today), 1);
        statistics.put("todayExpense", todayExpense);
        
        int billMonthStartDay = Integer.parseInt(systemConfigMapper.getBillMonthStartDay(userId));
        LocalDate firstDayOfBillCycle = getFirstDayOfBillCycle(today, billMonthStartDay);
        LocalDate lastDayOfBillCycle = getDayOfMonth(today, billMonthStartDay);
        
        // 获取本月收入金额并添加到统计结果中（按账单周期计算）
        BigDecimal monthIncome = baseMapper.getBillCycleAmountByType(
            userId, 
            java.sql.Date.valueOf(firstDayOfBillCycle), 
            java.sql.Date.valueOf(lastDayOfBillCycle), 
            2
        );
        statistics.put("monthIncome", monthIncome);
        
        // 获取本月支出金额并添加到统计结果中（按账单周期计算）
        BigDecimal monthExpense = baseMapper.getBillCycleAmountByType(
            userId, 
            java.sql.Date.valueOf(firstDayOfBillCycle), 
            java.sql.Date.valueOf(lastDayOfBillCycle), 
            1
        );
        statistics.put("monthExpense", monthExpense);
        
        // 调用预算服务获取预算信息 获得本月预算、已使用预算、每日预算、本日剩余预算
        BudgetBo budgetBo = BudgetBo.builder()
                .userId(userId)
                .year(today.getYear())
                .month(today.getMonthValue())
                .build();
        BudgetStatisticsVo budgetStatistics = budgetService.getStatistics(budgetBo);
        
        // 本月预算总金额
        statistics.put("monthBudget", budgetStatistics.getRemaining().add(budgetStatistics.getUsed()).add(budgetStatistics.getExpected()));
        // 已使用预算
        statistics.put("budgetUsed", budgetStatistics.getUsed());
        // 每日预算
        statistics.put("dailyBudget", budgetStatistics.getDaily());
        // 本日剩余预算
        statistics.put("todayRemaining", budgetStatistics.getTodayRemaining());
        
        return statistics;
    }
    
    /**
     * 模糊查询账单（支持备注、商户名、金额）
     * @param page 页码
     * @param size 每页条数
     * @param billBo 查询条件（包含 keyword）
     * @return 账单分页数据
     */
    @Override
    public Page<BillVo> searchBills(Integer page, Integer size, BillBo billBo) {
        Long userId = userContextUtil.getCurrentUserId();
        Page<Bill> billPage = new Page<>(page, size);
        QueryWrapper<Bill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(userId != null, "user_id", userId);
        
        // 关键字模糊查询（同时查询备注、商户名、金额）
        if (billBo.getKeyword() != null && !billBo.getKeyword().trim().isEmpty()) {
            String keyword = billBo.getKeyword().trim();
            queryWrapper.and(wrapper -> 
                wrapper.like("remark", keyword)
                       .or()
                       .like("merchant", keyword)
                       .or()
                       .like("original_amount", keyword)
                       .or()
                       .like("refund_amount", keyword)
                       .or()
                       .like("amount", keyword)
            );
        }
        
        // 时间筛选
        if (billBo.getBillTime() != null) {
            queryWrapper.like("bill_time", billBo.getBillTime());
        }
        // 时间范围筛选
        if (billBo.getStartTime() != null) {
            queryWrapper.ge("bill_time", billBo.getStartTime());
        }
        if (billBo.getEndTime() != null) {
            queryWrapper.le("bill_time", billBo.getEndTime());
        }
        
        // 分类筛选
        if (billBo.getCategoryId() != null) {
            queryWrapper.eq("category_id", billBo.getCategoryId());
        }
        
        // 类型筛选
        if (billBo.getType() != null) {
            queryWrapper.eq("type", billBo.getType());
        }
        
        // 按时间倒序排列
        queryWrapper.orderByDesc("bill_time");
        
        Page<Bill> resultPage = page(billPage, queryWrapper);
        
        // 转换为 VO（复用原有逻辑）
        return convertToBillVoPage(resultPage);
    }
    
    /**
     * 将 Bill 分页数据转换为 BillVo 分页数据
     */
    private Page<BillVo> convertToBillVoPage(Page<Bill> resultPage) {
        // 收集分类 ID，进行批量查询优化 N+1 问题
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> subCategoryIds = new HashSet<>();
        for (Bill bill : resultPage.getRecords()) {
            if (bill.getCategoryId() != null) categoryIds.add(bill.getCategoryId());
            if (bill.getSubCategoryId() != null) subCategoryIds.add(bill.getSubCategoryId());
        }

        Map<Long, String> categoryMap = new HashMap<>();
        Map<Long, String> categoryIconMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryMapper.selectBatchIds(categoryIds).forEach(c -> {
                categoryMap.put(c.getId(), c.getName());
                categoryIconMap.put(c.getId(), c.getCategoryIcon());
            });
        }

        Map<Long, String> subCategoryMap = new HashMap<>();
        Map<Long, String> subCategoryIconMap = new HashMap<>();
        if (!subCategoryIds.isEmpty()) {
            subCategoryMapper.selectBatchIds(subCategoryIds).forEach(sc -> {
                subCategoryMap.put(sc.getId(), sc.getName());
                subCategoryIconMap.put(sc.getId(), sc.getCategoryIcon());
            });
        }

        // 转换为 VO
        Page<BillVo> billVoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        billVoPage.setRecords(resultPage.getRecords().stream().map(bill -> {
            BillVo vo = new BillVo();
            BeanUtils.copyProperties(bill, vo);
            vo.setUserId(userContextUtil.getCurrentUserId());
            // 使用内存中的 Map 填充名称，避免循环查库
            if (bill.getCategoryId() != null) {
                vo.setCategoryName(categoryMap.get(bill.getCategoryId()));
            }
            if (bill.getSubCategoryId() != null) {
                vo.setSubCategoryName(subCategoryMap.get(bill.getSubCategoryId()));
            }
            // 设置图标标识符：优先使用子分类图标，如果没有则使用主分类图标
            if (bill.getSubCategoryId() != null && subCategoryIconMap.get(bill.getSubCategoryId()) != null) {
                vo.setCategoryIcon(subCategoryIconMap.get(bill.getSubCategoryId()));
            } else if (bill.getCategoryId() != null) {
                vo.setCategoryIcon(categoryIconMap.get(bill.getCategoryId()));
            }
            return vo;
        }).collect(Collectors.toList()));
        
        return billVoPage;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BillVo> createBills(List<BillBo> bills) {
        if (bills == null || bills.isEmpty()) {
            throw new IllegalArgumentException("账单列表不能为空");
        }
        
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再创建账单");
        }
        
        List<Bill> billEntities = bills.stream().map(billBo -> {
            Bill bill = new Bill();
            BeanUtils.copyProperties(billBo, bill);
            bill.setUserId(userId);
            
            // 校验字段
            if (bill.getOriginalAmount() == null || bill.getType() == null || bill.getCategoryId() == null) {
                throw new IllegalArgumentException("原始金额、类型和分类ID不能为空");
            }
            
            calculateAndValidateAmount(bill);
            if (bill.getBillTime() == null) {
                bill.setBillTime(new Date());
            }
            return bill;
        }).collect(Collectors.toList());
        
        saveBatch(billEntities);
        
        return billEntities.stream().map(this::convertToVo).collect(Collectors.toList());
    }
    
    /**
     * 将 Bill 实体转换为 BillVo
     * @param bill Bill 实体
     * @return BillVo 对象
     */
    private BillVo convertToVo(Bill bill) {
        BillVo billVo = new BillVo();
        BeanUtils.copyProperties(bill, billVo);
        billVo.setUserId(userContextUtil.getCurrentUserId());
        
        // 填充分类名称和图标
        fillCategoryAndIcon(bill, billVo);
        
        return billVo;
    }
    
    private void fillCategoryAndIcon(Bill bill, BillVo billVo) {
        if (bill.getSubCategoryId() != null) {
            SubCategory subCategory = subCategoryMapper.selectById(bill.getSubCategoryId());
            if (subCategory != null) {
                billVo.setSubCategoryName(subCategory.getName());
                billVo.setCategoryIcon(subCategory.getCategoryIcon());
            }
        }
        
        if (bill.getCategoryId() != null) {
            Category category = categoryMapper.selectById(bill.getCategoryId());
            if (category != null) {
                billVo.setCategoryName(category.getName());
                // 如果子分类没有图标，则使用主分类图标
                if (billVo.getCategoryIcon() == null) {
                    billVo.setCategoryIcon(category.getCategoryIcon());
                }
            }
        }
    }
    
    /**
     * 获取账单周期的结束日期（最后一天）
     * @param currentDate 当前日期
     * @param billMonthStartDay 账单月起始日
     * @return 账单周期结束日期
     */
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
}

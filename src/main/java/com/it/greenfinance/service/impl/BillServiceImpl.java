package com.it.greenfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.greenfinance.mapper.BillMapper;
import com.it.greenfinance.mapper.CategoryMapper;
import com.it.greenfinance.mapper.SubCategoryMapper;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    
    public BillServiceImpl(CategoryMapper categoryMapper, SubCategoryMapper subCategoryMapper, UserContextUtil userContextUtil, BudgetService budgetService) {
        this.categoryMapper = categoryMapper;
        this.subCategoryMapper = subCategoryMapper;
        this.userContextUtil = userContextUtil;
        this.budgetService = budgetService;
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
        
        if (bill.getRefundAmount() == null) {
            bill.setRefundAmount(BigDecimal.ZERO);
        }
        
        // 验证退款金额不能超过原始金额
        if (bill.getRefundAmount().compareTo(bill.getOriginalAmount()) > 0) {
            throw new IllegalArgumentException("退款金额不能超过原始金额");
        }
        
        if (bill.getOriginalAmount() != null && bill.getRefundAmount() != null) {
            bill.setAmount(bill.getOriginalAmount().subtract(bill.getRefundAmount()));
        }
        
        if (bill.getBillTime() == null) {
            bill.setBillTime(new Date());
        }
        save(bill);
        return convertToVo(bill);
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
        // 金额匹配
        if (billBo.getOriginalAmount()!= null) {
            queryWrapper.like("original_amount", billBo.getOriginalAmount());
        }
        if (billBo.getRefundAmount() != null){
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
        
        // 收集分类ID，进行批量查询优化N+1问题
        java.util.Set<Long> categoryIds = new java.util.HashSet<>();
        java.util.Set<Long> subCategoryIds = new java.util.HashSet<>();
        for (Bill bill : resultPage.getRecords()) {
            if (bill.getCategoryId() != null) categoryIds.add(bill.getCategoryId());
            if (bill.getSubCategoryId() != null) subCategoryIds.add(bill.getSubCategoryId());
        }

        Map<Long, String> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryMapper.selectBatchIds(categoryIds).forEach(c -> categoryMap.put(c.getId(), c.getName()));
        }

        Map<Long, String> subCategoryMap = new HashMap<>();
        if (!subCategoryIds.isEmpty()) {
            subCategoryMapper.selectBatchIds(subCategoryIds).forEach(sc -> subCategoryMap.put(sc.getId(), sc.getName()));
        }

        // 转换为VO
        Page<BillVo> billVoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        billVoPage.setRecords(resultPage.getRecords().stream().map(bill -> {
            BillVo vo = new BillVo();
            BeanUtils.copyProperties(bill, vo);
            vo.setUserId(userContextUtil.getCurrentUserId());
            // 使用内存中的Map填充名称，避免循环查库
            if (bill.getCategoryId() != null) {
                vo.setCategoryName(categoryMap.get(bill.getCategoryId()));
            }
            if (bill.getSubCategoryId() != null) {
                vo.setSubCategoryName(subCategoryMap.get(bill.getSubCategoryId()));
            }
            return vo;
        }).collect(java.util.stream.Collectors.toList()));
        
        return billVoPage;
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
        if (billBo.getSubCategoryId() != null && billBo.getCategoryId() != null) {
            SubCategory sc = subCategoryMapper.selectById(billBo.getSubCategoryId());
            if (sc == null || !sc.getCategoryId().equals(billBo.getCategoryId())) {
                throw new IllegalArgumentException("子分类不存在或不属于该主分类");
            }
        }

        Bill bill = new Bill();
        BeanUtils.copyProperties(billBo, bill);
        bill.setUserId(userId);
        if (bill.getRefundAmount() != null || bill.getOriginalAmount() != null) {
            Bill existingBill = getById(bill.getId());
            if (bill.getRefundAmount() == null) {
                bill.setRefundAmount(existingBill.getRefundAmount());
            } else {
                bill.setRefundAmount(bill.getRefundAmount());
            }
            if (bill.getOriginalAmount() == null) {
                bill.setOriginalAmount(existingBill.getOriginalAmount());
            }
            
            // 确保两个值都不为null再进行计算
            if (bill.getOriginalAmount() != null && bill.getRefundAmount() != null) {
                bill.setAmount(bill.getOriginalAmount().subtract(bill.getRefundAmount()));
            }
            
            // 验证退款金额不能超过原始金额
            if (bill.getRefundAmount().compareTo(bill.getOriginalAmount()) > 0) {
                throw new IllegalArgumentException("退款金额不能超过原始金额");
            }
        }
        bill.setUpdateTime(new Date());
        updateById(bill);
        return convertToVo(bill);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "billStats", allEntries = true),
        @CacheEvict(value = "budgetStats", allEntries = true),
        @CacheEvict(value = "billDetail", key = "#id")
    })
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
 * @return 包含各类统计数据的Map，键为统计类型，值为对应金额
 */
    @Override
    @Cacheable(value = "billStats", key = "@userContextUtil.getCurrentUserId()")
    public Map<String, BigDecimal> getBillStatistics() {
    // 获取当前用户ID
        Long userId = userContextUtil.getCurrentUserId();
    // 获取当前日期
        Date now = new Date();
        
    // 创建用于存储统计结果的Map
        Map<String, BigDecimal> statistics = new HashMap<>();
        
        // 获取今日支出金额并添加到统计结果中
        BigDecimal todayExpense = baseMapper.getDayAmountByType(userId, now, 1);
        statistics.put("todayExpense", todayExpense);
        
        // 获取本月收入金额并添加到统计结果中
        BigDecimal monthIncome = baseMapper.getMonthAmountByType(userId, now, 2);
        statistics.put("monthIncome", monthIncome);
        
        // 获取本月支出金额并添加到统计结果中
        BigDecimal monthExpense = baseMapper.getMonthAmountByType(userId, now, 1);
        statistics.put("monthExpense", monthExpense);
        
        // 调用预算服务获取预算信息 获得本月预算、已使用预算、每日预算、本日剩余预算
        BudgetBo budgetBo = BudgetBo.builder()
                .userId(userId)
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
     * 将Bill实体转换为BillVo
     * @param bill Bill实体
     * @return BillVo对象
     */
    private BillVo convertToVo(Bill bill) {
        BillVo billVo = new BillVo();
        BeanUtils.copyProperties(bill, billVo);
        billVo.setUserId(userContextUtil.getCurrentUserId());
        setCategoryNames(bill, billVo);
        return billVo;
    }
    
   public void setCategoryNames(Bill bill, BillVo billVo) {
        if (bill.getCategoryId() != null) {
            Category category = categoryMapper.selectById(bill.getCategoryId());
            if (category != null) {
                billVo.setCategoryName(category.getName());
            }
        }
        if (bill.getSubCategoryId() != null) {
            SubCategory subCategory = subCategoryMapper.selectById(bill.getSubCategoryId());
            if (subCategory != null) {
                billVo.setSubCategoryName(subCategory.getName());
            }
        }
    }
}

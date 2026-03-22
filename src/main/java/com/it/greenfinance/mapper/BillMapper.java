package com.it.greenfinance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.it.greenfinance.pojo.Bill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 账单记录表 Mapper 接口
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Mapper
public interface BillMapper extends BaseMapper<Bill> {
    
    /**
     * 获取用户某天的支出总额
     * @param userId 用户ID
     * @param date 日期
     * @param type 账单类型(1-支出, 2-收入)
     * @return 金额总和
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM bill WHERE user_id = #{userId} AND type = #{type} AND DATE(bill_time) = DATE(#{date})")
    BigDecimal getDayAmountByType(@Param("userId") Long userId, @Param("date") Date date, @Param("type") Integer type);
    
    /**
     * 获取用户某月的账单总额
     * @param userId 用户 ID
     * @param date 日期
     * @param type 账单类型 (1-支出，2-收入)
     * @return 金额总和
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM bill WHERE user_id = #{userId} AND type = #{type} AND YEAR(bill_time) = YEAR(#{date}) AND MONTH(bill_time) = MONTH(#{date})")
    BigDecimal getMonthAmountByType(@Param("userId") Long userId, @Param("date") Date date, @Param("type") Integer type);
        
    /**
     * 获取用户在账单周期内的账单总额
     * @param userId 用户 ID
     * @param startDate 账单周期开始日期
     * @param endDate 账单周期结束日期
     * @param type 账单类型 (1-支出，2-收入)
     * @return 金额总和
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM bill WHERE user_id = #{userId} AND type = #{type} " +
            "AND DATE(bill_time) >= DATE(#{startDate}) AND DATE(bill_time) <= DATE(#{endDate})")
    BigDecimal getBillCycleAmountByType(@Param("userId") Long userId, 
                                        @Param("startDate") Date startDate, 
                                        @Param("endDate") Date endDate, 
                                        @Param("type") Integer type);
    
    /**
     * 获取用户某月按分类统计的支出金额
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份（1-12）
     * @return 分类支出统计（分类ID -> 金额）
     */
    @Select("SELECT c.id as categoryId, c.name as categoryName, COALESCE(SUM(b.amount), 0) as amount " +
            "FROM category c " +
            "INNER JOIN bill b ON c.id = b.category_id " +
            "WHERE b.user_id = #{userId} AND b.type = 1 " +
            "AND YEAR(b.bill_time) = #{year} AND MONTH(b.bill_time) = #{month} " +
            "AND (c.user_id = #{userId} OR c.user_id IS NULL) " +
            "GROUP BY c.id, c.name " +
            "HAVING amount > 0 " +
            "ORDER BY amount DESC")
    java.util.List<java.util.Map<String, Object>> getCategoryExpenseByMonth(@Param("userId") Long userId, 
                                                                              @Param("year") Integer year, 
                                                                              @Param("month") Integer month);
    
    /**
     * 获取用户近 N 天的每日收支统计
     * @param userId 用户 ID
     * @param days 天数
     * @return 每日收支统计列表
     */
    @Select("SELECT DATE(bill_time) as date, " +
            "COALESCE(SUM(CASE WHEN type = 2 THEN amount ELSE 0 END), 0) as income, " +
            "COALESCE(SUM(CASE WHEN type = 1 THEN amount ELSE 0 END), 0) as expense " +
            "FROM bill " +
            "WHERE user_id = #{userId} " +
            "AND bill_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(bill_time) " +
            "ORDER BY date ASC")
    java.util.List<java.util.Map<String, Object>> getDailyTrend(@Param("userId") Long userId, @Param("days") Integer days);
    
    /**
     * 获取用户在指定时间区间内按分类统计的支出金额
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分类支出统计列表
     */
    @Select("SELECT c.id as categoryId, c.name as categoryName, COALESCE(SUM(b.amount), 0) as amount " +
            "FROM category c " +
            "INNER JOIN bill b ON c.id = b.category_id " +
            "WHERE b.user_id = #{userId} AND b.type = 1 " +
            "AND DATE(b.bill_time) >= DATE(#{startDate}) AND DATE(b.bill_time) <= DATE(#{endDate}) " +
            "AND (c.user_id = #{userId} OR c.user_id IS NULL) " +
            "GROUP BY c.id, c.name " +
            "HAVING amount > 0 " +
            "ORDER BY amount DESC")
    java.util.List<java.util.Map<String, Object>> getCategoryExpenseByTimeRange(
            @Param("userId") Long userId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);
    
    /**
     * 获取用户在指定时间区间内的每日收支统计
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日收支统计列表
     */
    @Select("SELECT DATE(bill_time) as date, " +
            "COALESCE(SUM(CASE WHEN type = 2 THEN amount ELSE 0 END), 0) as income, " +
            "COALESCE(SUM(CASE WHEN type = 1 THEN amount ELSE 0 END), 0) as expense " +
            "FROM bill " +
            "WHERE user_id = #{userId} " +
            "AND DATE(bill_time) >= DATE(#{startDate}) AND DATE(bill_time) <= DATE(#{endDate}) " +
            "GROUP BY DATE(bill_time) " +
            "ORDER BY date ASC")
    java.util.List<java.util.Map<String, Object>> getDailyTrendByTimeRange(
            @Param("userId") Long userId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);
}
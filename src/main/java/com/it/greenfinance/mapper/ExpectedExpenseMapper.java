package com.it.greenfinance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.it.greenfinance.pojo.ExpectedExpense;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 预计支出表 Mapper 接口
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Mapper
public interface ExpectedExpenseMapper extends BaseMapper<ExpectedExpense> {
    
    /**
     * 获取用户某月的预计支出总额
     * @param userId 用户ID
     * @param date 日期
     * @return 金额总和
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM expected_expense WHERE user_id = #{userId} AND YEAR(due_date) = YEAR(#{date}) AND MONTH(due_date) = MONTH(#{date}) AND status = 1")
    BigDecimal getMonthExpectedAmount(@Param("userId") Long userId, @Param("date") Date date);
}
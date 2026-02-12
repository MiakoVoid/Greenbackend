package com.it.greenfinance.controller;


import com.it.greenfinance.pojo.bo.ExpectedExpenseBo;
import com.it.greenfinance.pojo.vo.ExpectedExpenseVo;
import com.it.greenfinance.service.ExpectedExpenseService;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 预计支出表 前端控制器
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Api(tags = "预计支出")
@RestController
@RequestMapping("/expected-expense")
public class ExpectedExpenseController {
    
    private final ExpectedExpenseService expectedExpenseService;
    private final UserContextUtil userContextUtil;
    
    public ExpectedExpenseController(ExpectedExpenseService expectedExpenseService, UserContextUtil userContextUtil) {
        this.expectedExpenseService = expectedExpenseService;
        this.userContextUtil = userContextUtil;
    }
    
    /**
     * 创建预计支出
     * @param expectedExpenseBo 预计支出信息
     * @return 创建结果
     */
    @PostMapping
    public Result createExpectedExpense(@RequestBody ExpectedExpenseBo expectedExpenseBo) {
        Long userId = userContextUtil.getCurrentUserId();
        expectedExpenseBo.setUserId(userId);
        ExpectedExpenseVo expectedExpenseVo = expectedExpenseService.create(expectedExpenseBo);
        return Result.ok("预计支出创建成功", expectedExpenseVo);
    }
    
    /**
     * 获取预计支出详情
     * @param id 预计支出ID
     * @return 预计支出详情
     */
    @GetMapping("/{id}")
    public Result getExpectedExpense(@PathVariable Long id) {
        Long userId = userContextUtil.getCurrentUserId();
        ExpectedExpenseVo expectedExpenseVo = expectedExpenseService.listByIds(id, userId);
        if (expectedExpenseVo != null) {
            return Result.ok(expectedExpenseVo);
        }
        return Result.error(404, "预计支出不存在");
    }
    
    /**
     * 获取预计支出列表
     * @param expectedExpenseBo 预计支出信息模板
     * @return 预计支出列表
     */
    @GetMapping("/list")
    public Result getExpectedExpenses(ExpectedExpenseBo expectedExpenseBo) {
        Long userId = userContextUtil.getCurrentUserId();
        List<ExpectedExpenseVo> expectedExpenseVos = expectedExpenseService.list(userId, expectedExpenseBo);
        return Result.ok(expectedExpenseVos);
    }
    
    /**
     * @param expectedExpenseBo 预计支出信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result updateExpectedExpense(@RequestBody ExpectedExpenseBo expectedExpenseBo) {
        Long userId = userContextUtil.getCurrentUserId();
        ExpectedExpenseVo expectedExpenseVo = expectedExpenseService.update(userId, expectedExpenseBo);
        if (expectedExpenseVo != null) {
            return Result.ok("预计支出更新成功", expectedExpenseVo);
        }
        return Result.error(404, "预计支出不存在或无权限操作");
    }
    
    /**
     * 删除预计支出
     * @param id 预计支出ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result deleteExpectedExpense(@PathVariable Long id) {
        Long userId = userContextUtil.getCurrentUserId();
        boolean result = expectedExpenseService.delete(id, userId);
        if (result) {
            return Result.ok("预计支出删除成功");
        }
        return Result.error(404, "预计支出不存在或无权限操作");
    }
    
    
    /**
     * 确认预计支出，创建实际支出
     */
    @PostMapping("/confirm/{id}")
    public Result confirmExpectedExpense(@PathVariable Long id) {
        Long userId = userContextUtil.getCurrentUserId();
        boolean result = expectedExpenseService.confirm(id, userId);
        if (result) {
            return Result.ok("预计支出确认成功");
        }
        return Result.error(404, "预计支出不存在或无权限操作");
    }
  
}
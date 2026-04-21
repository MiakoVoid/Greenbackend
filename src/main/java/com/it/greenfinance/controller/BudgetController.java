package com.it.greenfinance.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.greenfinance.pojo.bo.BudgetBo;
import com.it.greenfinance.pojo.vo.BudgetStatisticsVo;
import com.it.greenfinance.pojo.vo.BudgetVo;
import com.it.greenfinance.service.BudgetService;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 月度预算表 前端控制器
 * </p>
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Api(tags = "月度预算")
@RestController
@Validated
@RequestMapping("/budget")
public class BudgetController {
    
    private final BudgetService budgetService;
    private final UserContextUtil userContextUtil;
    
    public BudgetController(BudgetService budgetService, UserContextUtil userContextUtil) {
        this.budgetService = budgetService;
        this.userContextUtil = userContextUtil;
    }
    
    /**
     * 创建预算
     *
     * @param budgetBo 预算信息
     * @return 创建结果
     */
    @PostMapping
    public Result create(@Valid @RequestBody BudgetBo budgetBo) {
        BudgetVo vo = budgetService.create(budgetBo);
        return Result.ok("预算创建成功", vo);
    }
    
    /**
     * 获取预算列表
     *
     * @param page 页码
     * @param size 每页数量
     * @return 预算列表
     */
    @GetMapping
    public Result list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            BudgetBo bo) {
        bo.setUserId(userContextUtil.getCurrentUserId());
        Page<BudgetVo> voPage = budgetService.list(page, size, bo);
        return Result.ok(voPage);
    }
    
    /**
     * 获取预算详情
     *
     * @param id 预算ID
     * @return 预算详情
     */
    @GetMapping("/{id}")
    public Result getDetail(@PathVariable Long id) {
        BudgetBo bo = BudgetBo.builder()
                .id(id)
                .userId(userContextUtil.getCurrentUserId())
                .build();
        BudgetVo vo = budgetService.getDetail(bo);
        if (vo != null)
            return Result.ok(vo);
        return Result.error(404, "预算不存在");
    }
    
    /**
     * 更新预算
     *
     * @param budgetBo 更新的预算信息
     * @return 更新结果
     */
    @PutMapping
    public Result update(@Valid @RequestBody BudgetBo budgetBo) {
        budgetBo.setUserId(userContextUtil.getCurrentUserId());
        BudgetVo budgetVo = budgetService.update(budgetBo);
        return Result.ok("预算更新成功", budgetVo);
    }
    
    /**
     * 删除预算
     *
     * @param id 预算ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        boolean deleted = budgetService.removeById(id);
        if (deleted) {
            return Result.ok("预算删除成功");
        } else {
            return Result.error(403, "无权限删除该预算");
        }
    }
    
    /**
     * 获取预算统计信息
     *bo 传递年月
     * @return 预算统计信息
     */
    @GetMapping("/statistics")
    public Result getStatistics(BudgetBo bo) {
        bo.setUserId(userContextUtil.getCurrentUserId());
        BudgetStatisticsVo statistics = budgetService.getStatistics(bo);
        return Result.ok(statistics);
    }
}

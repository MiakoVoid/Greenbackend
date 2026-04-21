package com.it.greenfinance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.greenfinance.pojo.bo.BillBo;
import com.it.greenfinance.pojo.vo.BillVo;
import com.it.greenfinance.service.BillService;
import com.it.utils.Result;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 账单控制器
 *
 * @author ${author}
 * @since 2025-10-25
 */
@Api(tags = "账单接口")
@RestController
@Validated
@RequestMapping("/bills")
public class BillController {
    
    private final BillService billService;
    
    public BillController(BillService billService) {
        this.billService = billService;
    }
    
    /**
     * 创建账单
     *
     * @param billBo 账单信息
     * @return 创建结果
     */
    @PostMapping("/add")
    public Result createBill(@Valid @RequestBody BillBo billBo) {
        BillVo vo = billService.createBill(billBo);
        return Result.ok("账单创建成功", vo);
    }
    
    /**
     * 获取账单列表
     *
     * @param page 页码
     * @param size 每页数量
     * @param billBo 账单查询条件
     * @return 账单列表
     */
    @GetMapping("/list")
    public Result listBills(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size, BillBo billBo) {
        Page<BillVo> voPage = billService.getBillsByPage(page, size, billBo);
        return Result.ok(voPage);
    }
    
    /**
     * 获取账单详情
     *
     * @param id 账单ID
     * @return 账单详情
     */
    @GetMapping("/{id}")
    public Result getBillDetail(@PathVariable Long id) {
        BillVo vo = billService.getBillDetail(id);
        if (vo != null)
            return Result.ok(vo);
        return Result.error(404, "账单不存在");
    }
    
    /**
     * 更新账单
     *
     * @param billBo 更新的账单信息
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result updateBill(@Valid @RequestBody BillBo billBo) {
        BillVo billVo = billService.updateBill(billBo);
        return Result.ok("账单更新成功", billVo);
    }
    
    /**
     * 删除账单
     *
     * @param id 账单ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result deleteBill(@PathVariable Long id) {
        boolean deleted = billService.removeBillById(id);
        if (deleted) {
            return Result.ok("账单删除成功");
        } else {
            return Result.error(403, "无权限删除该账单");
        }
    }
    
    /**
     * 获取账单统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public Result getBillStatistics() {
        Map<String, BigDecimal> statistics = billService.getBillStatistics();
        return Result.ok(statistics);
    }
    
    /**
     * 模糊查询账单（支持备注、商户名、金额）
     *
     * @param page 页码
     * @param size 每页数量
     * @param keyword 搜索关键字
     * @return 账单列表
     */
    @GetMapping("/search")
    public Result searchBills(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            BillBo billBo) {
        // 将关键字设置到 billBo 中
        if (billBo == null) {
            billBo = new BillBo();
        }
        billBo.setKeyword(keyword);
        
        Page<BillVo> voPage = billService.searchBills(page, size, billBo);
        return Result.ok(voPage);
    }
}

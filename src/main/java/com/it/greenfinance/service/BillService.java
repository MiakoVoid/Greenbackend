package com.it.greenfinance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.it.greenfinance.pojo.Bill;
import com.it.greenfinance.pojo.bo.BillBo;
import com.it.greenfinance.pojo.vo.BillVo;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 账单记录表 服务类
 * </p>
 *
 * @author Lingma
 * @since 2025-10-25
 */
public interface BillService extends IService<Bill> {
    
    /**
     * 创建账单
     *
     * @param billBo 账单信息
     * @return 创建后的账单VO
     */
    BillVo createBill(BillBo billBo);
    
    /**
     * 通过文本识别数据创建账单
     *
     * @param textRecognitionBo 文本识别的账单信息
     * @return 创建后的账单VO
     */
    
    /**
     * 分页获取账单列表
     *
     * @param page 页码
     * @param size 每页条数
     * @param billBo 查询条件
     * @return 账单分页数据
     */
    Page<BillVo> getBillsByPage(Integer page, Integer size, BillBo billBo);
    
    /**
     * 获取账单详情
     *
     * @param id 账单ID
     * @return 账单详情
     */
    BillVo getBillDetail(Long id);
    
    /**
     * 更新账单
     *
     * @param billBo 账单信息
     * @return 更新后的账单
     */
    BillVo updateBill(BillBo billBo);
    
    /**
     * 根据ID删除账单
     *
     * @param id 账单ID
     * @return 是否删除成功
     */
    boolean removeBillById(Long id);
    
    Map<String, BigDecimal> getBillStatistics();
    
    /**
     * 模糊查询账单（支持备注、商户名、金额）
     *
     * @param page 页码
     * @param size 每页条数
     * @param billBo 查询条件（包含 keyword）
     * @return 账单分页数据
     */
    Page<BillVo> searchBills(Integer page, Integer size, BillBo billBo);
    
    List<BillVo> createBills(@Valid List<BillBo> bills);
}
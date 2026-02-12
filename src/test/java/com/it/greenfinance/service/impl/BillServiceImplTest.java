package com.it.greenfinance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.greenfinance.mapper.BillMapper;
import com.it.greenfinance.mapper.CategoryMapper;
import com.it.greenfinance.mapper.SubCategoryMapper;
import com.it.greenfinance.pojo.Bill;
import com.it.greenfinance.pojo.Category;
import com.it.greenfinance.pojo.SubCategory;
import com.it.greenfinance.pojo.bo.BillBo;
import com.it.greenfinance.pojo.vo.BillVo;
import com.it.utils.UserContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillServiceImplTest {

    @Mock
    private BillMapper billMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private SubCategoryMapper subCategoryMapper;

    @Mock
    private UserContextUtil userContextUtil;

    @InjectMocks
    private BillServiceImpl billService;

    private Long userId = 123L;

    @BeforeEach
    void setUp() throws Exception {
        // 使用反射手动注入 baseMapper 到 ServiceImpl 父类中
        // 解决 ServiceImpl.baseMapper 为空导致 NPE 的问题
        java.lang.reflect.Field baseMapperField = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(billService, billMapper);
    }

    @Test
    void createBill_Success() {
        // Arrange
        BillBo billBo = new BillBo();
        billBo.setOriginalAmount(new BigDecimal("100.00"));
        billBo.setType(1);
        billBo.setCategoryId(1L);

        when(userContextUtil.getCurrentUserId()).thenReturn(userId);
        when(billMapper.insert(any(Bill.class))).thenReturn(1);
        when(categoryMapper.selectById(1L)).thenReturn(new Category()); // Mock category lookup

        // Act
        BillVo result = billService.createBill(billBo);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        verify(billMapper).insert(any(Bill.class));
    }

    @Test
    void createBill_WithRefund() {
        // Arrange
        BillBo billBo = new BillBo();
        billBo.setOriginalAmount(new BigDecimal("100.00"));
        billBo.setRefundAmount(new BigDecimal("20.00"));
        billBo.setType(1);

        when(userContextUtil.getCurrentUserId()).thenReturn(userId);
        when(billMapper.insert(any(Bill.class))).thenReturn(1);

        // Act
        BillVo result = billService.createBill(billBo);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("80.00"), result.getAmount()); // 100 - 20
    }

    @Test
    void getBillsByPage_Success() {
        // Arrange
        BillBo billBo = new BillBo();
        Page<Bill> mockPage = new Page<>();
        Bill bill = new Bill();
        bill.setId(1L);
        bill.setCategoryId(1L);
        bill.setSubCategoryId(2L);
        mockPage.setRecords(Collections.singletonList(bill));

        when(userContextUtil.getCurrentUserId()).thenReturn(userId);
        when(billMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);
        
        // Mock batch queries
        Category category = new Category();
        category.setId(1L);
        category.setName("Food");
        when(categoryMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(category));

        SubCategory subCategory = new SubCategory();
        subCategory.setId(2L);
        subCategory.setName("Lunch");
        when(subCategoryMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(subCategory));

        // Act
        Page<BillVo> result = billService.getBillsByPage(1, 10, billBo);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("Food", result.getRecords().get(0).getCategoryName());
        assertEquals("Lunch", result.getRecords().get(0).getSubCategoryName());
    }
}

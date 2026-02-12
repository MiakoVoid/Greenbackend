package com.it.greenfinance.service.impl;

import com.it.greenfinance.mapper.BillMapper;
import com.it.greenfinance.mapper.CategoryKeywordMapper;
import com.it.greenfinance.mapper.CategoryMapper;
import com.it.greenfinance.mapper.SubCategoryMapper;
import com.it.greenfinance.pojo.Category;
import com.it.greenfinance.pojo.SubCategory;
import com.it.greenfinance.service.BillService;
import com.it.greenfinance.service.ExpectedExpenseService;
import com.it.utils.QwenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 性能基准测试
 */
@ExtendWith(MockitoExtension.class)
public class AIServiceBenchmarkTest {

    @Mock
    private CategoryKeywordMapper categoryKeywordMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private SubCategoryMapper subCategoryMapper;
    @Mock
    private BillMapper billMapper;
    @Mock
    private QwenUtil qwenUtil;
    @Mock
    private BillService billService;
    @Mock
    private ExpectedExpenseService expectedExpenseService;

    private AIServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiService = new AIServiceImpl(categoryKeywordMapper, categoryMapper, subCategoryMapper, billMapper, qwenUtil, billService, expectedExpenseService);
        
        // Mock data
        List<Category> categories = new ArrayList<>();
        Category c1 = new Category(); c1.setId(1L); c1.setName("餐饮");
        categories.add(c1);
        when(categoryMapper.selectList(any())).thenReturn(categories);
        when(subCategoryMapper.selectList(any())).thenReturn(Collections.emptyList());
        // Ensure keyword mapper returns empty list instead of null
        when(categoryKeywordMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void benchmarkProcessText() {
        // Construct a long text with many segments to simulate load
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("今天买菜花了").append(i + 10).append("元，");
        }
        String text = sb.toString();
        
        long start = System.currentTimeMillis();
        aiService.processText(1L, text, null);
        long end = System.currentTimeMillis();
        
        System.out.println("Benchmark ProcessText (50 segments): " + (end - start) + " ms");
    }
}
